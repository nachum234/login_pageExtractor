package com.bis.service;


import com.bis.conf.CrawlerProperties;
import com.bis.constants.CrawlerConstants;
import com.bis.constants.CreativeStatusEnum;
import com.bis.constants.DiscoveryStatusEnum;
import com.bis.constants.UserAgent;
import com.bis.dao.EntitiesDao;
import com.bis.model.JobTicketCrawlerToAdex;
import com.bis.model.JsonModel.PublisherScenario;
import com.bis.model.UrlStatus;
import com.biscience.shared.*;
import com.biscience.shared.constants.Channel;
import com.biscience.util.config.ConfigurationManager;
import com.biscience.util.monitoring.counters.dynamic.CounterManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import utils.CommonUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by Anna Kuranda on 1/11/2017.
 */
@Service
public class CrawlerQService implements MQListener {
    private static Logger logger = Logger.getLogger(CrawlerQService.class);
    private static Logger report = Logger.getLogger("report");
    private static final String lang = "english";
    private static final String countryCode = "us";


    public static  Map<String, Boolean> allPublishers;
    public static Map<String, Set<String>> publAliases;


    //local objects
    private MQBase mq;        // Reads data from rabbitmq in auto mode

    private WatchThreadCrawlerWrapper watchThread;
    // counters
    private CounterManager counterManager;


    // workaround for mobile channel
    private HashMap<String, String> mobileDomainsMapper;

    private int pageFetchMultiplier;

    private Properties params = null;
    private int numThreads = 1;


    private volatile Map<String, MQBase> mqSender = null;
    private volatile Map<String, MQBase> mqUpdaterSender = null;


    public void init() {
        pageFetchMultiplier = 1;
        watchThread = new WatchThreadCrawlerWrapper();
        counterManager = new CounterManager();
        allPublishers = EntitiesDao.getAllActivePublishersAndSetions();
        publAliases = EntitiesDao.getPublisherAliases();

        mq = null;

        params = ConfigurationManager.getInstance().getPropertiesObject();

        if (params.getProperty("in.mq.multithreaded") != null && params.getProperty("in.mq.multithreaded").equals("true")) {
            this.numThreads = params.getProperty("in.mq.multithreaded.threads.num") == null ? 1 : Integer.parseInt(params.getProperty("in.mq.multithreaded.threads.num"));
        }

        mqSender = new HashMap<>(numThreads);
        mqUpdaterSender = new HashMap<>(numThreads);


        pageFetchMultiplier = CrawlerProperties.FETCH_MULTIPLIER.getIntValue();
        this.watchThread.init();


    }

    @Override
    public boolean message(byte[] msg, String exchangeName, String queueName, String unusedRoutingKey) {
        this.watchThread.reset();
        CrawlerService crawlerService = new CrawlerService();
        counterManager.inc(CounterManager.Types.RECEIVED);
        logger.debug("Message received: " + new String(msg));
        report.debug("message: " + new String(msg));
        try {
            // get message parameters
            JSONObject inMsg = new JSONObject(new String(msg));
            long entity_id = inMsg.getLong(CrawlerConstants.Ticket.ENTITY_ID_JSON_FIELD_NAME);
            String bulk_id = inMsg.getString(CrawlerConstants.Ticket.BULK_ID_JSON_FIELD_NAME);
            String domain = inMsg.getString(CrawlerConstants.Ticket.DOMAIN_FIELD_NAME);

            Channel channel = Channel.getChannel(inMsg.getInt(CrawlerConstants.Ticket.CHANNEL_FIELD_NAME));
            int channelNum = inMsg.getInt(CrawlerConstants.Ticket.CHANNEL_FIELD_NAME);
            final int countryId = inMsg.getInt(CrawlerConstants.Ticket.COUNTRY_ID_JSON_FIELD_NAME);
            int maxPageFetch = inMsg.getInt(CrawlerConstants.Ticket.MAX_URLS_FIELD_NAME);
            String countryCode = inMsg.getString(CrawlerConstants.Ticket.COUNTRY_CODE_FIELD_NAME);
            boolean shouldUseLangFilter = (inMsg.getBoolean(CrawlerConstants.Ticket.LANGUAGE_DETECTION));
            String publScenario = inMsg.getString(CrawlerConstants.Ticket.LOGIN_SCENARIO);
            //used for test
            // publScenario = "{\"inputs\":[{\"selector\":\"#username\",\"value\":\"annatest\"},{\"selector\":\"#password\",\"value\":\"Abcdefganna1!\"}],\"submit\":{\"type\":\"submit\",\"target\":\".form-horizontal\"},\"injects\":[],\"loginUrl\":\"http://www.healthyplace.com/index.php?option=com_users&view=login\",\"validation\":\"a[href*='logout']\",\"additionalLoginJava\":null}";

            PublisherScenario publisherScenario = CommonUtils.objectMapper.readValue(publScenario, PublisherScenario.class);


            // check for mapping
            if (channel == Channel.WebMobile && mobileDomainsMapper.containsKey(domain)) {
                domain = mobileDomainsMapper.get(domain);
                logger.info("!!! Replaced ticket domain " + inMsg.getString(CrawlerConstants.Ticket.DOMAIN_FIELD_NAME) + " with " + domain);
            }

            String seedUrl;
            seedUrl = domain;

            // when the e_type = 8 its means the url is section url
            boolean isSectionDomain = (inMsg.getInt(CrawlerConstants.Ticket.E_TYPE) == 8);
            // indicates if this is a retry


            // we used to get a fixed number of urls to fetch and multiply it by a contant (pageFetchMultiplier)
            // this has changed and now we have an estimated number in the ticket (~3 times more than needed). The multiplier still exist and can be used, default is 1
            int numPagesFetch = maxPageFetch * pageFetchMultiplier;

            //map jobid to found pages
            Map<String, Map<String, UrlStatus>> jobfoundPages = crawlerService.execute(domain, numPagesFetch, isSectionDomain, channelNum, entity_id, publisherScenario);
            if (!jobfoundPages.isEmpty() && !jobfoundPages.keySet().isEmpty()) {
                //each ticket should return one jobid
                String jobId = jobfoundPages.keySet().stream().findFirst().get();

                String res = CommonUtils.objectMapper.writeValueAsString(jobfoundPages);
                logger.debug("Got urls " + res);


                // get proxy data again from the ticket, incase it was overwritten
                String proxyHost = inMsg.getString(CrawlerConstants.Ticket.PROXY_HOST_FIELD_NAME);
                String proxyPort = "" + inMsg.getInt(CrawlerConstants.Ticket.PROXY_PORT_FIELD_NAME);
                String proxyUser = inMsg.getString(CrawlerConstants.Ticket.PROXY_USER_FIELD_NAME);
                String proxyPassword = inMsg.getString(CrawlerConstants.Ticket.PROXY_PASSWORD_FIELD_NAME);
                UserAgent user_agent = UserAgent.getUserAgent(channel);

                List<JobTicketIF> ticketsSentToAdex = sendTickets(bulk_id, channel, countryCode, entity_id, jobId, countryId, proxyHost, proxyPort, proxyUser, proxyPassword, user_agent.getUserAgentName(), user_agent.getUserAgentID(), jobfoundPages.get(jobId),publScenario);

                Integer numOfTicketsSentToUpdater = jobfoundPages.get(jobId).keySet().size();


                // write to report
                report.info("***** Summary for " + domain + " (country " + countryId + " , channel " + channel.getValue() + ") *****");
                report.info("crawled from seed url: " + seedUrl);
                report.info("found " + numOfTicketsSentToUpdater + " urls, limit is " + numPagesFetch);
                report.info("Sent " + numOfTicketsSentToUpdater + " to updater");
                report.info("Sent " + ticketsSentToAdex.size() + " to adextractor");

                report.info("");
                report.info("");
            } else {
                logger.debug("The domain not a publisher ,No crawling done ");
                String logMessage = "Stopped the crawling because NOT a publisher.";
                logger.info(logWithTicketTemplate(logMessage,domain,countryId,channel));
                report.info(logWithTicketTemplate(logMessage,domain,countryId,channel));
                return true;
            }

        } catch (Exception e) {
            logger.error("Failed in Crawler.message() ", e);

        }


        counterManager.inc(CounterManager.Types.COMPLETED);
        return true;

    }


    private String logWithTicketTemplate(String messageToLog, String domain, Integer countryId, Channel channel) {
        return messageToLog + " " + "domain: " + domain + " ,country: " + countryId + " ,channel: " + channel + " ";
    }

    //=======================
    private MQBase getPublisherAdex() throws IOException {
        return getWantedMQPublisher(mqSender, "out");
    }

    private MQBase getWantedMQPublisher(Map<String, MQBase> mqSenders, String mqPrefix) throws IOException {
        MQBase publisher = mqSenders.get(Thread.currentThread().getName());

        // check that publisher is valid. if not, create and add new
        if (publisher == null) {
            publisher = new MQBase(Thread.currentThread().getName(), mqPrefix, params);
            mqSenders.put(Thread.currentThread().getName(), publisher);
        }
        if (!publisher.isConnected()) {
            publisher.connect();
        }

        return (publisher);
    }


    //=======================
    private MQBase getPublisherInsert() throws IOException {
        return getWantedMQPublisher(mqUpdaterSender, "updater");
    }


    //-----------------------------
    private List<JobTicketIF> sendTickets(String bulk_id, Channel channel, String countryCode, long holdEntityId, String jobId, int countryId, String proxyHost, String proxyPort, String proxyUser, String proxyPassword, String userAgentName, long userAgentId, Map<String, UrlStatus> results, String publScenario) throws IOException {
        // get MQ publishers for adex and updater
        MQBase publisherAdex = getPublisherAdex();
        MQBase publisherInsertMsg = getPublisherInsert();


        // collect the adex tickets to send together
        List<JobTicketIF> ticketsToSend = new ArrayList<>();
        JobTicketFlat adexTicket;
        JSONObject updaterInsertTicket;

        // loop over all results
        for (String urlStr : results.keySet()) {

            Integer channelID = 0;
            if (channel != null) {
                channelID = channel.getValue();
            }

            // Prepare adex ticket
            adexTicket = prepareAdexTicket(countryId, holdEntityId, urlStr, userAgentName, userAgentId, proxyHost, Integer.parseInt(proxyPort), proxyUser, proxyPassword, channelID, results.get(urlStr).getDepth(), countryCode,publScenario);


            // prepare updater-insert ticket
            updaterInsertTicket = prepareUpdaterInsertTicket(jobId, bulk_id, holdEntityId, countryId, adexTicket.getId(), urlStr, results.get(urlStr).getStatus(), channelID, lang, results.get(urlStr).getDepth());

            // publish the updater-insert ticket (send everything, including errors etc)
            publisherInsertMsg.publish(updaterInsertTicket.toString().getBytes());
            logger.debug("sent to updater-insert: " + updaterInsertTicket.toString());


            // so why do we send everything to updateder and filter out for the adextractor????
            ticketsToSend.add(adexTicket);


        }

        // Send a msg to AdExtractor
        sendToAdExtractor(publisherAdex, ticketsToSend);
        return ticketsToSend;
    }


    private void sendToAdExtractor(MQBase publisherAdex, List<JobTicketIF> ticketsToSend) throws IOException {
        logger.debug("Strating to publish adex tickets - " + ticketsToSend.size());
        int counter = 0;
        for (JobTicketIF ticket : ticketsToSend) {
            publisherAdex.publish(ticket.toString().getBytes());
            logger.debug("Message " + (++counter) + "/" + ticketsToSend.size() + " to AdExtractors has been successfully published: " + ticket.toString());
        }
    }


    //=======================
    private JSONObject prepareUpdaterInsertTicket(String jobId, String bulkId, long entityId, int countryId, String ticketID, String url, int status, int channel, String lang, int depth) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("job_id", jobId);
        obj.put("bulk_id", bulkId);
        obj.put("ticket_id", ticketID);
        obj.put("entity_id", entityId);
        obj.put("country_id", countryId);
        obj.put("url", url);
        obj.put("creative_status", CreativeStatusEnum.NOT_FOUND.getCreativeStatus());
        obj.put("discovery_status", DiscoveryStatusEnum.getDiscoveryStatusEnumObj(status).getDiscoveryStatus());
        obj.put("json", "{}");
        obj.put("msg_json_type", "updater_sql_data");
        obj.put("page_lang", lang);
        obj.put("page_depth", depth);
        obj.put("channel_id", channel);
        return obj;
    }

    //=======================
    private JobTicketFlat prepareAdexTicket(int countryId, long holdEntityId, String urlStr, String userAgentName, long userAgentId, String proxyHost, int proxyPort, String proxyUser, String proxyPassword, int channel, int pageDepth, String countryCode,String jsonLogin) {
        return new JobTicketCrawlerToAdex(countryId, holdEntityId, urlStr, userAgentName, userAgentId, proxyHost, proxyPort, proxyUser, proxyPassword, channel, pageDepth, countryCode,jsonLogin);
    }


    //=======================
    public void close() {

        // Close all connectons
        logger.info("Trying to close RabbitMQ subscription connection...");
        try {
            if (mq != null) {
                mq.close();
            }
            logger.info("RabbitMQ subscription connection successfully closed");
        } catch (IOException ex) {
            logger.error("Failed closing RabbitMQ subscription connection", ex);
        }
    }

    //@Override
    //=======================
    public void listen() {
        try {
            mq = new MQBase(Thread.currentThread().getName(), "data.in", params);
            mq.connect();
            mq.subscribe(this); // blocks here; MQBase will call message() and exception() functons of MQListener implementation (i.e. the Dispatcher class)
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (mq != null) {
                try {
                    mq.close();
                } catch (IOException e) {
                    logger.error("Can not subscribe to RabbitMQ", e);
                }
            }
        }
    }


    @Override
    //=======================
    public boolean exception(Exception e) {
        logger.error(e.getMessage(), e);
        return true;
    }


    /**
     * if activated, will create a thread to watch if the main thread is idle for too long, based on a configurable maximum timeout
     */
    private class WatchThreadCrawlerWrapper {

        private WatchThread watchThread;

        private void init() {
            if (CrawlerProperties.WATCHTHREAD_ACTIVE.getBooleanValue()) {
                this.watchThread = new WatchThread(System.currentTimeMillis(), CrawlerProperties.MAX_TIMEOUT.getIntValue(), logger, null, "sw");
                this.watchThread.start();
            }
        }


        //=======================
        private void reset() {
            if (CrawlerProperties.WATCHTHREAD_ACTIVE.getBooleanValue()) {
                this.watchThread.setTimestamp(System.currentTimeMillis());
            }
        }
    }
}

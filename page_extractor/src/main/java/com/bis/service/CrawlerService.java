package com.bis.service;

import com.bis.conf.CrawlerProperties;
import com.bis.conf.CrawlingConfiguration;
import com.bis.constants.UserAgent;
import com.bis.model.CrawlEntity;
import com.bis.model.JsonModel.PublisherScenario;
import com.bis.model.UrlStatus;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import utils.CommonUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.bis.dao.EntitiesDao.addAlias;

/**
 * Created by Anna Kuranda on 2/17/2017.
 */

public class CrawlerService {
    private static Logger logger = Logger.getLogger(CrawlerService.class);


    //=======================
    public CrawlingConfiguration createCrawlingConfiguration(int maxPageFetch, boolean isSectionDomain, int channelNum, long entityId, String publisherUrl) throws Exception {
        CrawlingConfiguration crawlConfigPubl;
        crawlConfigPubl = new CrawlingConfiguration(entityId);
        StringBuilder storagePerJob = new StringBuilder();
        storagePerJob.append(CrawlerProperties.URL_DISCOVERY_TMP_FOLDER.getValue()).append(File.separator).append(crawlConfigPubl.getJobId());
        crawlConfigPubl.setCrawlStorageFolder(storagePerJob.toString());
        crawlConfigPubl.setMaxPagesToFetch(maxPageFetch);
        crawlConfigPubl.setMaxDepthOfCrawling(CrawlerProperties.MAX_CRAWL_DEPTH.getIntValue());
        crawlConfigPubl.setIs_section(isSectionDomain);
        crawlConfigPubl.setChannelNum(channelNum);
        crawlConfigPubl.setPublisherNormalisedDomain(publisherUrl);
        crawlConfigPubl.setPublisherUrl(publisherUrl);




        if (!crawlConfigPubl.isValid()) {
            throw new RuntimeException("Invalid crawler configuration ");
        }

        return crawlConfigPubl;
    }


    //start crawling
    public Map<String, Map<String, UrlStatus>> execute(String domain, int numPagesFetch, boolean isSectionDomain, int channelNum, long entityId, PublisherScenario currPublisherData) {
        PageFetcherService pageFetcherService = new PageFetcherService();

        Map<String, Map<String, UrlStatus>> jobIdToFoundPages = Maps.newHashMap();
        try {
            //if domain =null.Done redirect to not a publisher
            CrawlingConfiguration crawlingConfiguration = createCrawlingConfiguration(numPagesFetch, isSectionDomain, channelNum, entityId, domain);
            crawlingConfiguration.setPublisherUrl(domain);

            CrawlEntity publisher = generatePublisherCrawlEntity(crawlingConfiguration);
            publisher.setPublisherScenario(currPublisherData);

            domain = updateDomainByRedirectIfFound( publisher, entityId,pageFetcherService);
            if (domain != null) {
                publisher.getCrawlingConfiguration().setPublisherUrl(domain);
                jobIdToFoundPages.put(crawlingConfiguration.getJobId(), pageFetcherService.crawlPublisher(publisher));
                if (CrawlerProperties.CLEAN_FILES.getBooleanValue()) {
                    cleanStorageFolders(crawlingConfiguration);
                }
            }
            else{
                logger.debug("Done redirect to not a publisher");
            }

        } catch (Exception ex) {
            logger.error("Failed execute crawler flow " + ex);
        }
        return jobIdToFoundPages;
    }


    public CrawlEntity generatePublisherCrawlEntity(CrawlingConfiguration crawlingConfiguration) {
        CrawlEntity crawlEntity = new CrawlEntity();
        crawlEntity.setCrawlingConfiguration(crawlingConfiguration);
        crawlEntity.setScanUrl(crawlingConfiguration.getPublisherUrl());
        return crawlEntity;
    }


    private String updateDomainByRedirectIfFound(CrawlEntity publisher, long entityId, PageFetcherService pageFetcherService) {
        String domain = publisher.getCrawlingConfiguration().getPublisherUrl();
        try {
            logger.debug("Check publisher for redirect "+publisher.getCrawlingConfiguration().getPublisherUrl());
            HttpURLConnection con = (HttpURLConnection) (new URL(domain).openConnection());
            con.setInstanceFollowRedirects(true);
            con.setRequestProperty("User-Agent", UserAgent.DESKTOP.getUserAgentName());
            con.connect();
            int responseCode = con.getResponseCode();
            logger.debug("Url response code " + responseCode);
            String location = con.getHeaderField("Location");
            //String normLocation = CommonUtils.getDomainNoProtocolAndNoWWW(location.toLowerCase().trim());
            if (CommonUtils.redirectCodes.contains(responseCode) ) {
                logger.debug(domain + " return response code that detect  redirect to "+location);
                domain = checkAndUpdate(location, domain, entityId);
            } else {
                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    logger.debug("Http url connection cant detect if done redirect.Should check by casper");
                    location = pageFetcherService.getRedirectedUrl(publisher);
                    domain = checkAndUpdate(location, domain, entityId);
                }
                else{
                    logger.debug("No redirect done . ");
                }
            }


        } catch (Exception e) {
            logger.error("Failed find if redirect exists " + e);

        }

        return domain;


    }


    private String checkAndUpdate(String location, String domain, long entityId) {
        //remove protocol and www from urls and then compare
        String normLocation = CommonUtils.getDomainNoProtocolAndNoWWW(location.toLowerCase().trim());
        if (!CommonUtils.getDomainNoProtocolAndNoWWW(domain.toLowerCase().trim()).equals(normLocation)) {
            if (CrawlerQService.allPublishers.containsKey(normLocation)) {
                logger.debug("Found alias  defined as publisher in entites table " + location);
                return null;
            }
            logger.debug("Found alias defined not as publisher in entites table " + location);
            //alias is publisher.continue with this publisher(ticket)
            updateDbEntities(location, domain, entityId);
            domain = location;
            logger.debug("Done redirect to " + domain);
        }

        return domain;


    }

    private synchronized void updateDbEntities(String location, String domain, long entityId) {


        String normdomain = CommonUtils.getDomainNoProtocolAndNoWWW(domain.toLowerCase().trim());
        String normlocation = CommonUtils.getDomainNoProtocolAndNoWWW(location.toLowerCase().trim());
        //if publisher was  not redirects before
        if (!CrawlerQService.publAliases.containsKey(normdomain)) {
            CrawlerQService.publAliases.put(normdomain, Sets.newHashSet());
        }

        //publisher was redirects before , but not to current alias
        if (!CrawlerQService.publAliases.get(normdomain).contains(normlocation)) {
            CrawlerQService.publAliases.get(normdomain).add(normlocation);
            addAlias(location, domain, entityId);
        }

    }

    private void cleanStorageFolders(CrawlingConfiguration crawlingConfiguration) {
        String storeFolder = null;
        try {
            storeFolder = crawlingConfiguration.getStoreFolder();
            logger.debug("Clean store folder from publisher run time files " + storeFolder);
            Path path = Paths.get(storeFolder);
            if (path.toFile().exists()) {
                deleteDir(path.toFile());
            }
        } catch (Exception e) {
            logger.error("failed clean directory " + storeFolder);
        }

        try {
            logger.debug("try remove " + crawlingConfiguration.getCasperPublishersLoginScriptName());
            Path path = Paths.get(crawlingConfiguration.getCasperPublishersLoginScriptName());
            if (path.toFile().exists()) {
                path.toFile().delete();
            }
            logger.debug("try remove " + crawlingConfiguration.getCookieFileName());
            path = Paths.get(crawlingConfiguration.getCookieFileName());
            if (path.toFile().exists()) {
                path.toFile().delete();
            }

        } catch (Exception e) {
            logger.error("failed remove login / cookie files ");
        }
    }


    public boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}



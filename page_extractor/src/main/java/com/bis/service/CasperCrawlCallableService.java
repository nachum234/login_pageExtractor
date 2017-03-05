package com.bis.service;

import com.bis.conf.CrawlerProperties;
import com.bis.model.CrawlEntity;
import com.bis.model.UrlStatus;
import com.biscience.shared.Stamper;
import com.google.api.client.util.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import utils.CommonUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by User on 2/17/2017.
 */
public class CasperCrawlCallableService implements Callable {
    private static Logger logger = Logger.getLogger(CasperCrawlCallableService.class);
    CrawlEntity crawlEntity;


    private int depth;
    private Map<String, UrlStatus> allLevelsUrls;

    public CasperCrawlCallableService(CrawlEntity crawlEntity, int depth, Map<String, UrlStatus> allLevelsUrls) {
        this.crawlEntity = crawlEntity;

        this.depth = depth;
        this.allLevelsUrls = allLevelsUrls;
    }

    @Override
    public Set<String> call() throws Exception {
        System.out.println("In call method");
        logger.debug("In call method");
        Set urls = Sets.newHashSet();
        String folder = crawlEntity.getCrawlingConfiguration().getStoreFolder();
        if (folder != null) {
            String storeUrlsFile = folder + File.separator + Stamper.md5(UUID.randomUUID().toString()) + ".txt";
            String cmd = buildCasperjsCmd(CrawlerProperties.CASPERJS_PROGRAMM.getValue(), storeUrlsFile);
            logger.debug("Run cmd " + cmd);
            final Process proc = Runtime.getRuntime().exec(cmd);
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            Future future = executorService.submit(new Callable() {
                public String call() throws Exception {
                    return execCasperCmd(proc);

                }

            });

            String execCasperCmdResult = null;
            try {
                execCasperCmdResult = (String) future.get(CrawlerProperties.CASPERJS_TIMEOUT.getLongValue(), TimeUnit.MINUTES);
                logger.debug(execCasperCmdResult);
            } catch (TimeoutException e) {
                logger.debug("No response after min" + CrawlerProperties.CASPERJS_TIMEOUT.getLongValue());
                future.cancel(true);
                proc.destroyForcibly();

            } finally {
                try {
                    proc.destroyForcibly();
                    executorService.shutdownNow();
                    logger.debug("stop casperjs process");
                } catch (Exception e) {
                    logger.debug(e);
                }

            }

            logger.debug("The casperjs console output" + execCasperCmdResult);

            urls = getUrls(storeUrlsFile);
            if (urls != null && !urls.isEmpty()) {
                //urls found update url status with 200 ok
                allLevelsUrls.get(this.crawlEntity.getScanUrl()).setStatus(HttpStatus.SC_OK);
                urls = analyseUrls(urls, depth, allLevelsUrls, crawlEntity.getCrawlingConfiguration().getPublisherNormalisedDomain());


            } else {
                allLevelsUrls.get(this.crawlEntity.getScanUrl()).setStatus(HttpStatus.SC_NOT_FOUND);
            }


        }

        return urls;
    }


    private Set<String> analyseUrls(Set<String> oneLevelUrls, int depth, Map<String, UrlStatus> allLevelsUrls, String publisherDomain) {
        Set<String> scanUrls = Sets.newHashSet();
        oneLevelUrls.forEach(oneLevelUrl -> {
                    //check if current url still not in scaned / or supposed to be scaned map
                    if (!urlFound(allLevelsUrls, oneLevelUrl)) {
                        //check by all rules if scan current url
                        if (validToScan(oneLevelUrl, publisherDomain)) {
                            //all found urls them for next depth and with default status 501 == not implemented
                            allLevelsUrls.put(oneLevelUrl, new UrlStatus(depth + 1));
                            scanUrls.add(oneLevelUrl);
                        }

                    }

                }
        );

        return scanUrls;
    }

    //check if current url in list of scaned/to be scaned urls
    private boolean urlFound(Map<String, UrlStatus> allLevelsUrls, String oneLevelUrl) {

        final String oneLevelUrl1 = CommonUtils.getDomainNoProtocolAndNoWWW(oneLevelUrl);

        //if url null or empty no need add to map.so if return true == found url should be not added
        if (StringUtils.isEmpty(oneLevelUrl)) {
            logger.error("Failed to get url without protocol .");
            return true;
        }
        for (String url : allLevelsUrls.keySet()) {
            url = CommonUtils.getDomainNoProtocolAndNoWWW(url);
            if (oneLevelUrl1.equalsIgnoreCase(url)) {
                return true;
            }
        }

        return false;
    }


    private boolean validToScan(String levelUrl, String publisherDomain) {
        boolean isValidToScan = true;
        levelUrl = levelUrl.trim().toLowerCase();
        publisherDomain = publisherDomain.trim().toLowerCase();
        if (!levelUrl.contains(publisherDomain)) {
            isValidToScan = false;
        } else {
            try {
                //get domain=hostname without protocol
                //  String currentUrlHostName = CommonUtils.getDomainNoProtocolAndNoWWW( new URL(levelUrl).getHost());
                //  currentUrlHostName = currentUrlHostName.toLowerCase().trim();
                //check if new url is not a my  publisher
                if (publisherDomain.equalsIgnoreCase(CommonUtils.getDomainNoProtocolAndNoWWW(levelUrl)))
                    isValidToScan = false;
                else {
                    //not in publishers and not in aliases
                    isValidToScan = !publisherOrSection(levelUrl, publisherDomain) && !foundAsAlias(levelUrl, publisherDomain);

                }
            } catch (Exception e) {
                logger.error("failed get domain from url " + levelUrl);
                isValidToScan = false;

            }
        }

        return isValidToScan;
    }

    private boolean publisherOrSection(String levelUrl, String publisherDomain) {
        Set<String> possiblePublAndSections = getPossiblePublAndSections(publisherDomain);
        for (String publOrSection : possiblePublAndSections) {
            if (publOrSection.toLowerCase().contains(levelUrl.toLowerCase())) return true;
        }
        return false;
    }

    private Set<String> getPossiblePublAndSections(String publisherDomain) {
        Set<String> possiblePublAndSectiona = Sets.newHashSet();
        possiblePublAndSectiona = CrawlerQService.allPublishers.keySet().stream().filter(
                p -> p.contains(publisherDomain)
        ).collect(Collectors.toSet());
        return possiblePublAndSectiona;
    }

    private boolean foundAsAlias(String levelUrl, String publisherDomain) {
        boolean isFound = false;
        if (CrawlerQService.publAliases.containsKey(publisherDomain)) {
            isFound = CrawlerQService.publAliases.get(publisherDomain).contains(levelUrl);
        }
        logger.debug(levelUrl + " is alias to " + publisherDomain + " ? " + isFound);
        return isFound;
    }


    private String buildCasperjsCmd(String casperjs, String storeUrlsFile) {
        StringBuilder sb = new StringBuilder();
        sb.append(casperjs).append(" ").append(this.crawlEntity.getCrawlingConfiguration().getCasperMainScript()).append(" ");
        //use for cookie
        sb.append("--cookies-file=").append(this.crawlEntity.getCrawlingConfiguration().getCookieFileName()).append(" ");
        sb.append("--loginScript=").append(this.crawlEntity.getCrawlingConfiguration().getCasperPublishersLoginScriptName()).append(" ");
        sb.append("--scanUrl=").append("\"").append(this.crawlEntity.getScanUrl()).append("\"").append(" ");
        sb.append("--loginUrl=").append("\"").append(this.crawlEntity.getPublisherScenario().getLoginUrl()).append("\"").append(" ");
        //used by main.js for save found urls
        sb.append("--storeUrlFile=").append("\"").append(storeUrlsFile).append("\"").append(" ");
        //use for login output files
        sb.append("--storeFolder=").append("\"").append(this.crawlEntity.getCrawlingConfiguration().getStoreFolder()).append(File.separator).append("\"").append(" ");


        return sb.toString();
    }

    private Set<String> getUrls(String storeUrlsFile) {
        Set<String> linesSet = Sets.newHashSet();
        Path path = null;
        try {
            path = Paths.get(storeUrlsFile);
            if (path.toFile().exists()) {
                List<String> lines = Files.readAllLines(Paths.get(storeUrlsFile), Charset.defaultCharset());
                linesSet.addAll(lines);
                //remove file after read casperjs urls
            }
        } catch (Exception e) {
            logger.error("failed read file "+storeUrlsFile +" \n" +e);
        }

        return linesSet;


    }


    private String execCasperCmd(Process proc) throws java.io.IOException {
        try {

            logger.debug("In execCasperCmd process is alive" + proc.isAlive());
            java.io.InputStream is = proc.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String val = "";
            if (s.hasNext()) {

                val = s.next();
                //logger.debug(val);
                proc.destroyForcibly();
            } else {
                val = "";

            }

            return val;
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }


}

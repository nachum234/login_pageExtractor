package com.bis.service;

import com.bis.conf.CrawlerProperties;
import com.bis.conf.CrawlingConfiguration;
import com.bis.model.CrawlEntity;
import com.bis.model.JsonModel.PublisherScenario;
import com.bis.model.UrlStatus;
import com.google.api.client.util.Sets;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Anna Kuranda on 2/15/2017.
 */

public class PageFetcherService {
    private static Logger logger = Logger.getLogger(PageFetcherService.class);


    public void executeCasperCrawlScenario(Set<String> oneLevelUrls, int depth, Map<String, UrlStatus> allLevelsUrls, CrawlEntity publisher) {



        ExecutorService executor = Executors.newFixedThreadPool(CrawlerProperties.NUM_PHANTOM_PROCESSES.getIntValue());
        Set<Future<Set<String>>> futureSet = Collections.synchronizedSet(new HashSet<Future<Set<String>>>());

        oneLevelUrls = Optional.ofNullable(oneLevelUrls).orElse(Sets.newHashSet());
        Set<String> nextLevelUrls = Collections.synchronizedSet(Sets.newHashSet());
        try {
            if (oneLevelUrls != null && !oneLevelUrls.isEmpty()) {
                //check if file exists .if not create it
                if (prepareToExecuteCasperCrawlScenario(publisher)) {
                    for (String url : oneLevelUrls) {
                        CrawlEntity crawlEntity = publisher.clone();
                        crawlEntity.setScanUrl(url);

                        Callable<Set<String>> callable = new CasperCrawlCallableService(crawlEntity, depth, allLevelsUrls);
                      //  Set<String> res = callable.call();

                        Future<Set<String>> future = executor.submit(callable);
                        futureSet.add(future);
                    }

                    futureSet.forEach(f -> {
                        try {
                            //get analised urls(after check domain)
                            Set<String> urls = f.get();
                            if (urls != null && !urls.isEmpty()) {
                                nextLevelUrls.addAll(urls);
                                if (!continueCrawl(depth, allLevelsUrls.size(), publisher.getCrawlingConfiguration().getMaxPagesToFetch())) {

                                    futureSet.clear();
                                    executor.shutdownNow();
                                    return;

                                }

                            }
                        } catch (InterruptedException e1) {
                            logger.error("Interruption issue " + e1);
                        } catch (ExecutionException e1) {
                            logger.error("Execution issue " + e1);
                        }
                    });

                }
                logger.debug("Finished to crawl depth " + depth);
                executor.shutdownNow();

            }

            // }

        } catch (Exception e) {
            if (e instanceof ConcurrentModificationException) {
                logger.debug("Stop executor process .Reach to last depth or got urls maximum number");
                executor.shutdownNow();
            } else {
                logger.error("Failed in crawl urls  " + e);
            }
        }

        if (continueCrawl(depth + 1, allLevelsUrls.size(), publisher.getCrawlingConfiguration().getMaxPagesToFetch()) && !nextLevelUrls.isEmpty()) {
            executeCasperCrawlScenario(nextLevelUrls, depth + 1, allLevelsUrls, publisher);
        }
    }


    //Check if reach to max pages or done all levels
    private boolean continueCrawl(int depth, int allFoundUrlsSize, int maxPagesToFetch) {
        return depth < CrawlerProperties.MAX_CRAWL_DEPTH.getIntValue() && allFoundUrlsSize < maxPagesToFetch;

    }


    //Create Crawl entities for casperjs next runs
    private boolean prepareToExecuteCasperCrawlScenario(CrawlEntity publisher) {
        boolean isLoginFileDone = true;
        try {

            File file = new File(publisher.getCrawlingConfiguration().getCasperPublishersLoginScriptName());
            if (!file.exists()) {
                isLoginFileDone = createLoginFile(publisher.getPublisherScenario(), publisher.getCrawlingConfiguration());
            }
            if (!isLoginFileDone) {

                logger.error(" login.js template file not found.Can't create login script and run crawling flow");

            }
        } catch (Exception e) {

            logger.error(" Failed  prepare to crawl execution " + e);
        }
        return isLoginFileDone;
    }


    private boolean createLoginFile(PublisherScenario publisherData, CrawlingConfiguration crawlingConfiguration) {
        boolean isDone = true;
        try {
            LoginScriptBuilderService loginScriptBuilderService = new LoginScriptBuilderService();
            isDone = loginScriptBuilderService.buildLoginScript(crawlingConfiguration, publisherData);


        } catch (Exception e) {
            logger.error("Failed create login script .Cant  crawl publisher " + crawlingConfiguration.getPublisherUrl());
            isDone = false;
        }
        return isDone;
    }


    public Map<String, UrlStatus> crawlPublisher(CrawlEntity publisher) {
        Map<String, UrlStatus> allLevelsUrls = new ConcurrentHashMap<>();
        try {

            allLevelsUrls.put(publisher.getScanUrl(), new UrlStatus(0));
            executeCasperCrawlScenario(Stream.of(publisher.getScanUrl()).collect(Collectors.toSet()), 0, allLevelsUrls, publisher);


        } catch (Exception e) {
            logger.error("Failed crawl publisher ");
        }
        return allLevelsUrls;


    }


    public String getRedirectedUrl(CrawlEntity publisher) {
        String url = null;
        try {
            if (prepareToExecuteCasperCrawlScenario(publisher)) {
                Callable<String> callable = new CasperCrawlRedirectCheckCallableService(publisher);
                url = callable.call();

            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return url;
    }
}

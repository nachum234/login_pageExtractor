package com.bis.conf;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import com.biscience.shared.Stamper;
import com.google.api.client.util.Charsets;
import com.google.common.io.Files;
import org.apache.log4j.Logger;
import utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class CrawlingConfiguration {


    private static Logger logger = Logger.getLogger(CrawlingConfiguration.class);
    private final String jobId;

    private int channelNum;

    private String casperMainScript;

    private String casperTemplateLoginScript;

    private String casperPublishersLoginScriptName;

    private String casperTemplateLoginStr;

    private String crawlStorageFolder;


    private String publisherNormalisedDomain;

    private String publisherUrl;



    /**
     * Maximum depth of crawling For unlimited depth this parameter should be
     * set to -1
     */
    private int maxDepthOfCrawling = -1;

    /**
     * Maximum number of pages to fetch For unlimited number of pages, this
     * parameter should be set to -1
     */
    private int maxPagesToFetch = -1;


    private boolean is_section;
    public String cookieFileName;


    public CrawlingConfiguration(long publisherId) throws Exception {
        this.jobId = Stamper.md5(UUID.randomUUID().toString());
        StringBuilder sb = new StringBuilder();
        sb.append(CrawlerProperties.CASPER_SCRIPTS_LOCATION.getValue()).append(CrawlerProperties.CASPER_MAIN_SCRIPT.getValue());
        this.casperMainScript = sb.toString();
        sb = new StringBuilder();
        //login script name contain publisher in his name -> unique login file per publisher!!
        //   sb.append(CrawlerProperties.CASPER_SCRIPTS_LOCATION.getValue()).append("login").append(publisherId).append(".js");

        sb.append(CrawlerProperties.CASPER_SCRIPTS_LOCATION.getValue()).append("login").append(".js");
        this.casperTemplateLoginScript = sb.toString();



        sb = new StringBuilder();
        //cookie file name name contain publisher in his name -> unique cookie  file per publisher!!
        sb.append(CrawlerProperties.CASPER_SCRIPTS_LOCATION.getValue()).append("publishers_scripts").append(File.separator).append("cookie").append(publisherId).append("_").append(jobId).append(".txt");
        this.cookieFileName = sb.toString();


        sb = new StringBuilder();
        sb.append(CrawlerProperties.CASPER_SCRIPTS_LOCATION.getValue()).append("publishers_scripts").append(File.separator).append("login").append(publisherId).append("_").append(jobId).append(".js");
        this.casperPublishersLoginScriptName = sb.toString();

        if(isValidLoginTemplate()){
            this.casperTemplateLoginStr = readTemplateLogin();
        }


    }

    private String readTemplateLogin() throws IOException {
       return  Files.toString(new File(casperTemplateLoginScript), Charsets.UTF_8);
    }

    public String getCasperPublishersLoginScriptName() {
        return casperPublishersLoginScriptName;
    }

    public void setCasperPublishersLoginScriptName(String casperPublishersLoginScriptName) {
        this.casperPublishersLoginScriptName = casperPublishersLoginScriptName;
    }

    public String getPublisherNormalisedDomain() {
        return publisherNormalisedDomain;
    }

    public void setPublisherNormalisedDomain(String publisherDomain) {
        this.publisherNormalisedDomain = CommonUtils.getDomainNoProtocolAndNoWWW(publisherDomain);
    }


    public String getJobId() {
        return jobId;
    }





//    public String getDomainNoProtocolAndNoWWW(String publisherUrl) {
//        // clean up http://  and https:// from publisher domain
//        String domain = null;
//        try {
//            domain  = publisherUrl.replaceFirst("^(http://|https://)","");
//            domain  = domain.replaceFirst("^(www.)","");
//            if(domain.endsWith("/")){
//                domain  = domain.replace("/","");
//            }
//
//        }catch(Exception e){
//            logger.error(" Cant get domain from publisher "+publisherUrl);
//        }
//        return domain;
//    }





    /**
     * Validates the configs specified by this instance.
     */
    public boolean isValid() {
        boolean isValid = true;
        if (crawlStorageFolder == null) {
            logger.error("Crawl storage folder is not set in the CrawlConfig.");
            isValid = false;
        }
        if (maxDepthOfCrawling < -1) {
            logger.error("Maximum crawl depth should be either a positive number or -1 for unlimited depth.");
            isValid = false;
        }
        if (maxDepthOfCrawling > Short.MAX_VALUE) {
            logger.error("Maximum value for crawl depth is " + Short.MAX_VALUE);
            isValid = false;
        }
        if (maxPagesToFetch < 0) {
            logger.error("Maximum pages to fetch should be either a positive number and bigger then 0.");
            isValid = false;
        }
        if (publisherNormalisedDomain == null || publisherNormalisedDomain.isEmpty()) {
            logger.error("Failed get publisher domain.");
            isValid = false;

        }
        if (casperTemplateLoginStr == null || casperTemplateLoginStr.isEmpty()) {

            logger.error("Failed get template login.js as String .");
            isValid = false;
        }


        return isValid;

    }

    public boolean isValidLoginTemplate() {
        if (casperTemplateLoginScript == null || casperTemplateLoginScript.isEmpty()) {
            logger.error("Failed get template login.js .");
            return false;
        }


        File f = new File(casperTemplateLoginScript);
        return f.exists();

    }



    public String getCrawlStorageFolder() {
        return crawlStorageFolder;
    }

    /**
     * The folder which will be used by crawler for storing the intermediate
     * crawl data. The content of this folder should not be modified manually.
     */
    public void setCrawlStorageFolder(String crawlStorageFolder) {
        this.crawlStorageFolder = crawlStorageFolder;
    }


    public int getMaxDepthOfCrawling() {
        return maxDepthOfCrawling;
    }

    /**
     * Maximum depth of crawling For unlimited depth this parameter should be
     * set to -1
     */
    public void setMaxDepthOfCrawling(int maxDepthOfCrawling) {
        this.maxDepthOfCrawling = maxDepthOfCrawling;
    }

    public int getMaxPagesToFetch() {
        return maxPagesToFetch;
    }


    public void setMaxPagesToFetch(int maxPagesToFetch) {
        this.maxPagesToFetch = maxPagesToFetch > 0 ? maxPagesToFetch : Integer.parseInt(CrawlerProperties.FETCH_PAGES_DEFAULT.getValue());
    }


    public int getChannelNum() {
        return channelNum;
    }

    public void setChannelNum(int channelNum) {
        this.channelNum = channelNum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Crawl storage folder: " + getCrawlStorageFolder() + "\n");
        sb.append("Max depth of crawl: " + getMaxDepthOfCrawling() + "\n");
        sb.append("Max pages to fetch: " + getMaxPagesToFetch() + "\n");
        sb.append("Channel number : " + getChannelNum() + "\n");
        sb.append("Job id : " + getJobId());
        return sb.toString();
    }

    public String getCasperMainScript() {
        return casperMainScript;
    }

    public void setCasperMainScript(String casperMainScript) {
        this.casperMainScript = casperMainScript;
    }

    public String getCookieFileName() {
        return cookieFileName;
    }

    public void setCookieFileName(String cookieFileName) {
        this.cookieFileName = cookieFileName;
    }

    public boolean isIs_section() {
        return is_section;
    }

    public void setIs_section(boolean is_section) {
        this.is_section = is_section;
    }

    public String getCasperTemplateLoginScript() {
        return casperTemplateLoginScript;
    }

    public void setCasperTemplateLoginScript(String casperTemplateLoginScript) {
        this.casperTemplateLoginScript = casperTemplateLoginScript;
    }

    public String getStoreFolder() {

        String folderPath = null;
        try {

            File folder = new File(getCrawlStorageFolder());
            folderPath = folder.getAbsolutePath();
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    logger.error("Couldn't create this folder: " + folder.getAbsolutePath());
                    folderPath = null;
                }
            }


        } catch (Exception e) {
            logger.error("Couldn't create this folder: " + folderPath);
            folderPath = null;
        }

        return folderPath;

    }

    public String getCasperTemplateLoginStr() {
        return casperTemplateLoginStr;
    }

    public void setCasperTemplateLoginStr(String casperTemplateLoginStr) {
        this.casperTemplateLoginStr = casperTemplateLoginStr;
    }

    public String getPublisherUrl() {
        return publisherUrl;
    }

    public void setPublisherUrl(String publisherUrl) {
        this.publisherUrl = publisherUrl;
    }
}


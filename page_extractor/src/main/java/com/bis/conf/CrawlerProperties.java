package com.bis.conf;

import com.biscience.util.config.Property;
import com.biscience.util.config.PropertyDefinitions;

public class CrawlerProperties implements PropertyDefinitions {

    // Watch thread
    public static final Property WATCHTHREAD_ACTIVE = new Property("crawler.watchthread.active", "false","true - activate automatic shutdown of process in case its idle");
    public static final Property MAX_TIMEOUT = new Property("crawler.watchthread.max.timeout", "120000","timeout in miliseconds to shutdown an idle crawler");
    public static final Property URL_DISCOVERY_TMP_FOLDER = new Property("urldicovery.crawler.tmp.folder","/tmp/crawler","url discovery url folder");
    public static final Property FETCH_MULTIPLIER = new Property("crawler.page.fetch.multiplier", "1", "by how much to multiple the number of pages to get to the max number that the crawler should fetch");

    //depth 0,1,2
    public static final Property MAX_CRAWL_DEPTH = new Property("crawler.max.crawl.depth", "2", "how deep to crawl the URL");

    public static final Property CASPERJS_PROGRAMM = new Property("casperjs.programm","C:/Users/User/AppData/Roaming/npm/casperjs.cmd","casperjs progaramm location");
    public static final Property FETCH_PAGES_DEFAULT = new Property("crawler.default.page.fetch", "100", "number of pages to fetch by the crawler");

    public static final Property CASPERJS_TIMEOUT = new Property("casperjs.timeout", "5", "crawler js process run timeout in minutes");

    public static final Property CASPER_SCRIPTS_LOCATION  = new Property("casperjs.scripts.location","c:\\opt\\bis\\casper_scripts\\","Casperjs main script");

    public static final Property CASPER_MAIN_SCRIPT  = new Property("casperjs.main.script","main.js","Casperjs main script");

    public static final Property NUM_PHANTOM_PROCESSES  = new Property("num.phantom.process","3","Number of phantom processes should execute in one run");

    public static final Property CLEAN_FILES  = new Property("clean.files","false","delete all created in runtime files.login,cookie,urls files");




}

package com.bis.service;

import com.bis.conf.CrawlerProperties;
import com.bis.model.CrawlEntity;
import com.biscience.shared.Stamper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by User on 2/17/2017.
 */
public class CasperCrawlRedirectCheckCallableService implements Callable {
    private static Logger logger = Logger.getLogger(CasperCrawlRedirectCheckCallableService.class);
    CrawlEntity crawlEntity;

    public CasperCrawlRedirectCheckCallableService(CrawlEntity crawlEntity) {
        this.crawlEntity = crawlEntity;

    }

    @Override
    public String call() throws Exception {
        System.out.println("In call method");
        logger.debug("In call method");
        String url = null;
        String folder = crawlEntity.getCrawlingConfiguration().getStoreFolder();
        if (folder != null) {
            String storeUrlsFile = folder + File.separator + Stamper.md5(UUID.randomUUID().toString()) + "_redirectUrl.txt";
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

            url = getRedirectUrl(storeUrlsFile);
            if (StringUtils.isEmpty(url)) {
                //urls found update url status with 200 ok
                logger.error("Casper process failed in get redirect url ");

            }

        }

        return url;
    }










    private String buildCasperjsCmd(String casperjs, String storeUrlsFile) {
        StringBuilder sb = new StringBuilder();
        sb.append(casperjs).append(" ").append(this.crawlEntity.getCrawlingConfiguration().getCasperMainScript()).append(" ");

        sb.append("--checkRedirect=").append("\"").append("true").append("\"").append(" ");

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

    private String getRedirectUrl(String storeUrlsFile) {
       String line = null;
        Path path = null;
        try {
            path = Paths.get(storeUrlsFile);
            if (path.toFile().exists()) {
                List<String> lines = Files.readAllLines(Paths.get(storeUrlsFile), Charset.defaultCharset());
                line = lines.get(0);
                //remove file after read casperjs urls
            }
        } catch (Exception e) {
            logger.error("failed read file "+storeUrlsFile +" \n" +e);
        }

        return line;


    }


    private String execCasperCmd(Process proc) throws java.io.IOException {
        try {

            logger.debug("In execCasperCmd process is alive" + proc.isAlive());
            java.io.InputStream is = proc.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String val = "";
            if (s.hasNext()) {

                val = s.next();

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

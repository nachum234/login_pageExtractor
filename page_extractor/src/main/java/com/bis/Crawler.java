package com.bis;

import com.bis.conf.CrawlerDbProperties;
import com.bis.conf.CrawlerProperties;
import com.bis.service.CrawlerQService;
import com.biscience.util.config.ConfigurationManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * Created by Anna Kuranda on 1/10/2017.
 */


@SpringBootApplication
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Crawler implements Runnable,CommandLineRunner {

    private static Logger logger = Logger.getLogger(Crawler.class);
    @Autowired
    private CrawlerQService crawlerQService;





    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar casperjs_crawler.jar -p <properties-file-name> -processId <processId>" );
            System.exit(1);
        }


        try {

            ConfigurationManager.init("Crawler", args, CrawlerDbProperties.class, null);
            ConfigurationManager.init("Crawler", args, CrawlerProperties.class, null);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    crawlerQService.close();
                    System.out.println("finished at: " + new Date().toString());
                }
            }));

            long startTime = System.nanoTime();
            logger.info("process started:");
            System.out.println("Stated at: " + new Date().toString());
            this.run();
            logger.info("exited...");
            logger.info("## elapsed time - " + (System.nanoTime() - startTime));

        } catch (Exception e) { // anything unhandled
            logger.error("Unhandled exception", e);
            System.exit(1);
        }
        Runtime.getRuntime().exit(0);

    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Crawler.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Crawler.class, args);
    }




    @Override
    public void run() {

        crawlerQService.init();
        crawlerQService.listen();
    }


}

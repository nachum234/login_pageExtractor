package com.bis.model;

import com.bis.conf.CrawlingConfiguration;
import com.bis.model.JsonModel.PublisherScenario;

/**
 * Created by User on 2/16/2017.
 */
public class CrawlEntity implements Cloneable {
    private CrawlingConfiguration crawlingConfiguration;
    private PublisherScenario publisherScenario;
    private String scanUrl;

    public CrawlingConfiguration getCrawlingConfiguration() {
        return crawlingConfiguration;
    }

    public void setCrawlingConfiguration(CrawlingConfiguration crawlingConfiguration) {
        this.crawlingConfiguration = crawlingConfiguration;
    }

    public String getScanUrl() {
        return scanUrl;
    }

    public void setScanUrl(String scanUrl) {
        this.scanUrl = scanUrl;
    }


    public PublisherScenario getPublisherScenario() {
        return publisherScenario;
    }

    public void setPublisherScenario(PublisherScenario publisherScenario) {
        this.publisherScenario = publisherScenario;
    }

    @Override
    public CrawlEntity clone() {
        final CrawlEntity clone;
        try {
            clone = (CrawlEntity) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("superclass messed up", ex);
        }
        clone.crawlingConfiguration = this.crawlingConfiguration;
        clone.publisherScenario = this.publisherScenario;

        return clone;
    }
}

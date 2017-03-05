package com.bis.model;

import org.apache.http.HttpStatus;

/**
 * Created by Anna Kuranda on 2/27/2017.
 */
public class UrlStatus {
    private Integer depth;
    private Integer status;

    public UrlStatus(Integer depth) {
        this.depth = depth;
        this.status = HttpStatus.SC_NOT_IMPLEMENTED;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

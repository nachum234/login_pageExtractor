package com.bis.model;

import com.biscience.shared.JobTicketFlat;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nadav.cohen on 27/07/2016.
 */
public class JobTicketCrawlerToAdex extends JobTicketFlat {

    private int pageDepth;
    private String countryCode;
    private String jsonLogin;

    public JobTicketCrawlerToAdex(int countryId, long pubId, String urlStr, String userAgentName, long userAgentId, String proxyHost, int proxyPort, String proxyUser, String proxyPassword, int channel, int pageDepth, String countryCode,String jsonLogin) {
        super(countryId, pubId, urlStr, 0, userAgentName, userAgentId, proxyHost, proxyPort, proxyUser, proxyPassword, channel);
        this.pageDepth = pageDepth;
        this.countryCode = countryCode;
        this.jsonLogin = jsonLogin;

    }


    @Override
    public String toJsonString() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("ticket_id", getId());
        json.put("country_id", getCountryId());
        json.put("publisher_id", getPubId());
        json.put("url", getMonUrl());
        json.put("mon_url_id", getMonUrlId());
        json.put("user_agent", getUserAgent());
        json.put("user_agent_id", getUserAgentId());
        json.put("proxy_host", getProxyHost());
        json.put("proxy_port", getProxyPort());
        json.put("proxy_user", getProxyUser());
        json.put("proxy_password", getProxyPwd());
        json.put("channel", getChannel());

        json.put("page_depth",this.pageDepth);
        json.put("country_code",this.countryCode);
        json.put("login_json",this.jsonLogin);

        return json.toString();
    }
}

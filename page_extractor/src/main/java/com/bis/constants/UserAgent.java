package com.bis.constants;

import com.biscience.shared.constants.Channel;

/**
 * Created by nadav.cohen on 18/11/2015.
 */
/*
 *
 * (!!!!!!!)
 * Note.
 * I really don'y know why, but I can see the following:
 * 1. "id" is used as user agent ID but this doesn't correspond to UserAgent.id column in DB
 * 2. "id" is a channel ID.
 *
 */
public enum UserAgent {

    DESKTOP(1, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36"),
    MOBILE(2, "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36"),
    DESKTOP_VIDEO(4, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36"),
    MOBILE_VIDEO(5, "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");

    private final String name;
    private final Integer id;

    private UserAgent(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getUserAgentName() {
        return name;
    }

    public Integer getUserAgentID() {
        return id;
    }

    public static UserAgent getUserAgent(Channel channel) {
        if (channel != null) {
            for (UserAgent userAgent : values()) {
                if (userAgent.getUserAgentID().equals(channel.getValue())) {
                    return userAgent;
                }
            }
        }
        return DESKTOP;
    }
}

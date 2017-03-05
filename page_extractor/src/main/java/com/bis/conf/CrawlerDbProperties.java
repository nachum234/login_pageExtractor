package com.bis.conf;

import com.biscience.util.config.Property;
import com.biscience.util.config.PropertyDefinitions;

public class CrawlerDbProperties implements PropertyDefinitions {

    public static final Property DB_DRIVER_CLASS_MAME = new Property("explorer.db.classname", "com.exasol.jdbc.EXADriver", "JDBC driver class name");
    public static final Property DB_URL = new Property("explorer.db.url", "jdbc:exa:209.190.120.98..100:8563;schema=DEV_ADC2_OPS", "Database URL");
    public static final Property DB_USERNAME = new Property("explorer.db.username", "bis_dev", "Database user name");
    public static final Property DB_PASSWORD = new Property("explorer.db.password", "bis_dev_password", "Database user password");
}
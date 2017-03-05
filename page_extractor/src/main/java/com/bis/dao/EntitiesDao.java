package com.bis.dao;

import com.beust.jcommander.internal.Sets;
import com.bis.conf.CrawlerDbProperties;
import com.biscience.siteexplorer.config.SiteExplorerDBProperties;
import com.biscience.util.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import utils.CommonUtils;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class EntitiesDao {

    // logger
    private static Logger log = Logger.getLogger(EntitiesDao.class);

    private final static int NUM_ALIASES = 200;


    private final static int NUM_PUBLISHERS = 200000;


    // insert new alias
    private final static String INSERT_PUBLISHER_ALIAS = "merge into PUBS_REDIRECT pr using (select ? as al, ? as ent, ? as ent_id, now()) nr on pr.alias = nr.al when not matched then insert (alias, entity_domain, entity_id, creation_time) values (nr.al, nr.ent, nr.ent_id, now()) when matched then update set pr.entity_domain = nr.ent";


    // get all active publishers
    private final static String SELECT_ACTIVE_PUBLISHERS = "select \"domain\" from Entities where e_type in (1,8) and id in (select entity_id from Entities_Countries group by entity_id having max(status)>0) OR status = -2";


    // get all publishers aliases
    private final static String SELECT_PUBLISHERS_ALIASES = "select ALIAS, ENTITY_DOMAIN, ENTITY_ID from pubs_redirect";


    //=======================
    public static Map<String, Boolean> getAllActivePublishersAndSetions() {
        Map<String, Boolean> publishers = new ConcurrentHashMap<String, Boolean>(NUM_PUBLISHERS);

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(SELECT_ACTIVE_PUBLISHERS);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String publ = rs.getString(1);
                if (!StringUtils.isEmpty(publ)) {
                    try {
                        publ = CommonUtils.getDomainNoProtocolAndNoWWW(publ);
                        publ = publ.toLowerCase().trim();
                        if (!StringUtils.isEmpty(publ)) {
                            publishers.put(publ, true);
                        }

                    } catch (Exception e) {
                        log.error(" Cant get domain from publisher " + publ);
                    }

                }
            }

        } catch (SQLException sqle) {
            log.error("SQLException while loading all publishers from DB", sqle);
        } finally {
            SqlUtils.close(rs);
            SqlUtils.close(pstmt);
            SqlUtils.close(con);
        }

        return (publishers);
    }


    //=======================
    public static Map<String, Set<String>> getPublisherAliases() {
        Map<String, Set<String>> aliasesMap = new ConcurrentHashMap<String, Set<String>>(NUM_ALIASES);

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(SELECT_PUBLISHERS_ALIASES);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String redirect = rs.getString(1);
                redirect = CommonUtils.getDomainNoProtocolAndNoWWW(redirect);
                redirect = redirect.toLowerCase().trim();
                String publisher = rs.getString(2);
                publisher = CommonUtils.getDomainNoProtocolAndNoWWW(publisher);
                publisher = publisher.toLowerCase().trim();

                // if publisher exist
                if (aliasesMap.containsKey(publisher)) {
                    aliasesMap.get(publisher).add(redirect);
                } else {  // create and add the record
                    Set<String> set = Sets.newHashSet();
                    set.add(redirect);
                    aliasesMap.put(publisher, set);
                }
            }

        } catch (SQLException sqle) {
            log.error("SQLException while loading all publishers from DB", sqle);
        } finally {
            SqlUtils.close(rs);
            SqlUtils.close(pstmt);
            SqlUtils.close(con);
        }

        return (aliasesMap);
    }


    //=======================
    public static boolean addAlias(String alias, String publisher, long entityId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean result = false;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(INSERT_PUBLISHER_ALIAS);
            pstmt.setString(1, alias);
            pstmt.setString(2, publisher);
            pstmt.setLong(3, entityId);
            result = pstmt.execute();

        } catch (SQLException sqle) {
            log.error("SQLException while loading Geo-Rules from DB", sqle);
        } finally {
            SqlUtils.closeAll(con, pstmt, null);
        }

        return (result);
    }

    //=======================
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName(CrawlerDbProperties.DB_DRIVER_CLASS_MAME.getValue());
        } catch (ClassNotFoundException cnfe) {
            log.fatal("Could not load JDBC driver class " + SiteExplorerDBProperties.DB_DRIVER_CLASS_MAME.getValue() + " , ", cnfe);

            System.exit(0);
        }
        Connection con = DriverManager.getConnection(CrawlerDbProperties.DB_URL.getValue(), CrawlerDbProperties.DB_USERNAME.getValue(), CrawlerDbProperties.DB_PASSWORD.getValue());

        return (con);
    }

}

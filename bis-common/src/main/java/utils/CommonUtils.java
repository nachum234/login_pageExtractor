package utils;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Anna Kuranda on 2/22/2017.
 */
public class CommonUtils {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private static Logger logger = Logger.getLogger(CommonUtils.class);

    public static  String getDomainNoProtocolAndNoWWW(String url) {
        // clean up http://  and https:// from publisher domain
        String domain = null;
        try {
            domain  = url.replaceFirst("^(http://|https://)","");
            domain  = domain.replaceFirst("^(www.)","");
            if(domain.endsWith("/")){
                domain  = domain.replace("/","");
            }

        }catch(Exception e){
            logger.error(" Cant get domain from publisher "+url);
        }
        return domain;
    }


    public static final Set<Integer> redirectCodes = Stream.of(301,302,303,307).collect(Collectors.toSet());;


}

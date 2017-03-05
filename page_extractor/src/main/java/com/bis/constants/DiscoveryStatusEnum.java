package com.bis.constants;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yuri.lisnovsky
 */
public enum DiscoveryStatusEnum {

    IN_WORK(0),
    /* TODO - this status is abusing the response status of the url - there should be seperation between the url response status and the status of the url regarding ads found!
    * The change should be also here, but mostly in DB data and ETL - to seperate completely creatives status and status (which should relate only to response status */
    ADS_FOUND(1),                                   /* not status given by crawler, but Adclarity status regarding wether urls were found */
    NULL_WHILE_FETCHING_ERROR(2), 
    NO_ADDRESS_ASSOCIATED_WITH_HOSTNAME_ERROR(3),     
    DID_NOT_SEND_TO_ADEXTRACTOR(4),
    CONTINUE( 100),
    SWITCHING_PROTOCOLS(101),
    PROCESSING(102),
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NON_AUTHORITATIVE_INFORMATION(203),
    NO_CONTENT(204),
    RESET_CONTENT(205),
    PARTIAL_CONTENT(206),
    MULTI_STATUS(207),
    MULTIPLE_CHOICES(300),
    MOVED_PERMANENTLY(301),
    MOVED_TEMPORARILY(302),
    SC_SEE_OTHER(303),
    NOT_MODIFIED(304),
    USE_PROXY(305),
    TEMPORARY_REDIRECT(307),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    PAYMENT_REQUIRED(402),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    SC_NOT_ACCEPTABLE(406),
    PROXY_AUTHENTICATION_REQUIRED(407),
    SC_REQUEST_TIMEOUT(408),
    CONFLICT(409),
    GONE(410),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    SC_REQUEST_TOO_LONG(413),
    REQUEST_URI_TOO_LONG(414),
    UNSUPPORTED_MEDIA_TYPE(415),
    REQUESTED_RANGE_NOT_SATISFIABLE(416),
    EXPECTATION_FAILED(417),
    INSUFFICIENT_SPACE_ON_RESOURCE(419),
    METHOD_FAILURE(420),
    UNPROCESSABLE_ENTITY(422),
    LOCKED(423),
    FAILED_DEPENDENCY(424),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    HTTP_VERSION_NOT_SUPPORTED(505),
    INSUFFICIENT_STORAGE(507),
    
    PAGE_TOO_BIG(1001),
    FATAL_TRANSPORT_ERROR(1005),
    UNKNOWN_ERROR( 1006 );

    // Internal state
    private int discoveryStatus;
    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, DiscoveryStatusEnum> codeToStatusMapping;
    
    // Constructor
    private DiscoveryStatusEnum(final int discoveryStatus) {

//        for ( DiscoveryStatusEnum s : values() ) {
//            if( s.getDiscoveryStatus() == discoveryStatus ){
//                this.discoveryStatus  = discoveryStatus;
//                return;
//            }
//        }
        
        if( HttpStatus.SC_OK == discoveryStatus ){
            this.discoveryStatus = 0;
        } else {
            this.discoveryStatus = discoveryStatus;
        }
    }

    public int getDiscoveryStatus() {
        return discoveryStatus;
    }
    
    public static DiscoveryStatusEnum getDiscoveryStatusEnumObj( int discoveryStatus ) {
        if (codeToStatusMapping == null) {
            initMapping();
        }        
        if( codeToStatusMapping.containsKey( discoveryStatus )){
            return codeToStatusMapping.get( discoveryStatus );        
        }
        return DiscoveryStatusEnum.UNKNOWN_ERROR;
    }    
    
    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for ( DiscoveryStatusEnum s : values() ) {
            codeToStatusMapping.put(s.discoveryStatus, s);
        }
        codeToStatusMapping.put(HttpStatus.SC_OK, DiscoveryStatusEnum.IN_WORK );
    }    
}

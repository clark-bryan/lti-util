package com.trustwell.ltiutil;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LTILaunchRequest implements Serializable {

    private Map<String, String> authHeaders;
    private Map<String, String> requestParameters;
    private Map<String, String> consumerPair;
    private transient HttpServletRequest servletRequest;
    private String version;
    private String messageType;
    private String resourceLinkId;
    private String contextId;
    private String launchPresentationReturnUrl;
    private String toolConsumerInstanceGuid;
    private String email;
    private String name;
    private String contextTitle;
    private String roles;

    private static final String CONTEXT_TITLE = "context_title";
    private static final String CONTEXT_ID= "context_id";
    private static final String REQUEST_NAME = "lis_person_name_given";
    private static final String REQUEST_EMAIL = "lis_person_contact_email_primary";
    private static final String REQUEST_ROLES = "roles";
    private static final String LTI_VERSION = "lti_version";
    private static final String LTI_MESSAGE_TYPE = "lti_message_type";
    private static final String LTI_RESOURCE_LINK_ID = "resource_link_id";
    private static final String LTI_LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";
    private static final String LTI_TOOL_CONSUMER_INSTANCE_GUID = "tool_consumer_instance_guid";

    public LTILaunchRequest(Map<String, String> authHeaders, Map<String, String> requestParameters, Map<String, String> consumerPair, HttpServletRequest servletRequest) {
        this.authHeaders = authHeaders;
        this.requestParameters = requestParameters;
        this.consumerPair = consumerPair;
        this.servletRequest = servletRequest;
        version = servletRequest.getParameter(LTI_VERSION);
        messageType = servletRequest.getParameter(LTI_MESSAGE_TYPE);
        resourceLinkId = servletRequest.getParameter(LTI_RESOURCE_LINK_ID);
        contextId = servletRequest.getParameter(CONTEXT_ID);
        contextTitle = servletRequest.getParameter(CONTEXT_TITLE);
        name = servletRequest.getParameter(REQUEST_NAME);
        email = servletRequest.getParameter(REQUEST_EMAIL);
        roles = servletRequest.getParameter(REQUEST_ROLES);
        launchPresentationReturnUrl = servletRequest.getParameter(LTI_LAUNCH_PRESENTATION_RETURN_URL);
        toolConsumerInstanceGuid = servletRequest.getParameter(LTI_TOOL_CONSUMER_INSTANCE_GUID);
    }
}

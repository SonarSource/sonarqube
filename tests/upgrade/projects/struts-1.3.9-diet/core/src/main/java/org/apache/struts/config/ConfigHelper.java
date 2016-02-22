/*
 * $Id: ConfigHelper.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.config;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionFormBean;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p> NOTE: THIS CLASS IS UNDER ACTIVE DEVELOPMENT. THE CURRENT CODE IS
 * WRITTEN FOR CLARITY NOT EFFICIENCY. NOT EVERY API FUNCTION HAS BEEN
 * IMPLEMENTED YET. </p><p> A helper object to expose the Struts shared
 * resources, which are be stored in the application, session, or request
 * contexts, as appropriate. </p><p> An instance should be created for each
 * request processed. The  methods which return resources from the request or
 * session contexts are not thread-safe. </p><p> Provided for use by other
 * servlets in the application so they can easily access the Struts shared
 * resources. </p><p> The resources are stored under attributes in the
 * application, session, or request contexts. </p><p> The ActionConfig methods
 * simply return the resources from under the context and key used by the
 * Struts ActionServlet when the resources are created. </p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-14 02:09:06 -0400 (Sat, 14 May 2005)
 *          $
 * @since Struts 1.1
 */
public class ConfigHelper implements ConfigHelperInterface {
    // --------------------------------------------------------  Properites

    /**
     * <p> The application associated with this instance. </p>
     */
    private ServletContext application = null;

    /**
     * <p> The session associated with this instance. </p>
     */
    private HttpSession session = null;

    /**
     * <p> The request associated with this instance. </p>
     */
    private HttpServletRequest request = null;

    /**
     * <p> The response associated with this instance. </p>
     */
    private HttpServletResponse response = null;

    /**
     * <p> The forward associated with this instance. </p>
     */
    private ActionForward forward = null;

    public ConfigHelper() {
        super();
    }

    public ConfigHelper(ServletContext application, HttpServletRequest request,
        HttpServletResponse response) {
        super();
        this.setResources(application, request, response);
    }

    /**
     * <p> Set the application associated with this instance.
     * [servlet.getServletContext()] </p>
     */
    public void setApplication(ServletContext application) {
        this.application = application;
    }

    /**
     * <p> Set the session associated with this instance. </p>
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * <p> Set the request associated with this object. Session object is also
     * set or cleared. </p>
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;

        if (this.request == null) {
            setSession(null);
        } else {
            setSession(this.request.getSession());
        }
    }

    /**
     * <p> Set the response associated with this isntance. Session object is
     * also set or cleared. </p>
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * Set the forward associated with this instance.
     */
    public void setForward(ActionForward forward) {
        this.forward = forward;
    }

    /**
     * <p> Set the application and request for this object instance. The
     * ServletContext can be set by any servlet in the application. The
     * request should be the instant request. Most of the other methods
     * retrieve their own objects by reference to the application, request, or
     * session attributes. Do not call other methods without setting these
     * first! This is also called by the convenience constructor. </p>
     *
     * @param application - The associated ServletContext.
     * @param request     - The associated HTTP request.
     * @param response    - The associated HTTP response.
     */
    public void setResources(ServletContext application,
        HttpServletRequest request, HttpServletResponse response) {
        setApplication(application);
        setRequest(request);
        setResponse(response);
    }

    // ------------------------------------------------ Application Context
    public ActionMessages getActionMessages() {
        if (this.application == null) {
            return null;
        }

        return (ActionMessages) this.application.getAttribute(Globals.MESSAGE_KEY);
    }

    /**
     * <p> The application resources for this application. </p>
     */
    public MessageResources getMessageResources() {
        if (this.application == null) {
            return null;
        }

        return (MessageResources) this.application.getAttribute(Globals.MESSAGES_KEY);
    }

    /**
     * <p> The path-mapped pattern (<code>/action/*</code>) or extension
     * mapped pattern ((<code>*.do</code>) used to determine our Action URIs
     * in this application. </p>
     */
    public String getServletMapping() {
        if (this.application == null) {
            return null;
        }

        return (String) this.application.getAttribute(Globals.SERVLET_KEY);
    }

    // ---------------------------------------------------- Session Context

    /**
     * <p> The transaction token stored in this session, if it is used. </p>
     */
    public String getToken() {
        if (this.session == null) {
            return null;
        }

        return (String) session.getAttribute(Globals.TRANSACTION_TOKEN_KEY);
    }

    // ---------------------------------------------------- Request Context

    /**
     * <p> The runtime JspException that may be been thrown by a Struts tag
     * extension, or compatible presentation extension, and placed in the
     * request. </p>
     */
    public Throwable getException() {
        if (this.request == null) {
            return null;
        }

        return (Throwable) this.request.getAttribute(Globals.EXCEPTION_KEY);
    }

    /**
     * <p> The multipart object for this request. </p>
     */
    public MultipartRequestWrapper getMultipartRequestWrapper() {
        if (this.request == null) {
            return null;
        }

        return (MultipartRequestWrapper) this.request.getAttribute(Globals.MULTIPART_KEY);
    }

    /**
     * <p> The <code>org.apache.struts.ActionMapping</code> instance for this
     * request. </p>
     */
    public ActionMapping getMapping() {
        if (this.request == null) {
            return null;
        }

        return (ActionMapping) this.request.getAttribute(Globals.MAPPING_KEY);
    }

    // ---------------------------------------------------- Utility Methods

    /**
     * <p> Return true if a message string for the specified message key is
     * present for the user's Locale. </p>
     *
     * @param key Message key
     */
    public boolean isMessage(String key) {
        // Look up the requested MessageResources
        MessageResources resources = getMessageResources();

        if (resources == null) {
            return false;
        }

        // Return the requested message presence indicator
        return resources.isPresent(RequestUtils.getUserLocale(request, null),
            key);
    }

    /*
     * <p>
     * Retrieve and return the <code>ActionForm</code> bean associated with
     * this mapping, creating and stashing one if necessary.  If there is no
     * form bean associated with this mapping, return <code>null</code>.
     * </p>
     */
    public ActionForm getActionForm() {
        // Is there a mapping associated with this request?
        ActionMapping mapping = getMapping();

        if (mapping == null) {
            return (null);
        }

        // Is there a form bean associated with this mapping?
        String attribute = mapping.getAttribute();

        if (attribute == null) {
            return (null);
        }

        // Look up the existing form bean, if any
        ActionForm instance;

        if ("request".equals(mapping.getScope())) {
            instance = (ActionForm) this.request.getAttribute(attribute);
        } else {
            instance = (ActionForm) this.session.getAttribute(attribute);
        }

        return instance;
    }

    /**
     * <p> Return the form bean definition associated with the specified
     * logical name, if any; otherwise return <code>null</code>. </p>
     *
     * @param name Logical name of the requested form bean definition
     */
    public ActionFormBean getFormBean(String name) {
        return null;
    }

    /**
     * <p> Return the forwarding associated with the specified logical name,
     * if any; otherwise return <code>null</code>. </p>
     *
     * @param name Logical name of the requested forwarding
     */
    public ActionForward getActionForward(String name) {
        return null;
    }

    /**
     * <p> Return the mapping associated with the specified request path, if
     * any; otherwise return <code>null</code>. </p>
     *
     * @param path Request path for which a mapping is requested
     */
    public ActionMapping getActionMapping(String path) {
        return null;
    }

    /**
     * <p> Return the form action converted into an action mapping path.  The
     * value of the <code>action</code> property is manipulated as follows in
     * computing the name of the requested mapping:</p>
     *
     * <ul>
     *
     * <li>Any filename extension is removed (on the theory that extension
     * mapping is being used to select the controller servlet).</li>
     *
     * <li>If the resulting value does not start with a slash, then a slash is
     * prepended.</li>
     *
     * </ul>
     */
    public String getActionMappingName(String action) {
        String value = action;
        int question = action.indexOf("?");

        if (question >= 0) {
            value = value.substring(0, question);
        }

        int slash = value.lastIndexOf("/");
        int period = value.lastIndexOf(".");

        if ((period >= 0) && (period > slash)) {
            value = value.substring(0, period);
        }

        if (value.startsWith("/")) {
            return (value);
        } else {
            return ("/" + value);
        }
    }

    /**
     * <p> Return the form action converted into a server-relative URL. </p>
     */
    public String getActionMappingURL(String action) {
        StringBuffer value = new StringBuffer(this.request.getContextPath());

        // Use our servlet mapping, if one is specified
        String servletMapping = getServletMapping();

        if (servletMapping != null) {
            String queryString = null;
            int question = action.indexOf("?");

            if (question >= 0) {
                queryString = action.substring(question);
            }

            String actionMapping = getActionMappingName(action);

            if (servletMapping.startsWith("*.")) {
                value.append(actionMapping);
                value.append(servletMapping.substring(1));
            } else if (servletMapping.endsWith("/*")) {
                value.append(servletMapping.substring(0,
                        servletMapping.length() - 2));
                value.append(actionMapping);
            }

            if (queryString != null) {
                value.append(queryString);
            }
        }
        // Otherwise, assume extension mapping is in use and extension is
        // already included in the action property
        else {
            if (!action.startsWith("/")) {
                value.append("/");
            }

            value.append(action);
        }

        // Return the completed value
        return (value.toString());
    }

    /**
     * <p> Return the url encoded to maintain the user session, if any. </p>
     */
    public String getEncodeURL(String url) {
        if ((session != null) && (response != null)) {
            boolean redirect = false;

            if (forward != null) {
                redirect = forward.getRedirect();
            }

            if (redirect) {
                return response.encodeRedirectURL(url);
            } else {
                return response.encodeURL(url);
            }
        } else {
            return (url);
        }
    }

    // ------------------------------------------------ Presentation API

    /**
     * <p> Renders the reference for a HTML <base> element </p>
     */
    public String getOrigRef() {
        // HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        if (request == null) {
            return null;
        }

        StringBuffer result =
            RequestUtils.requestToServerUriStringBuffer(request);

        return result.toString();
    }

    /**
     * <p> Renders the reference for a HTML <base> element. </p>
     */
    public String getBaseRef() {
        if (request == null) {
            return null;
        }

        StringBuffer result = RequestUtils.requestToServerStringBuffer(request);
        String path;

        if (forward == null) {
            path = request.getRequestURI();
        } else {
            path = request.getContextPath() + forward.getPath();
        }

        result.append(path);

        return result.toString();
    }

    /**
     * <p> Return the path for the specified forward, otherwise return
     * <code>null</code>. </p>
     *
     * @param name Name given to local or global forward.
     */
    public String getLink(String name) {
        ActionForward forward = getActionForward(name);

        if (forward == null) {
            return null;
        }

        StringBuffer path = new StringBuffer(this.request.getContextPath());

        path.append(forward.getPath());

        // :TODO: What about runtime parameters?
        return getEncodeURL(path.toString());
    }

    /**
     * <p> Return the localized message for the specified key, otherwise
     * return <code>null</code>. </p>
     *
     * @param key Message key
     */
    public String getMessage(String key) {
        MessageResources resources = getMessageResources();

        if (resources == null) {
            return null;
        }

        return resources.getMessage(RequestUtils.getUserLocale(request, null),
            key);
    }

    /**
     * <p> Look up and return a message string, based on the specified
     * parameters. </p>
     *
     * @param key  Message key to be looked up and returned
     * @param args Replacement parameters for this message
     */
    public String getMessage(String key, Object[] args) {
        MessageResources resources = getMessageResources();

        if (resources == null) {
            return null;
        }

        // Return the requested message
        if (args == null) {
            return resources.getMessage(RequestUtils.getUserLocale(request, null),
                key);
        } else {
            return resources.getMessage(RequestUtils.getUserLocale(request, null),
                key, args);
        }
    }

    /**
     * <p> Return the URL for the specified ActionMapping, otherwise return
     * <code>null</code>. </p>
     *
     * @param path Name given to local or global forward.
     */
    public String getAction(String path) {
        return getEncodeURL(getActionMappingURL(path));
    }

    // --------------------------------------------- Presentation Wrappers

    /**
     * <p> Wrapper for getLink(String) </p>
     *
     * @param name Name given to local or global forward.
     */
    public String link(String name) {
        return getLink(name);
    }

    /**
     * <p> Wrapper for getMessage(String) </p>
     *
     * @param key Message key
     */
    public String message(String key) {
        return getMessage(key);
    }

    /**
     * <p> Wrapper for getMessage(String,Object[]) </p>
     *
     * @param key  Message key to be looked up and returned
     * @param args Replacement parameters for this message
     */
    public String message(String key, Object[] args) {
        return getMessage(key, args);
    }

    /**
     * <p> Wrapper for getAction(String) </p>
     *
     * @param path Name given to local or global forward.
     */
    public String action(String path) {
        return getAction(path);
    }
}

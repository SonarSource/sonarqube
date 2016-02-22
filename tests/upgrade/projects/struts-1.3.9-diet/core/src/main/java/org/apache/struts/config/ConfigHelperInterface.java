/*
 * $Id: ConfigHelperInterface.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionFormBean;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.struts.util.MessageResources;

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
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 * @since Struts 1.1
 */
public interface ConfigHelperInterface {
    // ------------------------------------------------ Application Context

    /**
     * <p> The <code>org.apache.struts.action.ActionFormBeans</code>
     * collection for this application. </p>
     */
    public ActionMessages getActionMessages();

    /**
     * <p> The application resources for this application. </p>
     */
    public MessageResources getMessageResources();

    /**
     * <p> The path-mapped pattern (<code>/action/*</code>) or extension
     * mapped pattern ((<code>*.do</code>) used to determine our Action URIs
     * in this application. </p>
     */
    public String getServletMapping();

    // ---------------------------------------------------- Session Context

    /**
     * <p> The transaction token stored in this session, if it is used. </p>
     */
    public String getToken();

    // ---------------------------------------------------- Request Context

    /**
     * <p> The runtime JspException that may be been thrown by a Struts tag
     * extension, or compatible presentation extension, and placed in the
     * request. </p>
     */
    public Throwable getException();

    /**
     * <p> The multipart object for this request. </p>
     */
    public MultipartRequestWrapper getMultipartRequestWrapper();

    /**
     * <p> The <code>org.apache.struts.ActionMapping</code> instance for this
     * request. </p>
     */
    public ActionMapping getMapping();

    // ---------------------------------------------------- Utility Methods

    /**
     * <p> Return true if a message string for the specified message key is
     * present for the user's Locale. </p>
     *
     * @param key Message key
     */
    public boolean isMessage(String key);

    /**
     * <p> Retrieve and return the <code>ActionForm</code> bean associated
     * with this mapping, creating and stashing one if necessary.  If there is
     * no form bean associated with this mapping, return <code>null</code>.
     * </p>
     */
    public ActionForm getActionForm();

    /**
     * <p> Return the form bean definition associated with the specified
     * logical name, if any; otherwise return <code>null</code>. </p>
     *
     * @param name Logical name of the requested form bean definition
     */
    public ActionFormBean getFormBean(String name);

    /**
     * <p> Return the forwarding associated with the specified logical name,
     * if any; otherwise return <code>null</code>. </p>
     *
     * @param name Logical name of the requested forwarding
     */
    public ActionForward getActionForward(String name);

    /**
     * <p> Return the mapping associated with the specified request path, if
     * any; otherwise return <code>null</code>. </p>
     *
     * @param path Request path for which a mapping is requested
     */
    public ActionMapping getActionMapping(String path);

    /**
     * <p> Return the form action converted into an action mapping path.  The
     * value of the <code>action</code> property is manipulated as follows in
     * computing the name of the requested mapping:
     *
     * <ul>
     *
     * <li>Any filename extension is removed (on the theory that extension
     * mapping is being used to select the controller servlet).</li>
     *
     * <li>If the resulting value does not start with a slash, then a slash is
     * prepended.</li>
     *
     * </ul> <p> :FIXME: Bad assumption =:o) </p>
     */
    public String getActionMappingName(String action);

    /**
     * <p> Return the form action converted into a server-relative URL. </p>
     */
    public String getActionMappingURL(String action);

    /**
     * <p> Return the url encoded to maintain the user session, if any. </p>
     */
    public String getEncodeURL(String url);

    // ------------------------------------------------ Presentation API

    /**
     * <p> Renders the reference for a HTML <base> element </p>
     */
    public String getOrigRef();

    /**
     * <p> Renders the reference for a HTML <base> element </p>
     */
    public String getBaseRef();

    /**
     * <p> Return the path for the specified forward, otherwise return
     * <code>null</code>. </p>
     *
     * @param name Name given to local or global forward.
     */
    public String getLink(String name);

    /**
     * <p> Return the localized message for the specified key, otherwise
     * return <code>null</code>. </p>
     *
     * @param key Message key
     */
    public String getMessage(String key);

    /**
     * <p> Look up and return a message string, based on the specified
     * parameters. </p>
     *
     * @param key  Message key to be looked up and returned
     * @param args Replacement parameters for this message
     */
    public String getMessage(String key, Object[] args);

    /**
     * <p> Return the URL for the specified ActionMapping, otherwise return
     * <code>null</code>. </p>
     *
     * @param path Name given to local or global forward.
     */
    public String getAction(String path);
}

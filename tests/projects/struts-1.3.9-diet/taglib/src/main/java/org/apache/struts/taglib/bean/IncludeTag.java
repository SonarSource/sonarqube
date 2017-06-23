/*
 * $Id: IncludeTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.bean;

import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Map;

/**
 * Define the contents of a specified intra-application request as a page
 * scope attribute of type <code>java.lang.String</code>.  If the current
 * request is part of a session, the session identifier will be included in
 * the generated request, so it will be part of the same session. <p>
 * <strong>FIXME</strong>:  In a servlet 2.3 environment, we can use a wrapped
 * response passed to RequestDispatcher.include().
 *
 * @version $Rev: 471754 $ $Date: 2005-08-21 19:08:45 -0400 (Sun, 21 Aug 2005)
 *          $
 */
public class IncludeTag extends TagSupport {
    // ------------------------------------------------------------- Properties

    /**
     * Buffer size to use when reading the input stream.
     */
    protected static final int BUFFER_SIZE = 256;

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    /**
     * The anchor to be added to the end of the generated hyperlink.
     */
    protected String anchor = null;

    /**
     * The name of the global <code>ActionForward</code> that contains a path
     * to our requested resource.
     */
    protected String forward = null;

    /**
     * The absolute URL to the resource to be included.
     */
    protected String href = null;

    /**
     * The name of the scripting variable that will be exposed as a page scope
     * attribute.
     */
    protected String id = null;

    /**
     * The context-relative URI of the page or servlet to be included.
     */
    protected String page = null;

    /**
     * Include transaction token (if any) in the hyperlink?
     */
    protected boolean transaction = false;
    protected boolean useLocalEncoding = false;

    public String getAnchor() {
        return (this.anchor);
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getForward() {
        return (this.forward);
    }

    public void setForward(String forward) {
        this.forward = forward;
    }

    public String getHref() {
        return (this.href);
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPage() {
        return (this.page);
    }

    public void setPage(String page) {
        this.page = page;
    }

    public boolean getTransaction() {
        return (this.transaction);
    }

    public void setTransaction(boolean transaction) {
        this.transaction = transaction;
    }

    public boolean isUseLocalEncoding() {
        return useLocalEncoding;
    }

    public void setUseLocalEncoding(boolean b) {
        useLocalEncoding = b;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Define the contents returned for the specified resource as a page scope
     * attribute.
     *
     * @throws JspException if a JSP error occurs
     */
    public int doStartTag() throws JspException {
        // Set up a URLConnection to read the requested resource
        Map params =
            TagUtils.getInstance().computeParameters(pageContext, null, null,
                null, null, null, null, null, transaction);

        // FIXME - <html:link> attributes
        String urlString = null;
        URL url = null;

        try {
            urlString =
                TagUtils.getInstance().computeURLWithCharEncoding(pageContext,
                    forward, href, page, null, null, params, anchor, false,
                    useLocalEncoding);

            if (urlString.indexOf(':') < 0) {
                HttpServletRequest request =
                    (HttpServletRequest) pageContext.getRequest();

                url = new URL(RequestUtils.requestURL(request), urlString);
            } else {
                url = new URL(urlString);
            }
        } catch (MalformedURLException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("include.url",
                    e.toString()));
        }

        URLConnection conn = null;

        try {
            // Set up the basic connection
            conn = url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);

            // Add a session id cookie if appropriate
            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();

            addCookie(conn, urlString, request);

            // Connect to the requested resource
            conn.connect();
        } catch (Exception e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("include.open",
                    url.toString(), e.toString()));
        }

        // Copy the contents of this URL
        StringBuffer sb = new StringBuffer();

        try {
            BufferedInputStream is =
                new BufferedInputStream(conn.getInputStream());
            InputStreamReader in = new InputStreamReader(is); // FIXME- encoding
            char[] buffer = new char[BUFFER_SIZE];
            int n = 0;

            while (true) {
                n = in.read(buffer);

                if (n < 1) {
                    break;
                }

                sb.append(buffer, 0, n);
            }

            in.close();
        } catch (Exception e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("include.read",
                    url.toString(), e.toString()));
        }

        // Define the retrieved content as a page scope attribute
        pageContext.setAttribute(id, sb.toString());

        // Skip any body of this tag
        return (SKIP_BODY);
    }

    /**
     * Add a session id cookie if appropriate. Can be overloaded to support a
     * cluster.
     *
     * @param conn
     * @param urlString
     * @param request
     * @since Struts 1.2.0
     */
    protected void addCookie(URLConnection conn, String urlString,
        HttpServletRequest request) {
        if ((conn instanceof HttpURLConnection)
            && urlString.startsWith(request.getContextPath())
            && (request.getRequestedSessionId() != null)
            && request.isRequestedSessionIdFromCookie()) {
            StringBuffer sb = new StringBuffer("JSESSIONID=");

            sb.append(request.getRequestedSessionId());
            conn.setRequestProperty("Cookie", sb.toString());
        }
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        anchor = null;
        forward = null;
        href = null;
        id = null;
        page = null;
        transaction = false;
    }
}

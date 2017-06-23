/*
 * $Id: MockHttpServletRequest.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.mock;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.BufferedReader;

import java.security.Principal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>Mock <strong>HttpServletRequest</strong> object for low-level unit tests
 * of Struts controller components.  Coarser grained tests should be
 * implemented in terms of the Cactus framework, instead of the mock object
 * classes.</p>
 *
 * <p><strong>WARNING</strong> - Only the minimal set of methods needed to
 * create unit tests is provided, plus additional methods to configure this
 * object as necessary.  Methods for unsupported operations will throw
 * <code>UnsupportedOperationException</code>.</p>
 *
 * <p><strong>WARNING</strong> - Because unit tests operate in a single
 * threaded environment, no synchronization is performed.</p>
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 */
public class MockHttpServletRequest implements HttpServletRequest {
    // ----------------------------------------------------- Instance Variables

    /**
     * <p> The set of request attributes. </p>
     */
    protected HashMap attributes = new HashMap();

    /**
     * <p> The context path for this request. </p>
     */
    protected String contextPath = null;

    /**
     * <p> The preferred locale for this request. </p>
     */
    protected Locale locale = null;

    /**
     * <p> The set of arrays of parameter values, keyed by parameter name.
     * </p>
     */
    protected HashMap parameters = new HashMap();

    /**
     * <p> The extra path information for this request. v     * </p>
     */
    protected String pathInfo = null;

    /**
     * <p> The authenticated user for this request. </p>
     */
    protected Principal principal = null;

    /**
     * <p> The query string for this request. </p>
     */
    protected String queryString = null;

    /**
     * <p> The servlet path for this request. </p>
     */
    protected String servletPath = null;

    /**
     * <p> The HttpSession with which we are associated. </p>
     */
    protected HttpSession session = null;

    /**
     * <p> The HTTP request method. </p>
     */
    protected String method = null;

    /**
     * <p> The Content Type for this request. </p>
     */
    protected String contentType = null;

    // ----------------------------------------------------------- Constructors
    public MockHttpServletRequest() {
        super();
    }

    public MockHttpServletRequest(HttpSession session) {
        super();
        setHttpSession(session);
    }

    public MockHttpServletRequest(String contextPath, String servletPath,
        String pathInfo, String queryString) {
        super();
        setPathElements(contextPath, servletPath, pathInfo, queryString);
    }

    public MockHttpServletRequest(String contextPath, String servletPath,
        String pathInfo, String queryString, HttpSession session) {
        super();
        setPathElements(contextPath, servletPath, pathInfo, queryString);
        setHttpSession(session);
    }

    // --------------------------------------------------------- Public Methods
    public void addParameter(String name, String value) {
        String[] values = (String[]) parameters.get(name);

        if (values == null) {
            String[] results = new String[] { value };

            parameters.put(name, results);

            return;
        }

        String[] results = new String[values.length + 1];

        System.arraycopy(values, 0, results, 0, values.length);
        results[values.length] = value;
        parameters.put(name, results);
    }

    public void setHttpSession(HttpSession session) {
        this.session = session;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setPathElements(String contextPath, String servletPath,
        String pathInfo, String queryString) {
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
    }

    public void setUserPrincipal(Principal principal) {
        this.principal = principal;
    }

    // --------------------------------------------- HttpServletRequest Methods
    public String getAuthType() {
        throw new UnsupportedOperationException();
    }

    public String getContextPath() {
        return (contextPath);
    }

    public Cookie[] getCookies() {
        throw new UnsupportedOperationException();
    }

    public long getDateHeader(String name) {
        throw new UnsupportedOperationException();
    }

    public String getHeader(String name) {
        throw new UnsupportedOperationException();
    }

    public Enumeration getHeaderNames() {
        throw new UnsupportedOperationException();
    }

    public Enumeration getHeaders(String name) {
        throw new UnsupportedOperationException();
    }

    public int getIntHeader(String name) {
        throw new UnsupportedOperationException();
    }

    public String getMethod() {
        return (method);
    }

    public String getPathInfo() {
        return (pathInfo);
    }

    public String getPathTranslated() {
        throw new UnsupportedOperationException();
    }

    public String getQueryString() {
        return (queryString);
    }

    public String getRemoteUser() {
        if (principal != null) {
            return (principal.getName());
        } else {
            return (null);
        }
    }

    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
    }

    public String getRequestURI() {
        StringBuffer sb = new StringBuffer();

        if (contextPath != null) {
            sb.append(contextPath);
        }

        if (servletPath != null) {
            sb.append(servletPath);
        }

        if (pathInfo != null) {
            sb.append(pathInfo);
        }

        if (sb.length() > 0) {
            return (sb.toString());
        }

        throw new UnsupportedOperationException();
    }

    public StringBuffer getRequestURL() {
        throw new UnsupportedOperationException();
    }

    public String getServletPath() {
        return (servletPath);
    }

    public HttpSession getSession() {
        return (getSession(true));
    }

    public HttpSession getSession(boolean create) {
        if (create && (session == null)) {
            session = new MockHttpSession();

            // modified to act like the real deal,
            // call with (false) if you want null
            // throw new UnsupportedOperationException();
        }

        return (session);
    }

    public Principal getUserPrincipal() {
        return (principal);
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException();
    }

    public boolean isUserInRole(String role) {
        if ((principal != null) && (principal instanceof MockPrincipal)) {
            return (((MockPrincipal) principal).isUserInRole(role));
        } else {
            return (false);
        }
    }

    // ------------------------------------------------- ServletRequest Methods
    public Object getAttribute(String name) {
        return (attributes.get(name));
    }

    public Enumeration getAttributeNames() {
        return (new MockEnumeration(attributes.keySet().iterator()));
    }

    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    public int getContentLength() {
        throw new UnsupportedOperationException();
    }

    public String getContentType() {
        return (contentType);
    }

    public ServletInputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    public Locale getLocale() {
        return (locale);
    }

    public Enumeration getLocales() {
        throw new UnsupportedOperationException();
    }

    public String getParameter(String name) {
        String[] values = (String[]) parameters.get(name);

        if (values != null) {
            return (values[0]);
        } else {
            return (null);
        }
    }

    public Map getParameterMap() {
        return (parameters);
    }

    public Enumeration getParameterNames() {
        return (new MockEnumeration(parameters.keySet().iterator()));
    }

    public String[] getParameterValues(String name) {
        return ((String[]) parameters.get(name));
    }

    public String getProtocol() {
        throw new UnsupportedOperationException();
    }

    public BufferedReader getReader() {
        throw new UnsupportedOperationException();
    }

    public String getRealPath(String path) {
        throw new UnsupportedOperationException();
    }

    public String getRemoteAddr() {
        throw new UnsupportedOperationException();
    }

    public String getRemoteHost() {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException();
    }

    public String getScheme() {
        return ("http");
    }

    public String getServerName() {
        return ("localhost");
    }

    public int getServerPort() {
        return (8080);
    }

    public boolean isSecure() {
        return (false);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    public void setCharacterEncoding(String name) {
        throw new UnsupportedOperationException();
    }
}

/*
 * $Id: MockPageContext.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * <p>Mock <strong>ServletContext</strong> object for low-level unit tests of
 * Struts controller components.  Coarser grained tests should be implemented
 * in terms of the Cactus framework, instead of the mock object classes.</p>
 *
 * <p><strong>WARNING</strong> - Only the minimal set of methods needed to
 * create unit tests is provided, plus additional methods to configure this
 * object as necessary.  Methods for unsupported operations will throw
 * <code>UnsupportedOperationException</code>.</p>
 *
 * <p><strong>WARNING</strong> - Because unit tests operate in a single
 * threaded environment, no synchronization is performed.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:45:39 -0400 (Sat, 07 May 2005)
 *          $
 */
public class MockPageContext extends PageContext {
    // ----------------------------------------------------- Instance Variables
    protected ServletContext application = null;
    protected HashMap attributes = new HashMap(); // Page scope attributes
    protected ServletConfig config = null;
    protected ServletRequest request = null;
    protected ServletResponse response = null;
    protected HttpSession session = null;
    private boolean throwIOException;
    private boolean returnBodyContent;

    // ----------------------------------------------------------- Constructors
    public MockPageContext() {
        super();
    }

    public MockPageContext(ServletConfig config, ServletRequest request,
        ServletResponse response) {
        super();
        setValues(config, request, response);
    }

    /**
     * <p> Construct a new PageContext impl. </p>
     *
     * @param throwIOException Determines if the returned JspWriter should
     *                         throw an IOException on any method call.
     * @param returnBody       Determines if getOut() should return a new
     *                         <code>JspWriter</code> or a <code>BodyContent</code>.
     */
    public MockPageContext(boolean throwIOException, boolean returnBody) {
        this.throwIOException = throwIOException;
        this.returnBodyContent = returnBody;
    }

    private void checkAndThrow()
        throws IOException {
        if (throwIOException) {
            throw new IOException();
        }
    }

    // --------------------------------------------------------- Public Methods
    public void setValues(ServletConfig config, ServletRequest request,
        ServletResponse response) {
        this.config = config;

        if (config != null) {
            this.application = config.getServletContext();
        } else {
            this.application = null;
        }

        this.request = request;
        this.response = response;

        if (request != null) {
            session = ((HttpServletRequest) request).getSession(false);
        } else {
            this.session = null;
        }
    }

    // ---------------------------------------------------- PageContext Methods
    public Object findAttribute(String name) {
        Object value = getAttribute(name, PageContext.PAGE_SCOPE);

        if (value == null) {
            value = getAttribute(name, PageContext.REQUEST_SCOPE);
        }

        if (value == null) {
            value = getAttribute(name, PageContext.SESSION_SCOPE);
        }

        if (value == null) {
            value = getAttribute(name, PageContext.APPLICATION_SCOPE);
        }

        return (value);
    }

    public void forward(String path) {
        throw new UnsupportedOperationException();
    }

    public Object getAttribute(String name) {
        return (getAttribute(name, PageContext.PAGE_SCOPE));
    }

    public Object getAttribute(String name, int scope) {
        if (scope == PageContext.PAGE_SCOPE) {
            return (attributes.get(name));
        } else if (scope == PageContext.REQUEST_SCOPE) {
            if (request != null) {
                return (request.getAttribute(name));
            }

            return (null);
        } else if (scope == PageContext.SESSION_SCOPE) {
            if (session != null) {
                return (session.getAttribute(name));
            }

            return (null);
        } else if (scope == PageContext.APPLICATION_SCOPE) {
            if (application != null) {
                return (application.getAttribute(name));
            }

            return (null);
        } else {
            throw new IllegalArgumentException("Invalid scope " + scope);
        }
    }

    public Enumeration getAttributeNamesInScope(int scope) {
        if (scope == PageContext.PAGE_SCOPE) {
            return (new MockEnumeration(attributes.keySet().iterator()));
        } else if (scope == PageContext.REQUEST_SCOPE) {
            if (request != null) {
                return (request.getAttributeNames());
            }

            return (new MockEnumeration(Collections.EMPTY_LIST.iterator()));
        } else if (scope == PageContext.SESSION_SCOPE) {
            if (session != null) {
                return (session.getAttributeNames());
            }

            return (new MockEnumeration(Collections.EMPTY_LIST.iterator()));
        } else if (scope == PageContext.APPLICATION_SCOPE) {
            if (application != null) {
                return (application.getAttributeNames());
            }

            return new MockEnumeration(Collections.EMPTY_LIST.iterator());
        } else {
            throw new IllegalArgumentException("Invalid scope " + scope);
        }
    }

    public int getAttributesScope(String name) {
        if (attributes.get(name) != null) {
            return (PageContext.PAGE_SCOPE);
        } else if ((request != null) && (request.getAttribute(name) != null)) {
            return (PageContext.REQUEST_SCOPE);
        } else if ((session != null) && (session.getAttribute(name) != null)) {
            return (PageContext.SESSION_SCOPE);
        } else if ((application != null)
            && (application.getAttribute(name) != null)) {
            return (PageContext.APPLICATION_SCOPE);
        } else {
            return (0);
        }
    }

    public Exception getException() {
        throw new UnsupportedOperationException();
    }

    /**
     * <p> Custom JspWriter that throws the specified exception (supplied on
     * the constructor...if any), else it simply returns. </p>
     */
    public JspWriter getOut() {
        JspWriter jspWriter =
            new JspWriter(0, false) {
                public void print(String s)
                    throws IOException {
                    checkAndThrow();
                }

                public void newLine()
                    throws IOException {
                    checkAndThrow();
                }

                public void print(boolean b)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(char c)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(int i)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(long l)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(float f)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(double d)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(char[] s)
                    throws IOException {
                    checkAndThrow();
                }

                public void print(Object obj)
                    throws IOException {
                    checkAndThrow();
                }

                public void println()
                    throws IOException {
                    checkAndThrow();
                }

                public void println(boolean x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(char x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(int x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(long x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(float x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(double x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(char[] x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(String x)
                    throws IOException {
                    checkAndThrow();
                }

                public void println(Object x)
                    throws IOException {
                    checkAndThrow();
                }

                public void clear()
                    throws IOException {
                    checkAndThrow();
                }

                public void clearBuffer()
                    throws IOException {
                    checkAndThrow();
                }

                public void flush()
                    throws IOException {
                    checkAndThrow();
                }

                public void close()
                    throws IOException {
                    checkAndThrow();
                }

                public int getRemaining() {
                    return 0;
                }

                public void write(char[] cbuf, int off, int len)
                    throws IOException {
                    checkAndThrow();
                }
            };

        if (returnBodyContent) {
            return new BodyContent(jspWriter) {
                    public Reader getReader() {
                        return null;
                    }

                    public String getString() {
                        return null;
                    }

                    public void writeOut(Writer out)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void newLine()
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(boolean b)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(char c)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(int i)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(long l)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(float f)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(double d)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(char[] s)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(String s)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void print(Object obj)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println()
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(boolean x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(char x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(int x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(long x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(float x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(double x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(char[] x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(String x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void println(Object x)
                        throws IOException {
                        checkAndThrow();
                    }

                    public void clear()
                        throws IOException {
                        checkAndThrow();
                    }

                    public void clearBuffer()
                        throws IOException {
                        checkAndThrow();
                    }

                    public void close()
                        throws IOException {
                        checkAndThrow();
                    }

                    public int getRemaining() {
                        return 0;
                    }

                    public void write(char[] cbuf, int off, int len)
                        throws IOException {
                        checkAndThrow();
                    }
                };
        }

        return jspWriter;
    }

    public Object getPage() {
        throw new UnsupportedOperationException();
    }

    public ServletRequest getRequest() {
        return (this.request);
    }

    public ServletResponse getResponse() {
        return (this.response);
    }

    public ServletConfig getServletConfig() {
        return (this.config);
    }

    public ServletContext getServletContext() {
        return (this.application);
    }

    public HttpSession getSession() {
        return (this.session);
    }

    public void handlePageException(Exception e) {
        throw new UnsupportedOperationException();
    }

    public void handlePageException(Throwable t) {
        throw new UnsupportedOperationException();
    }

    public void include(String path) {
        throw new UnsupportedOperationException();
    }

    public void initialize(Servlet servlet, ServletRequest request,
        ServletResponse response, String errorPageURL, boolean needsSession,
        int bufferSize, boolean autoFlush) {
        throw new UnsupportedOperationException();
    }

    public JspWriter popBody() {
        throw new UnsupportedOperationException();
    }

    public BodyContent pushBody() {
        throw new UnsupportedOperationException();
    }

    public void release() {
        throw new UnsupportedOperationException();
    }

    public void removeAttribute(String name) {
        int scope = getAttributesScope(name);

        if (scope != 0) {
            removeAttribute(name, scope);
        }
    }

    public void removeAttribute(String name, int scope) {
        if (scope == PageContext.PAGE_SCOPE) {
            attributes.remove(name);
        } else if (scope == PageContext.REQUEST_SCOPE) {
            if (request != null) {
                request.removeAttribute(name);
            }
        } else if (scope == PageContext.SESSION_SCOPE) {
            if (session != null) {
                session.removeAttribute(name);
            }
        } else if (scope == PageContext.APPLICATION_SCOPE) {
            if (application != null) {
                application.removeAttribute(name);
            }
        } else {
            throw new IllegalArgumentException("Invalid scope " + scope);
        }
    }

    public void setAttribute(String name, Object value) {
        setAttribute(name, value, PageContext.PAGE_SCOPE);
    }

    public void setAttribute(String name, Object value, int scope) {
        if (scope == PageContext.PAGE_SCOPE) {
            attributes.put(name, value);
        } else if (scope == PageContext.REQUEST_SCOPE) {
            if (request != null) {
                request.setAttribute(name, value);
            }
        } else if (scope == PageContext.SESSION_SCOPE) {
            if (session != null) {
                session.setAttribute(name, value);
            }
        } else if (scope == PageContext.APPLICATION_SCOPE) {
            if (application != null) {
                application.setAttribute(name, value);
            }
        } else {
            throw new IllegalArgumentException("Invalid scope " + scope);
        }
    }
}

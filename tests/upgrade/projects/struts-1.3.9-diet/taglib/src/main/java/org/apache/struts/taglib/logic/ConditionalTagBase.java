/*
 * $Id: ConditionalTagBase.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.logic;

import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Abstract base class for the various conditional evaluation tags.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public abstract class ConditionalTagBase extends TagSupport {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.logic.LocalStrings");

    // ------------------------------------------------------------- Properties

    /**
     * The name of the cookie to be used as a variable.
     */
    protected String cookie = null;

    /**
     * The name of the HTTP request header to be used as a variable.
     */
    protected String header = null;

    /**
     * The name of the JSP bean to be used as a variable (if
     * <code>property</code> is not specified), or whose property is to be
     * accessed (if <code>property</code> is specified).
     */
    protected String name = null;

    /**
     * The name of the HTTP request parameter to be used as a variable.
     */
    protected String parameter = null;

    /**
     * The name of the bean property to be used as a variable.
     */
    protected String property = null;

    /**
     * The name of the security role to be checked for.
     */
    protected String role = null;

    /**
     * The scope to search for the bean named by the name property, or "any
     * scope" if null.
     */
    protected String scope = null;

    /**
     * The user principal name to be checked for.
     */
    protected String user = null;

    public String getCookie() {
        return (this.cookie);
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getHeader() {
        return (this.header);
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameter() {
        return (this.parameter);
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getRole() {
        return (this.role);
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUser() {
        return (this.user);
    }

    public void setUser(String user) {
        this.user = user;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Perform the test required for this particular tag, and either evaluate
     * or skip the body of this tag.
     *
     * @throws JspException if a JSP exception occurs
     */
    public int doStartTag() throws JspException {
        if (condition()) {
            return (EVAL_BODY_INCLUDE);
        } else {
            return (SKIP_BODY);
        }
    }

    /**
     * Evaluate the remainder of the current page normally.
     *
     * @throws JspException if a JSP exception occurs
     */
    public int doEndTag() throws JspException {
        return (EVAL_PAGE);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        cookie = null;
        header = null;
        name = null;
        parameter = null;
        property = null;
        role = null;
        scope = null;
        user = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Evaluate the condition that is being tested by this particular tag, and
     * return <code>true</code> if the nested body content of this tag should
     * be evaluated, or <code>false</code> if it should be skipped. This
     * method must be implemented by concrete subclasses.
     *
     * @throws JspException if a JSP exception occurs
     */
    protected abstract boolean condition()
        throws JspException;
}

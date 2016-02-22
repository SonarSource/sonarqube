/*
 * $Id: HeaderTag.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Define a scripting variable based on the value(s) of the specified header
 * received with this request.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class HeaderTag extends TagSupport {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    // ------------------------------------------------------------- Properties

    /**
     * The name of the scripting variable that will be exposed as a page scope
     * attribute.
     */
    protected String id = null;

    /**
     * Return an array of header values if <code>multiple</code> is non-null.
     */
    protected String multiple = null;

    /**
     * The name of the header whose value is to be exposed.
     */
    protected String name = null;

    /**
     * The default value to return if no header of the specified name is
     * found.
     */
    protected String value = null;

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMultiple() {
        return (this.multiple);
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve the required property and expose it as a scripting variable.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        if (this.multiple == null) {
            this.handleSingleHeader();
        } else {
            this.handleMultipleHeaders();
        }

        return SKIP_BODY;
    }

    /**
     * Expose an array of header values.
     *
     * @throws JspException
     * @since Struts 1.2
     */
    protected void handleMultipleHeaders()
        throws JspException {
        ArrayList values = new ArrayList();
        Enumeration items =
            ((HttpServletRequest) pageContext.getRequest()).getHeaders(name);

        while (items.hasMoreElements()) {
            values.add(items.nextElement());
        }

        if (values.isEmpty() && (this.value != null)) {
            values.add(this.value);
        }

        String[] headers = new String[values.size()];

        if (headers.length == 0) {
            JspException e =
                new JspException(messages.getMessage("header.get", name));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        pageContext.setAttribute(id, values.toArray(headers));
    }

    /**
     * Expose a single header value.
     *
     * @throws JspException
     * @since Struts 1.2
     */
    protected void handleSingleHeader()
        throws JspException {
        String value =
            ((HttpServletRequest) pageContext.getRequest()).getHeader(name);

        if ((value == null) && (this.value != null)) {
            value = this.value;
        }

        if (value == null) {
            JspException e =
                new JspException(messages.getMessage("header.get", name));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        pageContext.setAttribute(id, value);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        id = null;
        multiple = null;
        name = null;
        value = null;
    }
}

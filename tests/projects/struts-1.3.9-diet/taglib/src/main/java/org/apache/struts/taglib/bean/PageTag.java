/*
 * $Id: PageTag.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Define a scripting variable that exposes the requested page context item as
 * a scripting variable and a page scope bean.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class PageTag extends TagSupport {
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
     * The name of the page context property to be retrieved.
     */
    protected String property = null;

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve the required configuration object and expose it as a scripting
     * variable.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Retrieve the requested object to be exposed
        Object object = null;

        if ("application".equalsIgnoreCase(property)) {
            object = pageContext.getServletContext();
        } else if ("config".equalsIgnoreCase(property)) {
            object = pageContext.getServletConfig();
        } else if ("request".equalsIgnoreCase(property)) {
            object = pageContext.getRequest();
        } else if ("response".equalsIgnoreCase(property)) {
            object = pageContext.getResponse();
        } else if ("session".equalsIgnoreCase(property)) {
            object = pageContext.getSession();
        } else {
            JspException e =
                new JspException(messages.getMessage("page.selector", property));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Expose this value as a scripting variable
        pageContext.setAttribute(id, object);

        return (SKIP_BODY);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        id = null;
        property = null;
    }
}

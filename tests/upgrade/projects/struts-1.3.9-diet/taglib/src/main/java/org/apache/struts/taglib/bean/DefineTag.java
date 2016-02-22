/*
 * $Id: DefineTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Define a scripting variable based on the value(s) of the specified bean
 * property.
 *
 * @version $Rev: 471754 $ $Date: 2005-06-15 12:16:32 -0400 (Wed, 15 Jun 2005)
 *          $
 */
public class DefineTag extends BodyTagSupport {
    /**
     * Commons logging instance.
     */
    private static final Log log = LogFactory.getLog(DefineTag.class);

    // ---------------------------------------------------- Protected variables

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    /**
     * The body content of this tag (if any).
     */
    protected String body = null;

    // ------------------------------------------------------------- Properties

    /**
     * The name of the scripting variable that will be exposed as a page scope
     * attribute.
     */
    protected String id = null;

    /**
     * The name of the bean owning the property to be exposed.
     */
    protected String name = null;

    /**
     * The name of the property to be retrieved.
     */
    protected String property = null;

    /**
     * The scope within which to search for the specified bean.
     */
    protected String scope = null;

    /**
     * The scope within which the newly defined bean will be creatd.
     */
    protected String toScope = null;

    /**
     * The fully qualified Java class name of the value to be exposed.
     */
    protected String type = null;

    /**
     * The (String) value to which the defined bean will be set.
     */
    protected String value = null;

    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getToScope() {
        return (this.toScope);
    }

    public void setToScope(String toScope) {
        this.toScope = toScope;
    }

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Check if we need to evaluate the body of the tag
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        return (EVAL_BODY_TAG);
    }

    /**
     * Save the body content of this tag (if any), or throw a JspException if
     * the value was already defined.
     *
     * @throws JspException if value was defined by an attribute
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            body = bodyContent.getString();

            if (body != null) {
                body = body.trim();
            }

            if (body.length() < 1) {
                body = null;
            }
        }

        return (SKIP_BODY);
    }

    /**
     * Retrieve the required property and expose it as a scripting variable.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Enforce restriction on ways to declare the new value
        int n = 0;

        if (this.body != null) {
            n++;
        }

        if (this.name != null) {
            n++;
        }

        if (this.value != null) {
            n++;
        }

        if (n > 1) {
            JspException e =
                new JspException(messages.getMessage("define.value", id));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Retrieve the required property value
        Object value = this.value;

        if ((value == null) && (name != null)) {
            value =
                TagUtils.getInstance().lookup(pageContext, name, property, scope);
        }

        if ((value == null) && (body != null)) {
            value = body;
        }

        if (value == null) {
            JspException e =
                new JspException(messages.getMessage("define.null", id));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Expose this value as a scripting variable
        int inScope = PageContext.PAGE_SCOPE;

        try {
            if (toScope != null) {
                inScope = TagUtils.getInstance().getScope(toScope);
            }
        } catch (JspException e) {
            log.warn("toScope was invalid name so we default to PAGE_SCOPE", e);
        }

        pageContext.setAttribute(id, value, inScope);

        // Continue processing this page
        return (EVAL_PAGE);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        body = null;
        id = null;
        name = null;
        property = null;
        scope = null;
        toScope = "page";
        type = null;
        value = null;
    }
}

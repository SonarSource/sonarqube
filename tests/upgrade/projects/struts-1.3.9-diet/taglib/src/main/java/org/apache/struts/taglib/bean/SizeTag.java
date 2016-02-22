/*
 * $Id: SizeTag.java 471754 2006-11-06 14:55:09Z husted $
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
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Map;

/**
 * Define a scripting variable that will contain the number of elements found
 * in a specified array, Collection, or Map.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class SizeTag extends TagSupport {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    // ------------------------------------------------------------- Properties

    /**
     * The actual collection to be counted.
     */
    protected Object collection = null;

    /**
     * The name of the scripting variable that will be exposed as a page scope
     * attribute.
     */
    protected String id = null;

    /**
     * The name of the bean owning the property to be counted.
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

    public Object getCollection() {
        return (this.collection);
    }

    public void setCollection(Object collection) {
        this.collection = collection;
    }

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

    // --------------------------------------------------------- Public Methods

    /**
     * Retrieve the required property and expose it as a scripting variable.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Retrieve the required property value
        Object value = this.collection;

        if (value == null) {
            if (name == null) {
                // Must specify either a collection attribute or a name
                // attribute.
                JspException e =
                    new JspException(messages.getMessage(
                            "size.noCollectionOrName"));

                TagUtils.getInstance().saveException(pageContext, e);
                throw e;
            }

            value =
                TagUtils.getInstance().lookup(pageContext, name, property, scope);
        }

        // Identify the number of elements, based on the collection type
        int size = 0;

        if (value == null) {
            JspException e =
                new JspException(messages.getMessage("size.collection"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        } else if (value.getClass().isArray()) {
            size = Array.getLength(value);
        } else if (value instanceof Collection) {
            size = ((Collection) value).size();
        } else if (value instanceof Map) {
            size = ((Map) value).size();
        } else {
            JspException e =
                new JspException(messages.getMessage("size.collection"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        // Expose this size as a scripting variable
        pageContext.setAttribute(this.id, new Integer(size),
            PageContext.PAGE_SCOPE);

        return (SKIP_BODY);
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        collection = null;
        id = null;
        name = null;
        property = null;
        scope = null;
    }
}

/*
 * $Id: NestedWriteNestingTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested;

import org.apache.struts.taglib.TagUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * NestedWriteNestingTag.
 *
 * Created so developers could have a more elegant way of getting to the
 * underlying nested property their tag properties are referencing.
 *
 * @version $Rev: 471754 $
 * @since Struts 1.1
 */
public class NestedWriteNestingTag extends BodyTagSupport {
    /* the usual private member variables */
    private boolean filter = false;
    private String property = null;
    private String id = null;
    private String originalProperty = null;

    /**
     * Getter method for the <i>property</i> property
     *
     * @return String value of the property property
     */
    public String getProperty() {
        return this.property;
    }

    /**
     * Setter method for the <i>property</i> property
     *
     * @param newProperty new value for the property property
     */
    public void setProperty(String newProperty) {
        this.property = newProperty;
    }

    /**
     * Getter method for the <i>id</i> property
     *
     * @return String value for the id property
     */
    public String getId() {
        return id;
    }

    /**
     * Setter method for the <i>id</i> property
     *
     * @param id new value for the id property
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter method for the <i>filter</i> property
     *
     * @return String value of the filter property
     */
    public boolean getFilter() {
        return this.filter;
    }

    /**
     * Setter method for the <i>filter</i> property
     *
     * @param newFilter new value for the filter property
     */
    public void setFilter(boolean newFilter) {
        this.filter = newFilter;
    }

    /**
     * Overriding method of the heart of the tag. Gets the relative property
     * and tells the JSP engine to evaluate its body content.
     *
     * @return int JSP continuation directive.
     */
    public int doStartTag() throws JspException {
        // set the original property
        originalProperty = property;

        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();
        String nesting =
            NestedPropertyHelper.getAdjustedProperty(request, property);

        if (id != null) {
            // use it as a scripting variable instead
            pageContext.setAttribute(id, nesting);
        } else {
            /* write output, filtering if required */
            if (this.filter) {
                TagUtils.getInstance().write(pageContext,
                    TagUtils.getInstance().filter(nesting));
            } else {
                TagUtils.getInstance().write(pageContext, nesting);
            }
        }

        /* continue with page processing */
        return (SKIP_BODY);
    }

    public int doEndTag() throws JspException {
        // do the super thing
        int i = super.doEndTag();

        // reset the property
        property = originalProperty;

        // complete
        return i;
    }

    /**
     * JSP method to release all resources held by the tag.
     */
    public void release() {
        super.release();
        this.filter = false;
        this.property = null;
        this.originalProperty = null;
    }
}

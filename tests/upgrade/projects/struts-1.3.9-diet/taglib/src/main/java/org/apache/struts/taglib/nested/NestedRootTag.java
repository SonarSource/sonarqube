/*
 * $Id: NestedRootTag.java 471754 2006-11-06 14:55:09Z husted $
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
 * NestedRootTag.
 *
 * The only other addition in this nested suite of tags. This tag allows for a
 * nested structure to start without relying on the bean and workings of the
 * FormTag. Useful for view pages that don't update when returning to the
 * server, or use hyperlinks rather than form submits.
 *
 * The Bean that it uses can come out of a jsp:useBean tag or define another
 * bean that's already in scope. As long as the other Struts tags can find the
 * bean by name, it'll work.
 *
 * It's simply recognised by the helper class and it's property is added to
 * the nesting list.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NestedRootTag extends BodyTagSupport implements NestedNameSupport {
    /* usual member variables */
    private String name = null;
    private String originalName = "";
    private String originalNesting = "";
    private String originalNestingName = "";

    /**
     * Getter method for the <i>property</i> property
     *
     * @return String value of the property property
     */
    public String getProperty() {
        return "";
    }

    /**
     * Setter method for the <i>property</i> property
     *
     * @param property new value for the property property
     */
    public void setProperty(String property) {
    }

    /**
     * Getter method for the <i>name</i> property
     *
     * @return String value of the name property
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter method for the <i>name</i> property
     *
     * @param name new value for the name property
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Overriding method of the heart of the tag. Gets the relative property
     * and tells the JSP engine to evaluate its body content.
     *
     * @return int JSP continuation directive.
     */
    public int doStartTag() throws JspException {
        /* set the nested reference for possible inclusions etc */
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        // get al the originals
        originalName = name;
        originalNesting = NestedPropertyHelper.getCurrentProperty(request);
        originalNestingName =
            NestedPropertyHelper.getCurrentName(request, this);

        // set what we have to
        if (name != null) {
            NestedPropertyHelper.setProperty(request, "");
            NestedPropertyHelper.setName(request, this.name);
        }

        // do the JSP thing
        return (EVAL_BODY_TAG);
    }

    /**
     * Render the resulting content evaluation.
     *
     * @return int JSP continuation directive.
     */
    public int doAfterBody() throws JspException {
        /* Render the output */
        if (bodyContent != null) {
            TagUtils.getInstance().writePrevious(pageContext,
                bodyContent.getString());
            bodyContent.clearBody();
        }

        return (SKIP_BODY);
    }

    /**
     * Evaluate the rest of the page
     *
     * @return int JSP continuation directive.
     */
    public int doEndTag() throws JspException {
        /* reset the reference */
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        if (originalNesting == null) {
            NestedPropertyHelper.deleteReference(request);
        } else {
            NestedPropertyHelper.setName(request, originalNestingName);
            NestedPropertyHelper.setProperty(request, originalNesting);
        }

        this.name = originalName;

        return (EVAL_PAGE);
    }

    /**
     * JSP method to release all resources held by the tag.
     */
    public void release() {
        super.release();
        this.name = null;
        this.originalName = null;
        this.originalNesting = null;
        this.originalNestingName = null;
    }
}

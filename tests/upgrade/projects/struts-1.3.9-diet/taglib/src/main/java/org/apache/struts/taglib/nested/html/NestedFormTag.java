/*
 * $Id: NestedFormTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested.html;

import org.apache.struts.taglib.html.FormTag;
import org.apache.struts.taglib.nested.NestedNameSupport;
import org.apache.struts.taglib.nested.NestedPropertyHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

/**
 * NestedFormTag.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NestedFormTag extends FormTag implements NestedNameSupport {
    //TODO: name property was removed from FormTag but appears to be required
    //      for the nested version to work. See if it can be removed
    //      from here altogether.

    /**
     * The name
     */
    protected String name = null;

    // original nesting environment
    private String originalNesting = null;
    private String originalNestingName = null;

    /**
     * Return the name.
     */
    public String getName() {
        return (this.name);
    }

    /**
     * Set the name.
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the string value of the "property" property.
     *
     * @return the property property
     */
    public String getProperty() {
        return "";
    }

    /**
     * Setter for the "property" property
     *
     * @param newProperty new value for the property
     */
    public void setProperty(String newProperty) {
    }

    /**
     * Overriding to allow the chance to set the details of the system, so
     * that dynamic includes can be possible
     *
     * @return int JSP continuation directive.
     */
    public int doStartTag() throws JspException {
        // store original result
        int temp = super.doStartTag();

        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        // original nesting details
        originalNesting = NestedPropertyHelper.getCurrentProperty(request);
        originalNestingName =
            NestedPropertyHelper.getCurrentName(request, this);

        // some new details
        NestedPropertyHelper.setProperty(request, null);
        NestedPropertyHelper.setName(request, super.getBeanName());

        // continue
        return temp;
    }

    /**
     * This is only overriden to clean up the include reference
     *
     * @return int JSP continuation directive.
     */
    public int doEndTag() throws JspException {
        // super result
        int temp = super.doEndTag();

        // all done. clean up
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        // reset the original nesting values
        if (originalNesting == null) {
            NestedPropertyHelper.deleteReference(request);
        } else {
            NestedPropertyHelper.setProperty(request, originalNesting);
            NestedPropertyHelper.setName(request, originalNestingName);
        }

        // return the super result
        return temp;
    }

    /**
     * Release the tag's resources and reset the values.
     */
    public void release() {
        // let the super release
        super.release();

        // reset the original value place holders
        originalNesting = null;
        originalNestingName = null;
    }
}

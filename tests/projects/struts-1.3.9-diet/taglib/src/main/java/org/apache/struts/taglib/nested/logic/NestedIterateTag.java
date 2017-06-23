/*
 * $Id: NestedIterateTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested.logic;

import org.apache.struts.taglib.logic.IterateTag;
import org.apache.struts.taglib.nested.NestedNameSupport;
import org.apache.struts.taglib.nested.NestedPropertyHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import java.util.Map;

/**
 * NestedIterateTag. Slightly more complex that the other extensions. This one
 * has to yield a proper index property. Very taxing.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NestedIterateTag extends IterateTag implements NestedNameSupport {
    // The current nesting
    private String nesting = null;

    // original tag properties
    private String originalName = null;
    private String originalProperty = null;

    // original nesting environment
    private String originalNesting = null;
    private String originalNestingName = null;

    /**
     * Overriding method of the heart of the matter. Gets the relative
     * property and leaves the rest up to the original tag implementation.
     * Sweet.
     *
     * @return int JSP continuation directive. This is in the hands of the
     *         super class.
     */
    public int doStartTag() throws JspException {
        // original values
        originalName = getName();
        originalProperty = getProperty();

        // set the ID to make the super tag happy
        if ((id == null) || (id.trim().length() == 0)) {
            id = property;
        }

        // the request object
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        // original nesting details
        originalNesting = NestedPropertyHelper.getCurrentProperty(request);
        originalNestingName =
            NestedPropertyHelper.getCurrentName(request, this);

        // set the bean if it's been provided
        // (the bean that's been provided! get it!?... nevermind)
        if (getName() == null) {
            // the qualified nesting value
            nesting =
                NestedPropertyHelper.getAdjustedProperty(request, getProperty());
        } else {
            // it's just the property
            nesting = getProperty();
        }

        // set the properties
        NestedPropertyHelper.setNestedProperties(request, this);

        // get the original result
        int temp = super.doStartTag();

        // set the new reference (including the index etc)
        NestedPropertyHelper.setName(request, getName());
        NestedPropertyHelper.setProperty(request, deriveNestedProperty());

        // return the result
        return temp;
    }

    /**
     * The only added property to the class. For use in proper nesting.
     *
     * @return String value of the property and the current index or mapping.
     */
    private String deriveNestedProperty() {
        Object idObj = pageContext.getAttribute(id);

        if (idObj instanceof Map.Entry) {
            return nesting + "(" + ((Map.Entry) idObj).getKey() + ")";
        } else {
            return nesting + "[" + this.getIndex() + "]";
        }
    }

    /**
     * This is only overriden as the include reference will need it's index
     * updated.
     *
     * @return int JSP continuation directive.
     */
    public int doAfterBody() throws JspException {
        // store original result
        int temp = super.doAfterBody();
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        if (temp != SKIP_BODY) {
            // set the new reference
            NestedPropertyHelper.setProperty(request, deriveNestedProperty());
        }

        // return super result
        return temp;
    }

    /**
     * Complete the processing of the tag. The nested tags here will restore
     * all the original value for the tag itself and the nesting context.
     *
     * @return int to describe the next step for the JSP processor
     * @throws JspException for the bad things JSP's do
     */
    public int doEndTag() throws JspException {
        // the super's thing
        int i = super.doEndTag();

        // request
        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();

        // reset the original tag values
        super.setName(originalName);
        super.setProperty(originalProperty);

        // reset the original nesting values
        if (originalNesting == null) {
            NestedPropertyHelper.deleteReference(request);
        } else {
            NestedPropertyHelper.setProperty(request, originalNesting);
            NestedPropertyHelper.setName(request, originalNestingName);
        }

        // job done
        return i;
    }

    /**
     * Release the tag's resources and reset the values.
     */
    public void release() {
        // let the super release
        super.release();

        // reset the original value place holders
        originalName = null;
        originalProperty = null;
        originalNesting = null;
        originalNestingName = null;
    }
}

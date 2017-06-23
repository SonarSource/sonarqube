/*
 * $Id: HiddenTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.html;

import org.apache.struts.taglib.TagUtils;

import javax.servlet.jsp.JspException;

/**
 * Custom tag for input fields of type "hidden".
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class HiddenTag extends BaseFieldTag {
    // ------------------------------------------------------------- Properties

    /**
     * Should the value of this field also be rendered to the response?
     */
    protected boolean write = false;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this tag.
     */
    public HiddenTag() {
        super();
        this.type = "hidden";
    }

    public boolean getWrite() {
        return (this.write);
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Generate the required input tag, followed by the optional rendered
     * text. Support for <code>write</code> property since Struts 1.1.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Render the <html:input type="hidden"> tag as before
        super.doStartTag();

        // Is rendering the value separately requested?
        if (!write) {
            return (EVAL_BODY_TAG);
        }

        // Calculate the value to be rendered separately
        // * @since Struts 1.1
        String results = null;

        if (value != null) {
            results = TagUtils.getInstance().filter(value);
        } else {
            Object value =
                TagUtils.getInstance().lookup(pageContext, name, property, null);

            if (value == null) {
                results = "";
            } else {
                results = TagUtils.getInstance().filter(value.toString());
            }
        }

        TagUtils.getInstance().write(pageContext, results);

        return (EVAL_BODY_TAG);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        write = false;
    }
}

/*
 * $Id: TextareaTag.java 471754 2006-11-06 14:55:09Z husted $
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
 * Custom tag for input fields of type "textarea".
 *
 * @version $Rev: 471754 $ $Date: 2005-04-06 02:37:00 -0400 (Wed, 06 Apr 2005)
 *          $
 */
public class TextareaTag extends BaseInputTag {
    // ----------------------------------------------------- Constructor
    public TextareaTag() {
        super();
        doReadonly = true;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Generate the required input tag. Support for indexed since Struts 1.1
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        TagUtils.getInstance().write(pageContext, this.renderTextareaElement());

        return (EVAL_BODY_TAG);
    }

    /**
     * Generate an HTML &lt;textarea&gt; tag.
     *
     * @throws JspException
     * @since Struts 1.1
     */
    protected String renderTextareaElement()
        throws JspException {
        StringBuffer results = new StringBuffer("<textarea");

        prepareAttribute(results, "name", prepareName());
        prepareAttribute(results, "accesskey", getAccesskey());
        prepareAttribute(results, "tabindex", getTabindex());
        prepareAttribute(results, "cols", getCols());
        prepareAttribute(results, "rows", getRows());
        results.append(prepareEventHandlers());
        results.append(prepareStyles());
        prepareOtherAttributes(results);
        results.append(">");

        results.append(this.renderData());

        results.append("</textarea>");

        return results.toString();
    }

    /**
     * Renders the value displayed in the &lt;textarea&gt; tag.
     *
     * @throws JspException
     * @since Struts 1.1
     */
    protected String renderData()
        throws JspException {
        String data = this.value;

        if (data == null) {
            data = this.lookupProperty(this.name, this.property);
        }

        return (data == null) ? "" : TagUtils.getInstance().filter(data);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        name = Constants.BEAN_KEY;
    }
}

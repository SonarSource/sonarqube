/*
 * $Id: SubmitTag.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;

/**
 * Tag for input fields of type "submit".
 *
 * @version $Rev: 471754 $ $Date: 2005-04-06 02:22:39 -0400 (Wed, 06 Apr 2005)
 *          $
 */
public class SubmitTag extends BaseHandlerTag {
    // ----------------------------------------------------- Instance Variables

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    /**
     * The name of the generated input field.
     */
    protected String property = null;

    /**
     * The body content of this tag (if any).
     */
    protected String text = null;

    /**
     * The value of the button label.
     */
    protected String value = null;

    // ------------------------------------------------------------- Properties

    /**
     * Return the property.
     */
    public String getProperty() {
        return (this.property);
    }

    /**
     * Set the property name.
     *
     * @param property The property name
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Return the label value.
     */
    public String getValue() {
        return (this.value);
    }

    /**
     * Set the label value.
     *
     * @param value The label value
     */
    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the start of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Do nothing until doEndTag() is called
        this.text = null;

        return (EVAL_BODY_TAG);
    }

    /**
     * Save the associated label from the body content.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            String value = bodyContent.getString().trim();

            if (value.length() > 0) {
                text = value;
            }
        }

        return (SKIP_BODY);
    }

    /**
     * Process the end of this tag. <p> Support for Indexed property since
     * Struts 1.1
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Generate an HTML element
        StringBuffer results = new StringBuffer();

        results.append(getElementOpen());
        prepareAttribute(results, "name", prepareName());
        prepareButtonAttributes(results);
        results.append(prepareEventHandlers());
        results.append(prepareStyles());
        prepareOtherAttributes(results);
        results.append(getElementClose());

        TagUtils.getInstance().write(pageContext, results.toString());

        return (EVAL_PAGE);
    }

    /**
     * Render the opening element.
     *
     * @return The opening part of the element.
     */
    protected String getElementOpen() {
        return "<input type=\"submit\"";
    }

    /**
     * Prepare the name element
     *
     * @return The element name.
     */
    protected String prepareName()
        throws JspException {
        if (property == null) {
            return null;
        }

        // * @since Struts 1.1
        if (indexed) {
            StringBuffer results = new StringBuffer();

            results.append(property);
            prepareIndex(results, null);

            return results.toString();
        }

        return property;
    }

    /**
     * Render the button attributes
     *
     * @param results The StringBuffer that output will be appended to.
     */
    protected void prepareButtonAttributes(StringBuffer results)
        throws JspException {
        prepareAttribute(results, "accesskey", getAccesskey());
        prepareAttribute(results, "tabindex", getTabindex());
        prepareValue(results);
    }

    /**
     * Render the value element
     *
     * @param results The StringBuffer that output will be appended to.
     */
    protected void prepareValue(StringBuffer results) {
        // Acquire the label value we will be generating
        String label = value;

        if ((label == null) && (text != null)) {
            label = text;
        }

        if ((label == null) || (label.length() < 1)) {
            label = getDefaultValue();
        }

        prepareAttribute(results, "value", label);
    }

    /**
     * Return the default value.
     *
     * @return The default value if none supplied.
     */
    protected String getDefaultValue() {
        return "Submit";
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        property = null;
        text = null;
        value = null;
    }
}

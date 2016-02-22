/*
 * $Id: MultiboxTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import java.lang.reflect.InvocationTargetException;

/**
 * Tag for input fields of type "checkbox".  This differs from CheckboxTag
 * because it assumes that the underlying property is an array getter (of any
 * supported primitive type, or String), and the checkbox is initialized to
 * "checked" if the value listed for the "value" attribute is present in the
 * values returned by the property getter.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class MultiboxTag extends BaseHandlerTag {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ----------------------------------------------------- Instance Variables

    /**
     * The constant String value to be returned when this checkbox is selected
     * and the form is submitted.
     */
    protected String constant = null;

    /**
     * The name of the bean containing our underlying property.
     */
    protected String name = Constants.BEAN_KEY;

    /**
     * The property name for this field.
     */
    protected String property = null;

    /**
     * The value which will mark this checkbox as "checked" if present in the
     * array returned by our property getter.
     */
    protected String value = null;

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    // ------------------------------------------------------------- Properties

    /**
     * Return the property name.
     */
    public String getProperty() {
        return (this.property);
    }

    /**
     * Set the property name.
     *
     * @param property The new property name
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Return the server value.
     */
    public String getValue() {
        return (this.value);
    }

    /**
     * Set the server value.
     *
     * @param value The new server value
     */
    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the beginning of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Defer processing until the end of this tag is encountered
        this.constant = null;

        return (EVAL_BODY_TAG);
    }

    /**
     * Save the body contents of this tag as the constant that we will be
     * returning.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            this.constant = bodyContent.getString().trim();
        }

        if ("".equals(this.constant)) {
            this.constant = null;
        }

        return SKIP_BODY;
    }

    /**
     * Render an input element for this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Create an appropriate "input" element based on our parameters
        StringBuffer results = new StringBuffer("<input type=\"checkbox\"");

        prepareAttribute(results, "name", prepareName());
        prepareAttribute(results, "accesskey", getAccesskey());
        prepareAttribute(results, "tabindex", getTabindex());

        String value = prepareValue(results);

        prepareChecked(results, value);
        results.append(prepareEventHandlers());
        results.append(prepareStyles());
        prepareOtherAttributes(results);
        results.append(getElementClose());

        TagUtils.getInstance().write(pageContext, results.toString());

        return EVAL_PAGE;
    }

    /**
     * Prepare the name element
     *
     * @return The element name.
     */
    protected String prepareName()
        throws JspException {
        return property;
    }

    /**
     * Render the value element
     *
     * @param results The StringBuffer that output will be appended to.
     */
    protected String prepareValue(StringBuffer results)
        throws JspException {
        String value = (this.value == null) ? this.constant : this.value;

        if (value == null) {
            JspException e =
                new JspException(messages.getMessage("multiboxTag.value"));

            pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                PageContext.REQUEST_SCOPE);
            throw e;
        }

        prepareAttribute(results, "value", TagUtils.getInstance().filter(value));

        return value;
    }

    /**
     * Render the checked element
     *
     * @param results The StringBuffer that output will be appended to.
     */
    protected void prepareChecked(StringBuffer results, String value)
        throws JspException {
        Object bean = TagUtils.getInstance().lookup(pageContext, name, null);
        String[] values = null;

        if (bean == null) {
            throw new JspException(messages.getMessage("getter.bean", name));
        }

        try {
            values = BeanUtils.getArrayProperty(bean, property);

            if (values == null) {
                values = new String[0];
            }
        } catch (IllegalAccessException e) {
            throw new JspException(messages.getMessage("getter.access",
                    property, name));
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();

            throw new JspException(messages.getMessage("getter.result",
                    property, t.toString()));
        } catch (NoSuchMethodException e) {
            throw new JspException(messages.getMessage("getter.method",
                    property, name));
        }

        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                results.append(" checked=\"checked\"");

                break;
            }
        }
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        constant = null;
        name = Constants.BEAN_KEY;
        property = null;
        value = null;
    }
}

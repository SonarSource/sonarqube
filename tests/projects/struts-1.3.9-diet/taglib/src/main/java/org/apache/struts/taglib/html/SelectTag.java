/*
 * $Id: SelectTag.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;

import java.lang.reflect.InvocationTargetException;

/**
 * Custom tag that represents an HTML select element, associated with a bean
 * property specified by our attributes.  This tag must be nested inside a
 * form tag.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class SelectTag extends BaseHandlerTag {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ----------------------------------------------------- Instance Variables

    /**
     * The actual values we will match against, calculated in doStartTag().
     */
    protected String[] match = null;

    /**
     * Should multiple selections be allowed.  Any non-null value will trigger
     * rendering this.
     */
    protected String multiple = null;

    /**
     * The name of the bean containing our underlying property.
     */
    protected String name = Constants.BEAN_KEY;

    /**
     * The property name we are associated with.
     */
    protected String property = null;

    /**
     * The saved body content of this tag.
     */
    protected String saveBody = null;

    /**
     * How many available options should be displayed when this element is
     * rendered?
     */
    protected String size = null;

    /**
     * The value to compare with for marking an option selected.
     */
    protected String value = null;

    public String getMultiple() {
        return (this.multiple);
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return (this.size);
    }

    public void setSize(String size) {
        this.size = size;
    }

    // ------------------------------------------------------------- Properties

    /**
     * Does the specified value match one of those we are looking for?
     *
     * @param value Value to be compared.
     */
    public boolean isMatched(String value) {
        if ((this.match == null) || (value == null)) {
            return false;
        }

        for (int i = 0; i < this.match.length; i++) {
            if (value.equals(this.match[i])) {
                return true;
            }
        }

        return false;
    }

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
     * Return the comparison value.
     */
    public String getValue() {
        return (this.value);
    }

    /**
     * Set the comparison value.
     *
     * @param value The new comparison value
     */
    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Render the beginning of this select tag. <p> Support for indexed
     * property since Struts 1.1
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        TagUtils.getInstance().write(pageContext, renderSelectStartElement());

        // Store this tag itself as a page attribute
        pageContext.setAttribute(Constants.SELECT_KEY, this);

        this.calculateMatchValues();

        return (EVAL_BODY_TAG);
    }

    /**
     * Create an appropriate select start element based on our parameters.
     *
     * @throws JspException
     * @since Struts 1.1
     */
    protected String renderSelectStartElement()
        throws JspException {
        StringBuffer results = new StringBuffer("<select");

        prepareAttribute(results, "name", prepareName());
        prepareAttribute(results, "accesskey", getAccesskey());

        if (multiple != null) {
            results.append(" multiple=\"multiple\"");
        }

        prepareAttribute(results, "size", getSize());
        prepareAttribute(results, "tabindex", getTabindex());
        results.append(prepareEventHandlers());
        results.append(prepareStyles());
        prepareOtherAttributes(results);
        results.append(">");

        return results.toString();
    }

    /**
     * Calculate the match values we will actually be using.
     *
     * @throws JspException
     */
    private void calculateMatchValues()
        throws JspException {
        if (this.value != null) {
            this.match = new String[1];
            this.match[0] = this.value;
        } else {
            Object bean =
                TagUtils.getInstance().lookup(pageContext, name, null);

            if (bean == null) {
                JspException e =
                    new JspException(messages.getMessage("getter.bean", name));

                TagUtils.getInstance().saveException(pageContext, e);
                throw e;
            }

            try {
                this.match = BeanUtils.getArrayProperty(bean, property);

                if (this.match == null) {
                    this.match = new String[0];
                }
            } catch (IllegalAccessException e) {
                TagUtils.getInstance().saveException(pageContext, e);
                throw new JspException(messages.getMessage("getter.access",
                        property, name));
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();

                TagUtils.getInstance().saveException(pageContext, t);
                throw new JspException(messages.getMessage("getter.result",
                        property, t.toString()));
            } catch (NoSuchMethodException e) {
                TagUtils.getInstance().saveException(pageContext, e);
                throw new JspException(messages.getMessage("getter.method",
                        property, name));
            }
        }
    }

    /**
     * Save any body content of this tag, which will generally be the
     * option(s) representing the values displayed to the user.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            String value = bodyContent.getString();

            if (value == null) {
                value = "";
            }

            this.saveBody = value.trim();
        }

        return (SKIP_BODY);
    }

    /**
     * Render the end of this form.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Remove the page scope attributes we created
        pageContext.removeAttribute(Constants.SELECT_KEY);

        // Render a tag representing the end of our current form
        StringBuffer results = new StringBuffer();

        if (saveBody != null) {
            results.append(saveBody);
            saveBody = null;
        }

        results.append("</select>");

        TagUtils.getInstance().write(pageContext, results.toString());

        return (EVAL_PAGE);
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

            prepareIndex(results, name);
            results.append(property);

            return results.toString();
        }

        return property;
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        match = null;
        multiple = null;
        name = Constants.BEAN_KEY;
        property = null;
        saveBody = null;
        size = null;
        value = null;
    }
}

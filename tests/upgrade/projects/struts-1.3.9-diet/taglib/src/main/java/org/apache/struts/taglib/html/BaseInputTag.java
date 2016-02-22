/*
 * $Id: BaseInputTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;

/**
 * Abstract base class for the various input tags.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public abstract class BaseInputTag extends BaseHandlerTag {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ----------------------------------------------------- Instance Variables

    /**
     * The number of character columns for this field, or negative for no
     * limit.
     */
    protected String cols = null;

    /**
     * The maximum number of characters allowed, or negative for no limit.
     */
    protected String maxlength = null;

    /**
     * The name of the field (and associated property) being processed.
     */
    protected String property = null;

    /**
     * The number of rows for this field, or negative for no limit.
     */
    protected String rows = null;

    /**
     * The value for this field, or <code>null</code> to retrieve the
     * corresponding property from our associated bean.
     */
    protected String value = null;

    /**
     * The name of the bean containing our underlying property.
     */
    protected String name = Constants.BEAN_KEY;

    // ------------------------------------------------------------- Properties
    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the number of columns for this field.
     */
    public String getCols() {
        return (this.cols);
    }

    /**
     * Set the number of columns for this field.
     *
     * @param cols The new number of columns
     */
    public void setCols(String cols) {
        this.cols = cols;
    }

    /**
     * Return the maximum length allowed.
     */
    public String getMaxlength() {
        return (this.maxlength);
    }

    /**
     * Set the maximum length allowed.
     *
     * @param maxlength The new maximum length
     */
    public void setMaxlength(String maxlength) {
        this.maxlength = maxlength;
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
     * Return the number of rows for this field.
     */
    public String getRows() {
        return (this.rows);
    }

    /**
     * Set the number of rows for this field.
     *
     * @param rows The new number of rows
     */
    public void setRows(String rows) {
        this.rows = rows;
    }

    /**
     * Return the size of this field (synonym for <code>getCols()</code>).
     */
    public String getSize() {
        return (getCols());
    }

    /**
     * Set the size of this field (synonym for <code>setCols()</code>).
     *
     * @param size The new size
     */
    public void setSize(String size) {
        setCols(size);
    }

    /**
     * Return the field value (if any).
     */
    public String getValue() {
        return (this.value);
    }

    /**
     * Set the field value (if any).
     *
     * @param value The new field value, or <code>null</code> to retrieve the
     *              corresponding property from the bean
     */
    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the start of this tag.  The default implementation does
     * nothing.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        return (EVAL_BODY_TAG);
    }

    /**
     * Process the end of this tag.  The default implementation does nothing.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
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
        name = Constants.BEAN_KEY;
        cols = null;
        maxlength = null;
        property = null;
        rows = null;
        value = null;
    }
}

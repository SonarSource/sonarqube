/*
 * $Id: CancelTag.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.jsp.JspException;

/**
 * Tag for input fields of type "cancel".
 *
 * @version $Rev: 471754 $ $Date: 2004-10-17 02:40:12 -0400 (Sun, 17 Oct 2004)
 *          $
 */
public class CancelTag extends SubmitTag {
    // --------------------------------------------------------- Constructor
    public CancelTag() {
        super();
        property = Constants.CANCEL_PROPERTY;
    }

    // ------------------------------------------------------------- Properties

    /**
     * Returns the onClick event handler.
     */
    public String getOnclick() {
        return (super.getOnclick() == null) ? "bCancel=true;" : super
        .getOnclick();
    }

    // --------------------------------------------------------- Protected Methods

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
        return property;
    }

    /**
     * Return the default value.
     *
     * @return The default value if none supplied.
     */
    protected String getDefaultValue() {
        return "Cancel";
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        property = Constants.CANCEL_PROPERTY;
    }
}

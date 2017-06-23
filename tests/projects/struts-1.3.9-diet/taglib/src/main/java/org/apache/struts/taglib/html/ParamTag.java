/*
 * $Id: ParamTag.java 482912 2006-12-06 05:48:32Z pbenedict $
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
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * Adds a new request parameter to its parent {@link LinkTag}.
 *
 * @version $Rev: 482912 $ $Date: 2006-12-06 06:48:32 +0100 (Wed, 06 Dec 2006) $
 * @since Struts 1.3.6
 */
public class ParamTag extends BodyTagSupport {

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    /**
     * The name of the query parameter.
     */
    protected String name = null;

    /**
     * The value of the query parameter or body content of this tag (if any).
     */
    protected String value = null;

    // ----------------------------------------------------- Constructor

    public ParamTag() {
        super();
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        return (EVAL_BODY_TAG);
    }

    /**
     * Save the associated from the body content.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (this.bodyContent != null) {
            String value = this.bodyContent.getString().trim();
            if (value.length() > 0) {
                this.value = value;
            }
        }
        return (SKIP_BODY);
    }

    /**
     * Render the end of the hyperlink.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        Tag tag = findAncestorWithClass(this, LinkTag.class);
        if (tag != null) {
            ((LinkTag)tag).addParameter(this.name, this.value);
        } else {
            throw new JspException(messages.getMessage("linkParamTag.linkParam"));
        }
        return (EVAL_PAGE);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        this.name = null;
        this.value = null;
    }
}

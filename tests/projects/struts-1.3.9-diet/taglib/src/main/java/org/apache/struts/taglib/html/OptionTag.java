/*
 * $Id: OptionTag.java 479633 2006-11-27 14:25:35Z pbenedict $
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

import org.apache.struts.Globals;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Tag for select options.  The body of this tag is presented to the user in
 * the option list, while the value attribute is the value returned to the
 * server if this option is selected.
 *
 * @version $Rev: 479633 $ $Date: 2005-08-21 19:08:45 -0400 (Sun, 21 Aug 2005)
 *          $
 */
public class OptionTag extends BodyTagSupport {
    // ----------------------------------------------------- Instance Variables

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");


    /**
     * The message text to be displayed to the user for this tag (if any)
     */
    protected String text = null;

    // ------------------------------------------------------------- Properties

    /**
     * The name of the servlet context attribute containing our message
     * resources.
     */
    protected String bundle = Globals.MESSAGES_KEY;

    /**
     * Is this option disabled?
     */
    protected boolean disabled = false;

    /**
     * Should the label be filtered for HTML sensitive characters?
     */
    protected boolean filter = false;

    /**
     * The key used to look up the text displayed to the user for this option,
     * if any.
     */
    protected String key = null;

    /**
     * The name of the attribute containing the Locale to be used for looking
     * up internationalized messages.
     */
    protected String locale = Globals.LOCALE_KEY;

    /**
     * The style associated with this tag.
     */
    private String style = null;

    /**
     * The named style class associated with this tag.
     */
    private String styleClass = null;

    /**
     * The identifier associated with this tag.
     */
    protected String styleId = null;

    /**
     * The language code of this element.
     */
    private String lang = null;
    
    /**
     * The direction for weak/neutral text of this element.
     */
    private String dir = null;

    /**
     * The server value for this option, also used to match against the
     * current property value to determine whether this option should be
     * marked as selected.
     */
    protected String value = null;

    public String getBundle() {
        return (this.bundle);
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public boolean getDisabled() {
        return (this.disabled);
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean getFilter() {
        return (this.filter);
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public String getKey() {
        return (this.key);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLocale() {
        return (this.locale);
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Return the style identifier for this tag.
     */
    public String getStyleId() {
        return (this.styleId);
    }

    /**
     * Set the style identifier for this tag.
     *
     * @param styleId The new style identifier
     */
    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

    public String getValue() {
        return (this.value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the language code of this element.
     * 
     * @since Struts 1.3.6
     */
    public String getLang() {
        return this.lang;
    }

    /**
     * Sets the language code of this element.
     * 
     * @since Struts 1.3.6
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * Returns the direction for weak/neutral text this element.
     * 
     * @since Struts 1.3.6
     */
    public String getDir() {
        return this.dir;
    }

    /**
     * Sets the direction for weak/neutral text of this element.
     * 
     * @since Struts 1.3.6
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the start of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Initialize the placeholder for our body content
        this.text = null;

        // Do nothing until doEndTag() is called
        return (EVAL_BODY_TAG);
    }

    /**
     * Process the body text of this tag (if any).
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            String text = bodyContent.getString();

            if (text != null) {
                text = text.trim();

                if (text.length() > 0) {
                    this.text = text;
                }
            }
        }

        return (SKIP_BODY);
    }

    /**
     * Process the end of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        TagUtils.getInstance().write(pageContext, this.renderOptionElement());

        return (EVAL_PAGE);
    }

    /**
     * Generate an HTML %lt;option&gt; element.
     *
     * @throws JspException
     * @since Struts 1.1
     */
    protected String renderOptionElement()
        throws JspException {
        StringBuffer results = new StringBuffer("<option value=\"");

        if (filter) {
            results.append(TagUtils.getInstance().filter(this.value));
        }
        else {
            results.append(this.value);
        }
        results.append("\"");

        if (disabled) {
            results.append(" disabled=\"disabled\"");
        }

        if (this.selectTag().isMatched(this.value)) {
            results.append(" selected=\"selected\"");
        }

        if (style != null) {
            results.append(" style=\"");
            results.append(style);
            results.append("\"");
        }

        if (styleId != null) {
            results.append(" id=\"");
            results.append(styleId);
            results.append("\"");
        }

        if (styleClass != null) {
            results.append(" class=\"");
            results.append(styleClass);
            results.append("\"");
        }

        if (dir != null) {
            results.append(" dir=\"");
            results.append(dir);
            results.append("\"");
        }

        if (lang != null) {
            results.append(" lang=\"");
            results.append(lang);
            results.append("\"");
        }

        results.append(">");

        results.append(text());

        results.append("</option>");

        return results.toString();
    }

    /**
     * Acquire the select tag we are associated with.
     *
     * @throws JspException
     */
    private SelectTag selectTag()
        throws JspException {
        SelectTag selectTag =
            (SelectTag) pageContext.getAttribute(Constants.SELECT_KEY);

        if (selectTag == null) {
            JspException e =
                new JspException(messages.getMessage("optionTag.select"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        return selectTag;
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        bundle = Globals.MESSAGES_KEY;
        dir = null;
        disabled = false;
        key = null;
        lang = null;
        locale = Globals.LOCALE_KEY;
        style = null;
        styleClass = null;
        text = null;
        value = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Return the text to be displayed to the user for this option (if any).
     *
     * @throws JspException if an error occurs
     */
    protected String text() throws JspException {
        String optionText = this.text;

        if ((optionText == null) && (this.key != null)) {
            optionText =
                TagUtils.getInstance().message(pageContext, bundle, locale, key);
        }

        // no body text and no key to lookup so display the value
        if (optionText == null) {
            optionText = this.value;
        }

        return optionText;
    }
}

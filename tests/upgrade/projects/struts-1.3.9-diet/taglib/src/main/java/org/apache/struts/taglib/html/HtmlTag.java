/*
 * $Id: HtmlTag.java 482895 2006-12-06 05:12:27Z niallp $
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
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import java.util.Locale;

/**
 * Renders an HTML <html> element with appropriate language attributes if
 * there is a current Locale available in the user's session.
 *
 * @version $Rev: 482895 $ $Date: 2005-08-21 19:08:45 -0400 (Sun, 21 Aug 2005)
 *          $
 */
public class HtmlTag extends TagSupport {
    // ------------------------------------------------------------- Properties

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    /**
     * Are we rendering an xhtml page?
     */
    protected boolean xhtml = false;

    /**
     * Are we rendering a lang attribute?
     *
     * @since Struts 1.2
     */
    protected boolean lang = false;

    public boolean getXhtml() {
        return this.xhtml;
    }

    public void setXhtml(boolean xhtml) {
        this.xhtml = xhtml;
    }

    /**
     * Returns true if the tag should render a lang attribute.
     *
     * @since Struts 1.2
     */
    public boolean getLang() {
        return this.lang;
    }

    /**
     * Sets whether the tag should render a lang attribute.
     *
     * @since Struts 1.2
     */
    public void setLang(boolean lang) {
        this.lang = lang;
    }

    /**
     * Process the start of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        TagUtils.getInstance().write(this.pageContext,
            this.renderHtmlStartElement());

        return EVAL_BODY_INCLUDE;
    }

    /**
     * Renders an &lt;html&gt; element with appropriate language attributes.
     *
     * @since Struts 1.2
     */
    protected String renderHtmlStartElement() {
        StringBuffer sb = new StringBuffer("<html");

        String language = null;
        String country = "";

        Locale currentLocale =
            TagUtils.getInstance().getUserLocale(pageContext, Globals.LOCALE_KEY);

        language = currentLocale.getLanguage();
        country = currentLocale.getCountry();

        boolean validLanguage = isValidRfc2616(language);
        boolean validCountry  = isValidRfc2616(country);

        if (this.xhtml) {
            this.pageContext.setAttribute(Globals.XHTML_KEY, "true",
                PageContext.PAGE_SCOPE);

            sb.append(" xmlns=\"http://www.w3.org/1999/xhtml\"");
        }

        if ((this.lang || this.xhtml) && validLanguage) {
            sb.append(" lang=\"");
            sb.append(language);

            if (validCountry) {
                sb.append("-");
                sb.append(country);
            }

            sb.append("\"");
        }

        if (this.xhtml && validLanguage) {
            sb.append(" xml:lang=\"");
            sb.append(language);

            if (validCountry) {
                sb.append("-");
                sb.append(country);
            }

            sb.append("\"");
        }

        sb.append(">");

        return sb.toString();
    }

    /**
     * Process the end of this tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        TagUtils.getInstance().write(pageContext, "</html>");

        // Evaluate the remainder of this page
        return (EVAL_PAGE);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        this.xhtml = false;
        this.lang = false;
    }

    /**
     * Check whether the value contains valid characters for the
     * "Accept-Language" header according to RFC 2616 (section 14.4).
     *
     * @param value The value to check
     * @return <code>true</code> if valid, otherwise <code>false</code>
     */
    private boolean isValidRfc2616(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            
            if (!(Character.isLetter(c) || c == '-')) {
                return false;
            }
        }
        return true;
    }
}

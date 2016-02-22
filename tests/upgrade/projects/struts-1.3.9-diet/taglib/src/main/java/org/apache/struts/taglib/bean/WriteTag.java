/*
 * $Id: WriteTag.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.bean;

import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import java.util.Locale;

/**
 * Tag that retrieves the specified property of the specified bean, converts
 * it to a String representation (if necessary), and writes it to the current
 * output stream, optionally filtering characters that are sensitive in HTML.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class WriteTag extends TagSupport {
    /**
     * The key to search default format string for java.sql.Timestamp in
     * resources.
     */
    public static final String SQL_TIMESTAMP_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.sql.timestamp";

    /**
     * The key to search default format string for java.sql.Date in
     * resources.
     */
    public static final String SQL_DATE_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.sql.date";

    /**
     * The key to search default format string for java.sql.Time in
     * resources.
     */
    public static final String SQL_TIME_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.sql.time";

    /**
     * The key to search default format string for java.util.Date in
     * resources.
     */
    public static final String DATE_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.date";

    /**
     * The key to search default format string for int (byte, short, etc.) in
     * resources.
     */
    public static final String INT_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.int";

    /**
     * The key to search default format string for float (double, BigDecimal)
     * in resources.
     */
    public static final String FLOAT_FORMAT_KEY =
        "org.apache.struts.taglib.bean.format.float";

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.taglib.bean.LocalStrings");

    // ------------------------------------------------------------- Properties

    /**
     * Filter the rendered output for characters that are sensitive in HTML?
     */
    protected boolean filter = true;

    /**
     * Should we ignore missing beans and simply output nothing?
     */
    protected boolean ignore = false;

    /**
     * Name of the bean that contains the data we will be rendering.
     */
    protected String name = null;

    /**
     * Name of the property to be accessed on the specified bean.
     */
    protected String property = null;

    /**
     * The scope to be searched to retrieve the specified bean.
     */
    protected String scope = null;

    /**
     * The format string to be used as format to convert value to String.
     */
    protected String formatStr = null;

    /**
     * The key to search format string in applciation resources
     */
    protected String formatKey = null;

    /**
     * The session scope key under which our Locale is stored.
     */
    protected String localeKey = null;

    /**
     * The servlet context attribute key for our resources.
     */
    protected String bundle = null;

    public boolean getFilter() {
        return (this.filter);
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public boolean getIgnore() {
        return (this.ignore);
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getFormat() {
        return (this.formatStr);
    }

    public void setFormat(String formatStr) {
        this.formatStr = formatStr;
    }

    public String getFormatKey() {
        return (this.formatKey);
    }

    public void setFormatKey(String formatKey) {
        this.formatKey = formatKey;
    }

    public String getLocale() {
        return (this.localeKey);
    }

    public void setLocale(String localeKey) {
        this.localeKey = localeKey;
    }

    public String getBundle() {
        return (this.bundle);
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Process the start tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Look up the requested bean (if necessary)
        if (ignore) {
            if (TagUtils.getInstance().lookup(pageContext, name, scope)
                == null) {
                return (SKIP_BODY); // Nothing to output
            }
        }

        // Look up the requested property value
        Object value =
            TagUtils.getInstance().lookup(pageContext, name, property, scope);

        if (value == null) {
            return (SKIP_BODY); // Nothing to output
        }

        // Convert value to the String with some formatting
        String output = formatValue(value);

        // Print this property value to our output writer, suitably filtered
        if (filter) {
            TagUtils.getInstance().write(pageContext,
                TagUtils.getInstance().filter(output));
        } else {
            TagUtils.getInstance().write(pageContext, output);
        }

        // Continue processing this page
        return (SKIP_BODY);
    }

    /**
     * Retrieve format string from message bundle and return null if message
     * not found or message string.
     *
     * @param formatKey value to use as key to search message in bundle
     * @throws JspException if a JSP exception has occurred
     */
    protected String retrieveFormatString(String formatKey)
        throws JspException {
        String result =
            TagUtils.getInstance().message(pageContext, this.bundle,
                this.localeKey, formatKey);

        if ((result != null)
            && !(result.startsWith("???") && result.endsWith("???"))) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Format value according to specified format string (as tag attribute or
     * as string from message resources) or to current user locale.
     *
     * When a format string is retrieved from the message resources,
     * <code>applyLocalizedPattern</code> is used. For more about localized
     * patterns, see
     * <http://www.dei.unipd.it/corsi/fi2ae-docs/source/jdk1.1.7/src/java/text/resources/>.
     * (To obtain the correct value for some characters, you may need to view
     * the file in a hex editor and then use the Unicode escape form in the
     * property resources file.)
     *
     * @param valueToFormat value to process and convert to String
     * @throws JspException if a JSP exception has occurred
     */
    protected String formatValue(Object valueToFormat)
        throws JspException {
        Format format = null;
        Object value = valueToFormat;
        Locale locale =
            TagUtils.getInstance().getUserLocale(pageContext, this.localeKey);
        boolean formatStrFromResources = false;
        String formatString = formatStr;

        // Return String object as is.
        if (value instanceof java.lang.String) {
            return (String) value;
        } else {
            // Try to retrieve format string from resources by the key from
            // formatKey.
            if ((formatString == null) && (formatKey != null)) {
                formatString = retrieveFormatString(this.formatKey);

                if (formatString != null) {
                    formatStrFromResources = true;
                }
            }

            // Prepare format object for numeric values.
            if (value instanceof Number) {
                if (formatString == null) {
                    if ((value instanceof Byte) || (value instanceof Short)
                        || (value instanceof Integer)
                        || (value instanceof Long)
                        || (value instanceof BigInteger)) {
                        formatString = retrieveFormatString(INT_FORMAT_KEY);
                    } else if ((value instanceof Float)
                        || (value instanceof Double)
                        || (value instanceof BigDecimal)) {
                        formatString = retrieveFormatString(FLOAT_FORMAT_KEY);
                    }

                    if (formatString != null) {
                        formatStrFromResources = true;
                    }
                }

                if (formatString != null) {
                    try {
                        format = NumberFormat.getNumberInstance(locale);

                        if (formatStrFromResources) {
                            ((DecimalFormat) format).applyLocalizedPattern(
                                formatString);
                        } else {
                            ((DecimalFormat) format).applyPattern(formatString);
                        }
                    } catch (IllegalArgumentException e) {
                        JspException ex =
                            new JspException(messages.getMessage(
                                    "write.format", formatString));

                        TagUtils.getInstance().saveException(pageContext, ex);
                        throw ex;
                    }
                }
            } else if (value instanceof java.util.Date) {
                if (formatString == null) {
                    if (value instanceof java.sql.Timestamp) {
                        formatString =
                            retrieveFormatString(SQL_TIMESTAMP_FORMAT_KEY);
                    } else if (value instanceof java.sql.Date) {
                        formatString =
                            retrieveFormatString(SQL_DATE_FORMAT_KEY);
                    } else if (value instanceof java.sql.Time) {
                        formatString =
                            retrieveFormatString(SQL_TIME_FORMAT_KEY);
                    } else if (value instanceof java.util.Date) {
                        formatString = retrieveFormatString(DATE_FORMAT_KEY);
                    }
                }

                if (formatString != null) {
                    format = new SimpleDateFormat(formatString, locale);
                }
            }
        }

        if (format != null) {
            return format.format(value);
        } else {
            return value.toString();
        }
    }

    /**
     * Release all allocated resources.
     */
    public void release() {
        super.release();
        filter = true;
        ignore = false;
        name = null;
        property = null;
        scope = null;
        formatStr = null;
        formatKey = null;
        localeKey = null;
        bundle = null;
    }
}

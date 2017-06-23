/*
 * $Id: MessageResources.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

import java.text.MessageFormat;

import java.util.HashMap;
import java.util.Locale;

/**
 * General purpose abstract class that describes an API for retrieving
 * Locale-sensitive messages from underlying resource locations of an
 * unspecified design, and optionally utilizing the <code>MessageFormat</code>
 * class to produce internationalized messages with parametric replacement.
 * <p> Calls to <code>getMessage()</code> variants without a
 * <code>Locale</code> argument are presumed to be requesting a message string
 * in the default <code>Locale</code> for this JVM. <p> Calls to
 * <code>getMessage()</code> with an unknown key, or an unknown
 * <code>Locale</code> will return <code>null</code> if the
 * <code>returnNull</code> property is set to <code>true</code>.  Otherwise, a
 * suitable error message will be returned instead. <p> <strong>IMPLEMENTATION
 * NOTE</strong> - Classes that extend this class must be Serializable so that
 * instances may be used in distributable application server environments.
 *
 * @version $Rev: 471754 $ $Date: 2005-08-29 23:57:50 -0400 (Mon, 29 Aug 2005)
 *          $
 */
public abstract class MessageResources implements Serializable {
    // ------------------------------------------------------------- Properties

    /**
     * Commons Logging instance.
     */
    protected static Log log = LogFactory.getLog(MessageResources.class);

    // --------------------------------------------------------- Static Methods

    /**
     * The default MessageResourcesFactory used to create MessageResources
     * instances.
     */
    protected static MessageResourcesFactory defaultFactory = null;

    /**
     * The configuration parameter used to initialize this MessageResources.
     */
    protected String config = null;

    /**
     * The default Locale for our environment.
     */
    protected Locale defaultLocale = Locale.getDefault();

    /**
     * The <code>MessageResourcesFactory</code> that created this instance.
     */
    protected MessageResourcesFactory factory = null;

    /**
     * The set of previously created MessageFormat objects, keyed by the key
     * computed in <code>messageKey()</code>.
     */
    protected HashMap formats = new HashMap();

    /**
     * Indicate is a <code>null</code> is returned instead of an error message
     * string when an unknown Locale or key is requested.
     */
    protected boolean returnNull = false;

    /**
     * Indicates whether 'escape processing' should be performed on the error
     * message string.
     */
    private boolean escape = true;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new MessageResources according to the specified
     * parameters.
     *
     * @param factory The MessageResourcesFactory that created us
     * @param config  The configuration parameter for this MessageResources
     */
    public MessageResources(MessageResourcesFactory factory, String config) {
        this(factory, config, false);
    }

    /**
     * Construct a new MessageResources according to the specified
     * parameters.
     *
     * @param factory    The MessageResourcesFactory that created us
     * @param config     The configuration parameter for this
     *                   MessageResources
     * @param returnNull The returnNull property we should initialize with
     */
    public MessageResources(MessageResourcesFactory factory, String config,
        boolean returnNull) {
        super();
        this.factory = factory;
        this.config = config;
        this.returnNull = returnNull;
    }

    /**
     * The configuration parameter used to initialize this MessageResources.
     *
     * @return parameter used to initialize this MessageResources
     */
    public String getConfig() {
        return (this.config);
    }

    /**
     * The <code>MessageResourcesFactory</code> that created this instance.
     *
     * @return <code>MessageResourcesFactory</code> that created instance
     */
    public MessageResourcesFactory getFactory() {
        return (this.factory);
    }

    /**
     * Indicates that a <code>null</code> is returned instead of an error
     * message string if an unknown Locale or key is requested.
     *
     * @return true if null is returned if unknown key or locale is requested
     */
    public boolean getReturnNull() {
        return (this.returnNull);
    }

    /**
     * Indicates that a <code>null</code> is returned instead of an error
     * message string if an unknown Locale or key is requested.
     *
     * @param returnNull true Indicates that a <code>null</code> is returned
     *                   if an unknown Locale or key is requested.
     */
    public void setReturnNull(boolean returnNull) {
        this.returnNull = returnNull;
    }

    /**
     * Indicates whether 'escape processing' should be performed on the error
     * message string.
     *
     * @since Struts 1.2.8
     */
    public boolean isEscape() {
        return escape;
    }

    /**
     * Set whether 'escape processing' should be performed on the error
     * message string.
     *
     * @since Struts 1.2.8
     */
    public void setEscape(boolean escape) {
        this.escape = escape;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Returns a text message for the specified key, for the default Locale.
     *
     * @param key The message key to look up
     */
    public String getMessage(String key) {
        return this.getMessage((Locale) null, key, null);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.
     *
     * @param key  The message key to look up
     * @param args An array of replacement parameters for placeholders
     */
    public String getMessage(String key, Object[] args) {
        return this.getMessage((Locale) null, key, args);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.
     *
     * @param key  The message key to look up
     * @param arg0 The replacement for placeholder {0} in the message
     */
    public String getMessage(String key, Object arg0) {
        return this.getMessage((Locale) null, key, arg0);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.
     *
     * @param key  The message key to look up
     * @param arg0 The replacement for placeholder {0} in the message
     * @param arg1 The replacement for placeholder {1} in the message
     */
    public String getMessage(String key, Object arg0, Object arg1) {
        return this.getMessage((Locale) null, key, arg0, arg1);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.
     *
     * @param key  The message key to look up
     * @param arg0 The replacement for placeholder {0} in the message
     * @param arg1 The replacement for placeholder {1} in the message
     * @param arg2 The replacement for placeholder {2} in the message
     */
    public String getMessage(String key, Object arg0, Object arg1, Object arg2) {
        return this.getMessage((Locale) null, key, arg0, arg1, arg2);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.
     *
     * @param key  The message key to look up
     * @param arg0 The replacement for placeholder {0} in the message
     * @param arg1 The replacement for placeholder {1} in the message
     * @param arg2 The replacement for placeholder {2} in the message
     * @param arg3 The replacement for placeholder {3} in the message
     */
    public String getMessage(String key, Object arg0, Object arg1, Object arg2,
        Object arg3) {
        return this.getMessage((Locale) null, key, arg0, arg1, arg2, arg3);
    }

    /**
     * Returns a text message for the specified key, for the default Locale. A
     * null string result will be returned by this method if no relevant
     * message resource is found for this key or Locale, if the
     * <code>returnNull</code> property is set.  Otherwise, an appropriate
     * error message will be returned. <p> This method must be implemented by
     * a concrete subclass.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     */
    public abstract String getMessage(Locale locale, String key);

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.  A null string result will be returned by this
     * method if no resource bundle has been configured.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @param args   An array of replacement parameters for placeholders
     */
    public String getMessage(Locale locale, String key, Object[] args) {
        // Cache MessageFormat instances as they are accessed
        if (locale == null) {
            locale = defaultLocale;
        }

        MessageFormat format = null;
        String formatKey = messageKey(locale, key);

        synchronized (formats) {
            format = (MessageFormat) formats.get(formatKey);

            if (format == null) {
                String formatString = getMessage(locale, key);

                if (formatString == null) {
                    return returnNull ? null : ("???" + formatKey + "???");
                }

                format = new MessageFormat(escape(formatString));
                format.setLocale(locale);
                formats.put(formatKey, format);
            }
        }

        return format.format(args);
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.  A null string result will never be returned by
     * this method.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @param arg0   The replacement for placeholder {0} in the message
     */
    public String getMessage(Locale locale, String key, Object arg0) {
        return this.getMessage(locale, key, new Object[] { arg0 });
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.  A null string result will never be returned by
     * this method.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @param arg0   The replacement for placeholder {0} in the message
     * @param arg1   The replacement for placeholder {1} in the message
     */
    public String getMessage(Locale locale, String key, Object arg0, Object arg1) {
        return this.getMessage(locale, key, new Object[] { arg0, arg1 });
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.  A null string result will never be returned by
     * this method.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @param arg0   The replacement for placeholder {0} in the message
     * @param arg1   The replacement for placeholder {1} in the message
     * @param arg2   The replacement for placeholder {2} in the message
     */
    public String getMessage(Locale locale, String key, Object arg0,
        Object arg1, Object arg2) {
        return this.getMessage(locale, key, new Object[] { arg0, arg1, arg2 });
    }

    /**
     * Returns a text message after parametric replacement of the specified
     * parameter placeholders.  A null string result will never be returned by
     * this method.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @param arg0   The replacement for placeholder {0} in the message
     * @param arg1   The replacement for placeholder {1} in the message
     * @param arg2   The replacement for placeholder {2} in the message
     * @param arg3   The replacement for placeholder {3} in the message
     */
    public String getMessage(Locale locale, String key, Object arg0,
        Object arg1, Object arg2, Object arg3) {
        return this.getMessage(locale, key,
            new Object[] { arg0, arg1, arg2, arg3 });
    }

    /**
     * Return <code>true</code> if there is a defined message for the
     * specified key in the system default locale.
     *
     * @param key The message key to look up
     */
    public boolean isPresent(String key) {
        return this.isPresent(null, key);
    }

    /**
     * Return <code>true</code> if there is a defined message for the
     * specified key in the specified Locale.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     */
    public boolean isPresent(Locale locale, String key) {
        String message = getMessage(locale, key);

        if (message == null) {
            return false;
        } else if (message.startsWith("???") && message.endsWith("???")) {
            return false; // FIXME - Only valid for default implementation
        } else {
            return true;
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Escape any single quote characters that are included in the specified
     * message string.
     *
     * @param string The string to be escaped
     */
    protected String escape(String string) {
        if (!isEscape()) {
            return string;
        }

        if ((string == null) || (string.indexOf('\'') < 0)) {
            return string;
        }

        int n = string.length();
        StringBuffer sb = new StringBuffer(n);

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (ch == '\'') {
                sb.append('\'');
            }

            sb.append(ch);
        }

        return sb.toString();
    }

    /**
     * Compute and return a key to be used in caching information by a Locale.
     * <strong>NOTE</strong> - The locale key for the default Locale in our
     * environment is a zero length String.
     *
     * @param locale The locale for which a key is desired
     */
    protected String localeKey(Locale locale) {
        return (locale == null) ? "" : locale.toString();
    }

    /**
     * Compute and return a key to be used in caching information by Locale
     * and message key.
     *
     * @param locale The Locale for which this format key is calculated
     * @param key    The message key for which this format key is calculated
     */
    protected String messageKey(Locale locale, String key) {
        return (localeKey(locale) + "." + key);
    }

    /**
     * Compute and return a key to be used in caching information by locale
     * key and message key.
     *
     * @param localeKey The locale key for which this cache key is calculated
     * @param key       The message key for which this cache key is
     *                  calculated
     */
    protected String messageKey(String localeKey, String key) {
        return (localeKey + "." + key);
    }

    /**
     * Create and return an instance of <code>MessageResources</code> for the
     * created by the default <code>MessageResourcesFactory</code>.
     *
     * @param config Configuration parameter for this message bundle.
     */
    public synchronized static MessageResources getMessageResources(
        String config) {
        if (defaultFactory == null) {
            defaultFactory = MessageResourcesFactory.createFactory();
        }

        return defaultFactory.createResources(config);
    }

    /**
     * Log a message to the Writer that has been configured for our use.
     *
     * @param message The message to be logged
     */
    public void log(String message) {
        log.debug(message);
    }

    /**
     * Log a message and exception to the Writer that has been configured for
     * our use.
     *
     * @param message   The message to be logged
     * @param throwable The exception to be logged
     */
    public void log(String message, Throwable throwable) {
        log.debug(message, throwable);
    }
}

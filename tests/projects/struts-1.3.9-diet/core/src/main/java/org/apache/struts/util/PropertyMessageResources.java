/*
 * $Id: PropertyMessageResources.java 480549 2006-11-29 12:16:15Z niallp $
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

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

/**
 * Concrete subclass of <code>MessageResources</code> that reads message keys
 * and corresponding strings from named property resources in a <b><i>similar</i></b> manner
 * (see <i>modes</i> below) that <code>java.util.PropertyResourceBundle</code> does.  The
 * <code>base</code> property defines the base property resource name, and
 * must be specified. <p> <strong>IMPLEMENTATION NOTE</strong> - This class
 * trades memory for speed by caching all messages located via generalizing
 * the Locale under the original locale as well. This results in specific
 * messages being stored in the message cache more than once, but improves
 * response time on subsequent requests for the same locale + key
 * combination.
 *
 * <h2>Operating Modes</h2>
 * This implementation can be configured to operate in one of three modes:
 * <ul>
 *    <li>1. <b>Default</b> - default, backwardly compatible, Struts behaviour (i.e. the way
 *    its always worked).</li>
 *    <li>2. <b>JSTL</b> - compatible with how JSTL finds messages
 *        (fix for <a href="http://issues.apache.org/struts/browse/STR-2925">STR-2925</a>)</li>
 *    <li>3. <b>Resource</b> - compatible with how Java's <code>PropertyResourceBundle</code>
 *        finds messages (fix for
 *        <a href="http://issues.apache.org/struts/browse/STR-2077">STR-2077</a>)</li>
 * </ul>
 *
 * <h3>1. Default Mode</h3>
 * <i>Default mode</i> is the way this implementation has always operated. It searches
 * for a message key for property resources in the following sequence:
 * <pre>
 *      base + "_" + localeLanguage + "_" + localeCountry + "_" + localeVariant
 *      base + "_" + localeLanguage + "_" + localeCountry
 *      base + "_" + localeLanguage
 *      base + "_" + default locale
 *      base
 * </pre>
 * <p>
 * This mode is the <i>default</i> and requires no additional configuration.
 *
 * <h3>2. JSTL Mode</h3>
 * <i>JSTL mode</i> is compatible with how JSTL operates and the default Locale
 * is not used when looking for a message key. <i>JSTL mode</i> searches for
 * a message key for property resources in the following sequence:
 * <pre>
 *      base + "_" + localeLanguage + "_" + localeCountry + "_" + localeVariant
 *      base + "_" + localeLanguage + "_" + localeCountry
 *      base + "_" + localeLanguage
 *      base
 * </pre>
 * <p>
 * Configure <code>PropertyMessageResources</code> to operate in this mode by
 * specifying a value of <code>JSTL</code> for the <code>mode</code>
 * key in your <code>struts-config.xml</code>:
 * <pre>
 *      &lt;message-resources parameter="mypackage.MyMessageResources"&gt;
 *          &lt;set-property key="mode" value="JSTL"/&gt;
 *      &lt;/message-resources&gt;
 * </pre>
 *
 * <h3>3. Resource Mode</h3>
 * <i>Resource mode</i> is compatible with how Java's <code>PropertyResourceBundle</code>
 * operates. <i>Resource mode</i> searches first through the specified Locale's language,
 * country and variant, then through the default Locale's language,
 * country and variant and finally using just the <code>base</code>:
 * <pre>
 *      base + "_" + localeLanguage + "_" + localeCountry + "_" + localeVariant
 *      base + "_" + localeLanguage + "_" + localeCountry
 *      base + "_" + localeLanguage
 *      base + "_" + defaultLanguage + "_" + defaultCountry + "_" + defaultVariant
 *      base + "_" + defaultLanguage + "_" + defaultCountry
 *      base + "_" + defaultLanguage
 *      base
 * </pre>
 * <p>
 * Configure <code>PropertyMessageResources</code> to operate in this mode by
 * specifying a value of <code>resource</code> for the <code>mode</code>
 * key in your <code>struts-config.xml</code>:
 * <pre>
 *      &lt;message-resources parameter="mypackage.MyMessageResources"&gt;
 *          &lt;set-property key="mode" value="resource"/&gt;
 *      &lt;/message-resources&gt;
 * </pre>
 * 
 * @version $Rev: 480549 $ $Date: 2006-11-29 13:16:15 +0100 (Wed, 29 Nov 2006) $
 */
public class PropertyMessageResources extends MessageResources {


    /** Indicates compatibility with how PropertyMessageResources has always looked up messages */
    private static final int MODE_DEFAULT = 0;

    /** Indicates compatibility with how JSTL looks up messages */
    private static final int MODE_JSTL = 1;

    /** Indicates compatibility with how java's PropertyResourceBundle looks up messages */
    private static final int MODE_RESOURCE_BUNDLE = 2;

    /**
     * The <code>Log</code> instance for this class.
     */
    protected static final Log log =
        LogFactory.getLog(PropertyMessageResources.class);

    // ------------------------------------------------------------- Properties

    /**
     * The set of locale keys for which we have already loaded messages, keyed
     * by the value calculated in <code>localeKey()</code>.
     */
    protected HashMap locales = new HashMap();

    /**
     * The cache of messages we have accumulated over time, keyed by the value
     * calculated in <code>messageKey()</code>.
     */
    protected HashMap messages = new HashMap();

    /**
     * Compatibility mode that PropertyMessageResources is operating in.
     */
    private int mode = MODE_DEFAULT;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new PropertyMessageResources according to the specified
     * parameters.
     *
     * @param factory The MessageResourcesFactory that created us
     * @param config  The configuration parameter for this MessageResources
     */
    public PropertyMessageResources(MessageResourcesFactory factory,
        String config) {
        super(factory, config);
        log.trace("Initializing, config='" + config + "'");
    }

    /**
     * Construct a new PropertyMessageResources according to the specified
     * parameters.
     *
     * @param factory    The MessageResourcesFactory that created us
     * @param config     The configuration parameter for this
     *                   MessageResources
     * @param returnNull The returnNull property we should initialize with
     */
    public PropertyMessageResources(MessageResourcesFactory factory,
        String config, boolean returnNull) {
        super(factory, config, returnNull);
        log.trace("Initializing, config='" + config + "', returnNull="
            + returnNull);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set the compatibility mode this implementation uses for message lookup.
     *
     * @param mode <code>JSTL</code> for JSTL compatibility,
     *  <code>resource</code> for PropertyResourceBundle compatibility or
     *  <code>default</code> for Struts backward compatibility.
     */
    public void setMode(String mode) {
        String value = (mode == null ? null : mode.trim());
        if ("jstl".equalsIgnoreCase(value)) {
            this.mode = MODE_JSTL;
            if (log.isDebugEnabled()) {
                log.info("Operating in JSTL compatible mode [" + mode + "]");
            }
        } else if ("resource".equalsIgnoreCase(value)) {
            this.mode = MODE_RESOURCE_BUNDLE;
            if (log.isDebugEnabled()) {
                log.info("Operating in PropertyResourceBundle compatible mode [" + mode + "]");
            }
        } else {
            this.mode = MODE_DEFAULT;
            if (log.isDebugEnabled()) {
                log.info("Operating in Default mode [" + mode + "]");
            }
        }
    }

    /**
     * Returns a text message for the specified key, for the specified or default
     * Locale. A null string result will be returned by this method if no relevant
     * message resource is found for this key or Locale, if the
     * <code>returnNull</code> property is set.  Otherwise, an appropriate
     * error message will be returned. <p> This method must be implemented by
     * a concrete subclass.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *               the system default Locale
     * @param key    The message key to look up
     * @return text message for the specified key and locale
     */
    public String getMessage(Locale locale, String key) {
        if (log.isDebugEnabled()) {
            log.debug("getMessage(" + locale + "," + key + ")");
        }

        // Initialize variables we will require
        String localeKey = localeKey(locale);
        String originalKey = messageKey(localeKey, key);
        String message = null;

        // Search the specified Locale
        message = findMessage(locale, key, originalKey);
        if (message != null) {
            return message;
        }

        // JSTL Compatibility - JSTL doesn't use the default locale
        if (mode == MODE_JSTL) {

           // do nothing (i.e. don't use default Locale)

        // PropertyResourcesBundle - searches through the hierarchy
        // for the default Locale (e.g. first en_US then en)
        } else if (mode == MODE_RESOURCE_BUNDLE) {

            if (!defaultLocale.equals(locale)) {
                message = findMessage(defaultLocale, key, originalKey);
            }

        // Default (backwards) Compatibility - just searches the
        // specified Locale (e.g. just en_US)
        } else {

            if (!defaultLocale.equals(locale)) {
                localeKey = localeKey(defaultLocale);
                message = findMessage(localeKey, key, originalKey);
            }

        }
        if (message != null) {
            return message;
        }

        // Find the message in the default properties file
        message = findMessage("", key, originalKey);
        if (message != null) {
            return message;
        }

        // Return an appropriate error indication
        if (returnNull) {
            return (null);
        } else {
            return ("???" + messageKey(locale, key) + "???");
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Load the messages associated with the specified Locale key.  For this
     * implementation, the <code>config</code> property should contain a fully
     * qualified package and resource name, separated by periods, of a series
     * of property resources to be loaded from the class loader that created
     * this PropertyMessageResources instance.  This is exactly the same name
     * format you would use when utilizing the <code>java.util.PropertyResourceBundle</code>
     * class.
     *
     * @param localeKey Locale key for the messages to be retrieved
     */
    protected synchronized void loadLocale(String localeKey) {
        if (log.isTraceEnabled()) {
            log.trace("loadLocale(" + localeKey + ")");
        }

        // Have we already attempted to load messages for this locale?
        if (locales.get(localeKey) != null) {
            return;
        }

        locales.put(localeKey, localeKey);

        // Set up to load the property resource for this locale key, if we can
        String name = config.replace('.', '/');

        if (localeKey.length() > 0) {
            name += ("_" + localeKey);
        }

        name += ".properties";

        InputStream is = null;
        Properties props = new Properties();

        // Load the specified property resource
        if (log.isTraceEnabled()) {
            log.trace("  Loading resource '" + name + "'");
        }

        ClassLoader classLoader =
            Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        is = classLoader.getResourceAsStream(name);

        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
                log.error("loadLocale()", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("loadLocale()", e);
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("  Loading resource completed");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("  Resource "+name+" Not Found.");
            }
        }


        // Copy the corresponding values into our cache
        if (props.size() < 1) {
            return;
        }

        synchronized (messages) {
            Iterator names = props.keySet().iterator();

            while (names.hasNext()) {
                String key = (String) names.next();

                if (log.isTraceEnabled()) {
                    log.trace("  Saving message key '"
                        + messageKey(localeKey, key));
                }

                messages.put(messageKey(localeKey, key), props.getProperty(key));
            }
        }
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Returns a text message for the specified key, for the specified Locale.
     * <p>
     * A null string result will be returned by this method if no relevant
     * message resource is found. This method searches through the locale
     * <i>hierarchy</i> (i.e. variant --> languge --> country) for the message.
     *
     * @param locale The requested message Locale, or <code>null</code> for
     *  the system default Locale
     * @param key The message key to look up
     * @param originalKey The original message key to cache any found message under
     * @return text message for the specified key and locale
     */
    private String findMessage(Locale locale, String key, String originalKey) {

        // Initialize variables we will require
        String localeKey = localeKey(locale);
        String messageKey = null;
        String message = null;
        int underscore = 0;

        // Loop from specific to general Locales looking for this message
        while (true) {
            message = findMessage(localeKey, key, originalKey);
            if (message != null) {
                break;
            }

            // Strip trailing modifiers to try a more general locale key
            underscore = localeKey.lastIndexOf("_");

            if (underscore < 0) {
                break;
            }

            localeKey = localeKey.substring(0, underscore);
        }

        return message;

    }

    /**
     * Returns a text message for the specified key, for the specified Locale.
     * <p>
     * A null string result will be returned by this method if no relevant
     * message resource is found.
     *
     * @param locale The requested key of the Locale
     * @param key The message key to look up
     * @param originalKey The original message key to cache any found message under
     * @return text message for the specified key and locale
     */
    private String findMessage(String localeKey, String key, String originalKey) {

        // Load this Locale's messages if we have not done so yet
        loadLocale(localeKey);

        // Check if we have this key for the current locale key
        String messageKey = messageKey(localeKey, key);

        // Add if not found under the original key
        boolean addIt = !messageKey.equals(originalKey);

        synchronized (messages) {
            String message = (String) messages.get(messageKey);

            if (message != null) {
                if (addIt) {
                    messages.put(originalKey, message);
                }

            }
            return (message);
        }
    }
}

/*
 * $Id: ActionServlet.java 483039 2006-12-06 11:34:28Z niallp $
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
package org.apache.struts.action;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BigIntegerConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.ByteConverter;
import org.apache.commons.beanutils.converters.CharacterConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.beanutils.converters.ShortConverter;
import org.apache.commons.chain.CatalogFactory;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ConfigRuleSet;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.FormPropertyConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.MessageResourcesConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;
import org.apache.struts.config.PlugInConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.MessageResourcesFactory;
import org.apache.struts.util.ModuleUtils;
import org.apache.struts.util.RequestUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

/**
 * <p><strong>ActionServlet</strong> provides the "controller" in the
 * Model-View-Controller (MVC) design pattern for web applications that is
 * commonly known as "Model 2".  This nomenclature originated with a
 * description in the JavaServerPages Specification, version 0.92, and has
 * persisted ever since (in the absence of a better name).</p>
 *
 * <p>Generally, a "Model 2" application is architected as follows:</p>
 *
 * <ul>
 *
 * <li>The user interface will generally be created with server pages, which
 * will not themselves contain any business logic. These pages represent the
 * "view" component of an MVC architecture.</li>
 *
 * <li>Forms and hyperlinks in the user interface that require business logic
 * to be executed will be submitted to a request URI that is mapped to this
 * servlet.</li>
 *
 * <li>There can be <b>one</b> instance of this servlet class, which receives
 * and processes all requests that change the state of a user's interaction
 * with the application. The servlet delegates the handling of a request to a
 * {@link RequestProcessor} object. This component represents the "controller"
 * component of an MVC architecture. </li>
 *
 * <li>The <code>RequestProcessor</code> selects and invokes an {@link Action}
 * class to perform the requested business logic, or delegates the response to
 * another resource.</li>
 *
 * <li>The <code>Action</code> classes can manipulate the state of the
 * application's interaction with the user, typically by creating or modifying
 * JavaBeans that are stored as request or session attributes (depending on
 * how long they need to be available). Such JavaBeans represent the "model"
 * component of an MVC architecture.</li>
 *
 * <li>Instead of producing the next page of the user interface directly,
 * <code>Action</code> classes generally return an {@link ActionForward} to
 * indicate which resource should handle the response. If the
 * <code>Action</code> does not return null, the <code>RequestProcessor</code>
 * forwards or redirects to the specified resource (by utilizing
 * <code>RequestDispatcher.forward</code> or <code>Response.sendRedirect</code>)
 * so as to produce the next page of the user interface.</li>
 *
 * </ul>
 *
 * <p>The standard version of <code>RequestsProcessor</code> implements the
 * following logic for each incoming HTTP request. You can override some or
 * all of this functionality by subclassing this object and implementing your
 * own version of the processing.</p>
 *
 * <ul>
 *
 * <li>Identify, from the incoming request URI, the substring that will be
 * used to select an action procedure.</li>
 *
 * <li>Use this substring to map to the Java class name of the corresponding
 * action class (an implementation of the <code>Action</code> interface).
 * </li>
 *
 * <li>If this is the first request for a particular <code>Action</code>
 * class, instantiate an instance of that class and cache it for future
 * use.</li>
 *
 * <li>Optionally populate the properties of an <code>ActionForm</code> bean
 * associated with this mapping.</li>
 *
 * <li>Call the <code>execute</code> method of this <code>Action</code> class,
 * passing on a reference to the mapping that was used, the relevant form-bean
 * (if any), and the request and the response that were passed to the
 * controller by the servlet container (thereby providing access to any
 * specialized properties of the mapping itself as well as to the
 * ServletContext). </li>
 *
 * </ul>
 *
 * <p>The standard version of <code>ActionServlet</code> is configured based
 * on the following servlet initialization parameters, which you will specify
 * in the web application deployment descriptor (<code>/WEB-INF/web.xml</code>)
 * for your application.  Subclasses that specialize this servlet are free to
 * define additional initialization parameters. </p>
 *
 * <ul>
 *
 * <li><strong>config</strong> - Comma-separated list of context-relative
 * path(s) to the XML resource(s) containing the configuration information for
 * the default module.  (Multiple files support since Struts 1.1)
 * [/WEB-INF/struts-config.xml].</li>
 *
 * <li><strong>config/${module}</strong> - Comma-separated list of
 * Context-relative path(s) to the XML resource(s) containing the
 * configuration information for the module that will use the specified prefix
 * (/${module}). This can be repeated as many times as required for multiple
 * modules. (Since Struts 1.1)</li>
 *
 * <li><strong>configFactory</strong> - The Java class name of the
 * <code>ModuleConfigFactory</code> used to create the implementation of the
 * ModuleConfig interface. </li>
 *
 * <li><strong>convertNull</strong> - Force simulation of the Struts 1.0
 * behavior when populating forms. If set to true, the numeric Java wrapper
 * class types (like <code>java.lang.Integer</code>) will default to null
 * (rather than 0). (Since Struts 1.1) [false] </li>
 *
 * <li><strong>rulesets </strong> - Comma-delimited list of fully qualified
 * classnames of additional <code>org.apache.commons.digester.RuleSet</code>
 * instances that should be added to the <code>Digester</code> that will be
 * processing <code>struts-config.xml</code> files.  By default, only the
 * <code>RuleSet</code> for the standard configuration elements is loaded.
 * (Since Struts 1.1)</li>
 *
 * <li><strong>validating</strong> - Should we use a validating XML parser to
 * process the configuration file (strongly recommended)? [true]</li>
 *
 * <li><strong>chainConfig</strong> - Comma-separated list of either
 * context-relative or classloader path(s) to load commons-chain catalog
 * definitions from.  If none specified, the default Struts catalog that is
 * provided with Struts will be used.</li>
 *
 * </ul>
 *
 * @version $Rev: 483039 $ $Date: 2005-10-14 19:54:16 -0400 (Fri, 14 Oct 2005)
 *          $
 */
public class ActionServlet extends HttpServlet {
    /**
     * <p>Commons Logging instance.</p>
     *
     * @since Struts 1.1
     */
    protected static Log log = LogFactory.getLog(ActionServlet.class);

    // ----------------------------------------------------- Instance Variables

    /**
     * <p>Comma-separated list of context-relative path(s) to our
     * configuration resource(s) for the default module.</p>
     */
    protected String config = "/WEB-INF/struts-config.xml";

    /**
     * <p>Comma-separated list of context or classloader-relative path(s) that
     * contain the configuration for the default commons-chain
     * catalog(s).</p>
     */
    protected String chainConfig = "org/apache/struts/chain/chain-config.xml";

    /**
     * <p>The Digester used to produce ModuleConfig objects from a Struts
     * configuration file.</p>
     *
     * @since Struts 1.1
     */
    protected Digester configDigester = null;

    /**
     * <p>The flag to request backwards-compatible conversions for form bean
     * properties of the Java wrapper class types.</p>
     *
     * @since Struts 1.1
     */
    protected boolean convertNull = false;

    /**
     * <p>The resources object for our internal resources.</p>
     */
    protected MessageResources internal = null;

    /**
     * <p>The Java base name of our internal resources.</p>
     *
     * @since Struts 1.1
     */
    protected String internalName = "org.apache.struts.action.ActionResources";

    /**
     * <p>The set of public identifiers, and corresponding resource names, for
     * the versions of the configuration file DTDs that we know about.  There
     * <strong>MUST</strong> be an even number of Strings in this list!</p>
     */
    protected String[] registrations =
        {
            "-//Apache Software Foundation//DTD Struts Configuration 1.0//EN",
            "/org/apache/struts/resources/struts-config_1_0.dtd",
            "-//Apache Software Foundation//DTD Struts Configuration 1.1//EN",
            "/org/apache/struts/resources/struts-config_1_1.dtd",
            "-//Apache Software Foundation//DTD Struts Configuration 1.2//EN",
            "/org/apache/struts/resources/struts-config_1_2.dtd",
            "-//Apache Software Foundation//DTD Struts Configuration 1.3//EN",
            "/org/apache/struts/resources/struts-config_1_3.dtd",
            "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN",
            "/org/apache/struts/resources/web-app_2_3.dtd"
        };

    /**
     * <p>The URL pattern to which we are mapped in our web application
     * deployment descriptor.</p>
     */
    protected String servletMapping = null; // :FIXME: - multiples?

    /**
     * <p>The servlet name under which we are registered in our web
     * application deployment descriptor.</p>
     */
    protected String servletName = null;

    // ---------------------------------------------------- HttpServlet Methods

    /**
     * <p>Gracefully shut down this controller servlet, releasing any
     * resources that were allocated at initialization.</p>
     */
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug(internal.getMessage("finalizing"));
        }

        destroyModules();
        destroyInternal();
        getServletContext().removeAttribute(Globals.ACTION_SERVLET_KEY);

        // Release our LogFactory and Log instances (if any)
        ClassLoader classLoader =
            Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = ActionServlet.class.getClassLoader();
        }

        try {
            LogFactory.release(classLoader);
        } catch (Throwable t) {
            ; // Servlet container doesn't have the latest version

            // of commons-logging-api.jar installed
            // :FIXME: Why is this dependent on the container's version of
            // commons-logging? Shouldn't this depend on the version packaged
            // with Struts?

            /*
              Reason: LogFactory.release(classLoader); was added as
              an attempt to investigate the OutOfMemory error reported on
              Bugzilla #14042. It was committed for version 1.136 by craigmcc
            */
        }

        CatalogFactory.clear();
        PropertyUtils.clearDescriptors();
    }

    /**
     * <p>Initialize this servlet.  Most of the processing has been factored
     * into support methods so that you can override particular functionality
     * at a fairly granular level.</p>
     *
     * @throws ServletException if we cannot configure ourselves correctly
     */
    public void init() throws ServletException {
        final String configPrefix = "config/";
        final int configPrefixLength = configPrefix.length() - 1;

        // Wraps the entire initialization in a try/catch to better handle
        // unexpected exceptions and errors to provide better feedback
        // to the developer
        try {
            initInternal();
            initOther();
            initServlet();
            initChain();

            getServletContext().setAttribute(Globals.ACTION_SERVLET_KEY, this);
            initModuleConfigFactory();

            // Initialize modules as needed
            ModuleConfig moduleConfig = initModuleConfig("", config);

            initModuleMessageResources(moduleConfig);
            initModulePlugIns(moduleConfig);
            initModuleFormBeans(moduleConfig);
            initModuleForwards(moduleConfig);
            initModuleExceptionConfigs(moduleConfig);
            initModuleActions(moduleConfig);
            moduleConfig.freeze();

            Enumeration names = getServletConfig().getInitParameterNames();

            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();

                if (!name.startsWith(configPrefix)) {
                    continue;
                }

                String prefix = name.substring(configPrefixLength);

                moduleConfig =
                    initModuleConfig(prefix,
                        getServletConfig().getInitParameter(name));
                initModuleMessageResources(moduleConfig);
                initModulePlugIns(moduleConfig);
                initModuleFormBeans(moduleConfig);
                initModuleForwards(moduleConfig);
                initModuleExceptionConfigs(moduleConfig);
                initModuleActions(moduleConfig);
                moduleConfig.freeze();
            }

            this.initModulePrefixes(this.getServletContext());

            this.destroyConfigDigester();
        } catch (UnavailableException ex) {
            throw ex;
        } catch (Throwable t) {
            // The follow error message is not retrieved from internal message
            // resources as they may not have been able to have been
            // initialized
            log.error("Unable to initialize Struts ActionServlet due to an "
                + "unexpected exception or error thrown, so marking the "
                + "servlet as unavailable.  Most likely, this is due to an "
                + "incorrect or missing library dependency.", t);
            throw new UnavailableException(t.getMessage());
        }
    }

    /**
     * <p>Saves a String[] of module prefixes in the ServletContext under
     * Globals.MODULE_PREFIXES_KEY.  <strong>NOTE</strong> - the "" prefix for
     * the default module is not included in this list.</p>
     *
     * @param context The servlet context.
     * @since Struts 1.2
     */
    protected void initModulePrefixes(ServletContext context) {
        ArrayList prefixList = new ArrayList();

        Enumeration names = context.getAttributeNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            if (!name.startsWith(Globals.MODULE_KEY)) {
                continue;
            }

            String prefix = name.substring(Globals.MODULE_KEY.length());

            if (prefix.length() > 0) {
                prefixList.add(prefix);
            }
        }

        String[] prefixes =
            (String[]) prefixList.toArray(new String[prefixList.size()]);

        context.setAttribute(Globals.MODULE_PREFIXES_KEY, prefixes);
    }

    /**
     * <p>Process an HTTP "GET" request.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        process(request, response);
    }

    /**
     * <p>Process an HTTP "POST" request.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        process(request, response);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>Remember a servlet mapping from our web application deployment
     * descriptor, if it is for this servlet.</p>
     *
     * @param servletName The name of the servlet being mapped
     * @param urlPattern  The URL pattern to which this servlet is mapped
     */
    public void addServletMapping(String servletName, String urlPattern) {
        if (servletName == null) {
            return;
        }

        if (servletName.equals(this.servletName)) {
            if (log.isDebugEnabled()) {
                log.debug("Process servletName=" + servletName
                    + ", urlPattern=" + urlPattern);
            }

            this.servletMapping = urlPattern;
        }
    }

    /**
     * <p>Return the <code>MessageResources</code> instance containing our
     * internal message strings.</p>
     *
     * @return the <code>MessageResources</code> instance containing our
     *         internal message strings.
     * @since Struts 1.1
     */
    public MessageResources getInternal() {
        return (this.internal);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>Gracefully terminate use of any modules associated with this
     * application (if any).</p>
     *
     * @since Struts 1.1
     */
    protected void destroyModules() {
        ArrayList values = new ArrayList();
        Enumeration names = getServletContext().getAttributeNames();

        while (names.hasMoreElements()) {
            values.add(names.nextElement());
        }

        Iterator keys = values.iterator();

        while (keys.hasNext()) {
            String name = (String) keys.next();
            Object value = getServletContext().getAttribute(name);

            if (!(value instanceof ModuleConfig)) {
                continue;
            }

            ModuleConfig config = (ModuleConfig) value;

            if (this.getProcessorForModule(config) != null) {
                this.getProcessorForModule(config).destroy();
            }

            getServletContext().removeAttribute(name);

            PlugIn[] plugIns =
                (PlugIn[]) getServletContext().getAttribute(Globals.PLUG_INS_KEY
                    + config.getPrefix());

            if (plugIns != null) {
                for (int i = 0; i < plugIns.length; i++) {
                    int j = plugIns.length - (i + 1);

                    plugIns[j].destroy();
                }

                getServletContext().removeAttribute(Globals.PLUG_INS_KEY
                    + config.getPrefix());
            }
        }
    }

    /**
     * <p>Gracefully release any configDigester instance that we have created.
     * </p>
     *
     * @since Struts 1.1
     */
    protected void destroyConfigDigester() {
        configDigester = null;
    }

    /**
     * <p>Gracefully terminate use of the internal MessageResources.</p>
     */
    protected void destroyInternal() {
        internal = null;
    }

    /**
     * <p>Return the module configuration object for the currently selected
     * module.</p>
     *
     * @param request The servlet request we are processing
     * @return The module configuration object for the currently selected
     *         module.
     * @since Struts 1.1
     */
    protected ModuleConfig getModuleConfig(HttpServletRequest request) {
        ModuleConfig config =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        if (config == null) {
            config =
                (ModuleConfig) getServletContext().getAttribute(Globals.MODULE_KEY);
        }

        return (config);
    }

    /**
     * <p>Look up and return the {@link RequestProcessor} responsible for the
     * specified module, creating a new one if necessary.</p>
     *
     * @param config The module configuration for which to acquire and return
     *               a RequestProcessor.
     * @return The {@link RequestProcessor} responsible for the specified
     *         module,
     * @throws ServletException If we cannot instantiate a RequestProcessor
     *                          instance a {@link UnavailableException} is
     *                          thrown, meaning your application is not loaded
     *                          and will not be available.
     * @since Struts 1.1
     */
    protected synchronized RequestProcessor getRequestProcessor(
        ModuleConfig config) throws ServletException {
        RequestProcessor processor = this.getProcessorForModule(config);

        if (processor == null) {
            try {
                processor =
                    (RequestProcessor) RequestUtils.applicationInstance(config.getControllerConfig()
                                                                              .getProcessorClass());
            } catch (Exception e) {
                throw new UnavailableException(
                    "Cannot initialize RequestProcessor of class "
                    + config.getControllerConfig().getProcessorClass() + ": "
                    + e);
            }

            processor.init(this, config);

            String key = Globals.REQUEST_PROCESSOR_KEY + config.getPrefix();

            getServletContext().setAttribute(key, processor);
        }

        return (processor);
    }

    /**
     * <p>Returns the RequestProcessor for the given module or null if one
     * does not exist.  This method will not create a RequestProcessor.</p>
     *
     * @param config The ModuleConfig.
     * @return The <code>RequestProcessor</code> for the given module, or
     *         <code>null</code> if one does not exist.
     */
    private RequestProcessor getProcessorForModule(ModuleConfig config) {
        String key = Globals.REQUEST_PROCESSOR_KEY + config.getPrefix();

        return (RequestProcessor) getServletContext().getAttribute(key);
    }

    /**
     * <p>Initialize the factory used to create the module configuration.</p>
     *
     * @since Struts 1.2
     */
    protected void initModuleConfigFactory() {
        String configFactory =
            getServletConfig().getInitParameter("configFactory");

        if (configFactory != null) {
            ModuleConfigFactory.setFactoryClass(configFactory);
        }
    }

    /**
     * <p>Initialize the module configuration information for the specified
     * module.</p>
     *
     * @param prefix Module prefix for this module
     * @param paths  Comma-separated list of context-relative resource path(s)
     *               for this modules's configuration resource(s)
     * @return The new module configuration instance.
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.1
     */
    protected ModuleConfig initModuleConfig(String prefix, String paths)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + prefix
                + "' configuration from '" + paths + "'");
        }

        // Parse the configuration for this module
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig(prefix);

        // Configure the Digester instance we will use
        Digester digester = initConfigDigester();

        List urls = splitAndResolvePaths(paths);
        URL url;

        for (Iterator i = urls.iterator(); i.hasNext();) {
            url = (URL) i.next();
            digester.push(config);
            this.parseModuleConfigFile(digester, url);
        }

        getServletContext().setAttribute(Globals.MODULE_KEY
            + config.getPrefix(), config);

        return config;
    }

    /**
     * <p>Parses one module config file.</p>
     *
     * @param digester Digester instance that does the parsing
     * @param path     The path to the config file to parse.
     * @throws UnavailableException if file cannot be read or parsed
     * @since Struts 1.2
     * @deprecated use parseModuleConfigFile(Digester digester, URL url)
     *             instead
     */
    protected void parseModuleConfigFile(Digester digester, String path)
        throws UnavailableException {
        try {
            List paths = splitAndResolvePaths(path);

            if (paths.size() > 0) {
                // Get first path as was the old behavior
                URL url = (URL) paths.get(0);

                parseModuleConfigFile(digester, url);
            } else {
                throw new UnavailableException("Cannot locate path " + path);
            }
        } catch (UnavailableException ex) {
            throw ex;
        } catch (ServletException ex) {
            handleConfigException(path, ex);
        }
    }

    /**
     * <p>Parses one module config file.</p>
     *
     * @param digester Digester instance that does the parsing
     * @param url      The url to the config file to parse.
     * @throws UnavailableException if file cannot be read or parsed
     * @since Struts 1.3
     */
    protected void parseModuleConfigFile(Digester digester, URL url)
        throws UnavailableException {

        try {
            digester.parse(url);
        } catch (IOException e) {
            handleConfigException(url.toString(), e);
        } catch (SAXException e) {
            handleConfigException(url.toString(), e);
        }
    }

    /**
     * <p>Simplifies exception handling in the parseModuleConfigFile
     * method.<p>
     *
     * @param path The path to which the exception relates.
     * @param e    The exception to be wrapped and thrown.
     * @throws UnavailableException as a wrapper around Exception
     */
    private void handleConfigException(String path, Exception e)
        throws UnavailableException {
        String msg = internal.getMessage("configParse", path);

        log.error(msg, e);
        throw new UnavailableException(msg);
    }

    /**
     * <p>Handle errors related to creating an instance of the specified
     * class.</p>
     *
     * @param className The className that could not be instantiated.
     * @param e         The exception that was caught.
     * @throws ServletException to communicate the error.
     */
    private void handleCreationException(String className, Exception e)
        throws ServletException {
        String errorMessage =
            internal.getMessage("configExtends.creation", className);

        log.error(errorMessage, e);
        throw new UnavailableException(errorMessage);
    }

    /**
     * <p>General handling for exceptions caught while inheriting config
     * information.</p>
     *
     * @param configType The type of configuration object of configName.
     * @param configName The name of the config that could not be extended.
     * @param e          The exception that was caught.
     * @throws ServletException to communicate the error.
     */
    private void handleGeneralExtensionException(String configType,
        String configName, Exception e)
        throws ServletException {
        String errorMessage =
            internal.getMessage("configExtends", configType, configName);

        log.error(errorMessage, e);
        throw new UnavailableException(errorMessage);
    }

    /**
     * <p>Handle errors caused by required fields that were not
     * specified.</p>
     *
     * @param field      The name of the required field that was not found.
     * @param configType The type of configuration object of configName.
     * @param configName The name of the config that's missing the required
     *                   value.
     * @throws ServletException to communicate the error.
     */
    private void handleValueRequiredException(String field, String configType,
        String configName) throws ServletException {
        String errorMessage =
            internal.getMessage("configFieldRequired", field, configType,
                configName);

        log.error(errorMessage);
        throw new UnavailableException(errorMessage);
    }

    /**
     * <p>Initialize the plug ins for the specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.1
     */
    protected void initModulePlugIns(ModuleConfig config)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + config.getPrefix()
                + "' plug ins");
        }

        PlugInConfig[] plugInConfigs = config.findPlugInConfigs();
        PlugIn[] plugIns = new PlugIn[plugInConfigs.length];

        getServletContext().setAttribute(Globals.PLUG_INS_KEY
            + config.getPrefix(), plugIns);

        for (int i = 0; i < plugIns.length; i++) {
            try {
                plugIns[i] =
                    (PlugIn) RequestUtils.applicationInstance(plugInConfigs[i]
                        .getClassName());
                BeanUtils.populate(plugIns[i], plugInConfigs[i].getProperties());

                // Pass the current plugIn config object to the PlugIn.
                // The property is set only if the plugin declares it.
                // This plugin config object is needed by Tiles
                try {
                    PropertyUtils.setProperty(plugIns[i],
                        "currentPlugInConfigObject", plugInConfigs[i]);
                } catch (Exception e) {
                    ;

                    // FIXME Whenever we fail silently, we must document a valid
                    // reason for doing so.  Why should we fail silently if a
                    // property can't be set on the plugin?

                    /**
                     * Between version 1.138-1.140 cedric made these changes.
                     * The exceptions are caught to deal with containers
                     * applying strict security. This was in response to bug
                     * #15736
                     *
                     * Recommend that we make the currentPlugInConfigObject part
                     * of the PlugIn Interface if we can, Rob
                     */
                }

                plugIns[i].init(this, config);
            } catch (ServletException e) {
                throw e;
            } catch (Exception e) {
                String errMsg =
                    internal.getMessage("plugIn.init",
                        plugInConfigs[i].getClassName());

                log(errMsg, e);
                throw new UnavailableException(errMsg);
            }
        }
    }

    /**
     * <p>Initialize the form beans for the specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.3
     */
    protected void initModuleFormBeans(ModuleConfig config)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + config.getPrefix()
                + "' form beans");
        }

        // Process form bean extensions.
        FormBeanConfig[] formBeans = config.findFormBeanConfigs();

        for (int i = 0; i < formBeans.length; i++) {
            FormBeanConfig beanConfig = formBeans[i];

            processFormBeanExtension(beanConfig, config);
        }

        for (int i = 0; i < formBeans.length; i++) {
            FormBeanConfig formBean = formBeans[i];

            // Verify that required fields are all present for the form config
            if (formBean.getType() == null) {
                handleValueRequiredException("type", formBean.getName(),
                    "form bean");
            }

            // ... and the property configs
            FormPropertyConfig[] fpcs = formBean.findFormPropertyConfigs();

            for (int j = 0; j < fpcs.length; j++) {
                FormPropertyConfig property = fpcs[j];

                if (property.getType() == null) {
                    handleValueRequiredException("type", property.getName(),
                        "form property");
                }
            }

            // Force creation and registration of DynaActionFormClass instances
            // for all dynamic form beans
            if (formBean.getDynamic()) {
                formBean.getDynaActionFormClass();
            }
        }
    }

    /**
     * <p>Extend the form bean's configuration as necessary.</p>
     *
     * @param beanConfig   the configuration to process.
     * @param moduleConfig the module configuration for this module.
     * @throws ServletException if initialization cannot be performed.
     */
    protected void processFormBeanExtension(FormBeanConfig beanConfig,
        ModuleConfig moduleConfig)
        throws ServletException {
        try {
            if (!beanConfig.isExtensionProcessed()) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing extensions for '"
                        + beanConfig.getName() + "'");
                }

                beanConfig =
                    processFormBeanConfigClass(beanConfig, moduleConfig);

                beanConfig.processExtends(moduleConfig);
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            handleGeneralExtensionException("FormBeanConfig",
                beanConfig.getName(), e);
        }
    }

    /**
     * <p>Checks if the current beanConfig is using the correct class based on
     * the class of its ancestor form bean config.</p>
     *
     * @param beanConfig   The form bean to check.
     * @param moduleConfig The config for the current module.
     * @return The form bean config using the correct class as determined by
     *         the config's ancestor and its own overridden value.
     * @throws UnavailableException if an instance of the form bean config
     *                              class cannot be created.
     * @throws ServletException     on class creation error
     */
    protected FormBeanConfig processFormBeanConfigClass(
        FormBeanConfig beanConfig, ModuleConfig moduleConfig)
        throws ServletException {
        String ancestor = beanConfig.getExtends();

        if (ancestor == null) {
            // Nothing to do, then
            return beanConfig;
        }

        // Make sure that this bean is of the right class
        FormBeanConfig baseConfig = moduleConfig.findFormBeanConfig(ancestor);

        if (baseConfig == null) {
            throw new UnavailableException("Unable to find " + "form bean '"
                + ancestor + "' to extend.");
        }

        // Was our bean's class overridden already?
        if (beanConfig.getClass().equals(FormBeanConfig.class)) {
            // Ensure that our bean is using the correct class
            if (!baseConfig.getClass().equals(beanConfig.getClass())) {
                // Replace the bean with an instance of the correct class
                FormBeanConfig newBeanConfig = null;
                String baseConfigClassName = baseConfig.getClass().getName();

                try {
                    newBeanConfig =
                        (FormBeanConfig) RequestUtils.applicationInstance(baseConfigClassName);

                    // copy the values
                    BeanUtils.copyProperties(newBeanConfig, beanConfig);

                    FormPropertyConfig[] fpc =
                        beanConfig.findFormPropertyConfigs();

                    for (int i = 0; i < fpc.length; i++) {
                        newBeanConfig.addFormPropertyConfig(fpc[i]);
                    }
                } catch (Exception e) {
                    handleCreationException(baseConfigClassName, e);
                }

                // replace beanConfig with newBeanConfig
                moduleConfig.removeFormBeanConfig(beanConfig);
                moduleConfig.addFormBeanConfig(newBeanConfig);
                beanConfig = newBeanConfig;
            }
        }

        return beanConfig;
    }

    /**
     * <p>Initialize the forwards for the specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     */
    protected void initModuleForwards(ModuleConfig config)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + config.getPrefix()
                + "' forwards");
        }

        // Process forwards extensions.
        ForwardConfig[] forwards = config.findForwardConfigs();

        for (int i = 0; i < forwards.length; i++) {
            ForwardConfig forward = forwards[i];

            processForwardExtension(forward, config, null);
        }

        for (int i = 0; i < forwards.length; i++) {
            ForwardConfig forward = forwards[i];

            // Verify that required fields are all present for the forward
            if (forward.getPath() == null) {
                handleValueRequiredException("path", forward.getName(),
                    "global forward");
            }
        }
    }

    /**
     * <p>Extend the forward's configuration as necessary.  If actionConfig is
     * provided, then this method will process the forwardConfig as part
     * of that actionConfig.  If actionConfig is null, the forwardConfig
     * will be processed as a global forward.</p>
     *
     * @param forwardConfig the configuration to process.
     * @param moduleConfig  the module configuration for this module.
     * @param actionConfig  If applicable, the config for the current action.
     * @throws ServletException if initialization cannot be performed.
     */
    protected void processForwardExtension(ForwardConfig forwardConfig,
        ModuleConfig moduleConfig, ActionConfig actionConfig)
        throws ServletException {
        try {
            if (!forwardConfig.isExtensionProcessed()) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing extensions for '"
                        + forwardConfig.getName() + "'");
                }

                forwardConfig =
                    processForwardConfigClass(forwardConfig, moduleConfig,
                        actionConfig);

                forwardConfig.processExtends(moduleConfig, actionConfig);
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            handleGeneralExtensionException("Forward", forwardConfig.getName(),
                e);
        }
    }

    /**
     * <p>Checks if the current forwardConfig is using the correct class based
     * on the class of its configuration ancestor.  If actionConfig is
     * provided, then this method will process the forwardConfig as part
     * of that actionConfig.  If actionConfig is null, the forwardConfig
     * will be processed as a global forward.</p>
     *
     * @param forwardConfig The forward to check.
     * @param moduleConfig  The config for the current module.
     * @param actionConfig  If applicable, the config for the current action.
     * @return The forward config using the correct class as determined by the
     *         config's ancestor and its own overridden value.
     * @throws UnavailableException if an instance of the forward config class
     *                              cannot be created.
     * @throws ServletException     on class creation error
     */
    protected ForwardConfig processForwardConfigClass(
        ForwardConfig forwardConfig, ModuleConfig moduleConfig,
        ActionConfig actionConfig)
        throws ServletException {
        String ancestor = forwardConfig.getExtends();

        if (ancestor == null) {
            // Nothing to do, then
            return forwardConfig;
        }

        // Make sure that this config is of the right class
        ForwardConfig baseConfig = null;
        if (actionConfig != null) {
            // Look for this in the actionConfig
            baseConfig = actionConfig.findForwardConfig(ancestor);
        }

        if (baseConfig == null) {
            // Either this is a forwardConfig that inherits a global config,
            //  or actionConfig is null
            baseConfig = moduleConfig.findForwardConfig(ancestor);
        }

        if (baseConfig == null) {
            throw new UnavailableException("Unable to find " + "forward '"
                + ancestor + "' to extend.");
        }

        // Was our forwards's class overridden already?
        if (forwardConfig.getClass().equals(ActionForward.class)) {
            // Ensure that our forward is using the correct class
            if (!baseConfig.getClass().equals(forwardConfig.getClass())) {
                // Replace the config with an instance of the correct class
                ForwardConfig newForwardConfig = null;
                String baseConfigClassName = baseConfig.getClass().getName();

                try {
                    newForwardConfig =
                        (ForwardConfig) RequestUtils.applicationInstance(
                                baseConfigClassName);

                    // copy the values
                    BeanUtils.copyProperties(newForwardConfig, forwardConfig);
                } catch (Exception e) {
                    handleCreationException(baseConfigClassName, e);
                }

                // replace forwardConfig with newForwardConfig
                if (actionConfig != null) {
                    actionConfig.removeForwardConfig(forwardConfig);
                    actionConfig.addForwardConfig(newForwardConfig);
                } else {
                    // this is a global forward
                    moduleConfig.removeForwardConfig(forwardConfig);
                    moduleConfig.addForwardConfig(newForwardConfig);
                }
                forwardConfig = newForwardConfig;
            }
        }

        return forwardConfig;
    }

    /**
     * <p>Initialize the exception handlers for the specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.3
     */
    protected void initModuleExceptionConfigs(ModuleConfig config)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + config.getPrefix()
                + "' forwards");
        }

        // Process exception config extensions.
        ExceptionConfig[] exceptions = config.findExceptionConfigs();

        for (int i = 0; i < exceptions.length; i++) {
            ExceptionConfig exception = exceptions[i];

            processExceptionExtension(exception, config, null);
        }

        for (int i = 0; i < exceptions.length; i++) {
            ExceptionConfig exception = exceptions[i];

            // Verify that required fields are all present for the config
            if (exception.getKey() == null) {
                handleValueRequiredException("key", exception.getType(),
                    "global exception config");
            }
        }
    }

    /**
     * <p>Extend the exception's configuration as necessary. If actionConfig is
     * provided, then this method will process the exceptionConfig as part
     * of that actionConfig.  If actionConfig is null, the exceptionConfig
     * will be processed as a global forward.</p>
     *
     * @param exceptionConfig the configuration to process.
     * @param moduleConfig    the module configuration for this module.
     * @param actionConfig  If applicable, the config for the current action.
     * @throws ServletException if initialization cannot be performed.
     */
    protected void processExceptionExtension(ExceptionConfig exceptionConfig,
        ModuleConfig moduleConfig, ActionConfig actionConfig)
        throws ServletException {
        try {
            if (!exceptionConfig.isExtensionProcessed()) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing extensions for '"
                        + exceptionConfig.getType() + "'");
                }

                exceptionConfig =
                    processExceptionConfigClass(exceptionConfig, moduleConfig,
                        actionConfig);

                exceptionConfig.processExtends(moduleConfig, actionConfig);
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            handleGeneralExtensionException("Exception",
                exceptionConfig.getType(), e);
        }
    }

    /**
     * <p>Checks if the current exceptionConfig is using the correct class
     * based on the class of its configuration ancestor. If actionConfig is
     * provided, then this method will process the exceptionConfig as part
     * of that actionConfig.  If actionConfig is null, the exceptionConfig
     * will be processed as a global forward.</p>
     *
     * @param exceptionConfig The config to check.
     * @param moduleConfig    The config for the current module.
     * @param actionConfig  If applicable, the config for the current action.
     * @return The exception config using the correct class as determined by
     *         the config's ancestor and its own overridden value.
     * @throws ServletException if an instance of the exception config class
     *                          cannot be created.
     */
    protected ExceptionConfig processExceptionConfigClass(
        ExceptionConfig exceptionConfig, ModuleConfig moduleConfig,
        ActionConfig actionConfig)
        throws ServletException {
        String ancestor = exceptionConfig.getExtends();

        if (ancestor == null) {
            // Nothing to do, then
            return exceptionConfig;
        }

        // Make sure that this config is of the right class
        ExceptionConfig baseConfig = null;
        if (actionConfig != null) {
            baseConfig = actionConfig.findExceptionConfig(ancestor);
        }

        if (baseConfig == null) {
            // This means either there's no actionConfig anyway, or the
            // ancestor is not defined within the action.
            baseConfig = moduleConfig.findExceptionConfig(ancestor);
        }

        if (baseConfig == null) {
            throw new UnavailableException("Unable to find "
                + "exception config '" + ancestor + "' to extend.");
        }

        // Was our config's class overridden already?
        if (exceptionConfig.getClass().equals(ExceptionConfig.class)) {
            // Ensure that our config is using the correct class
            if (!baseConfig.getClass().equals(exceptionConfig.getClass())) {
                // Replace the config with an instance of the correct class
                ExceptionConfig newExceptionConfig = null;
                String baseConfigClassName = baseConfig.getClass().getName();

                try {
                    newExceptionConfig =
                        (ExceptionConfig) RequestUtils.applicationInstance(
                            baseConfigClassName);

                    // copy the values
                    BeanUtils.copyProperties(newExceptionConfig,
                        exceptionConfig);
                } catch (Exception e) {
                    handleCreationException(baseConfigClassName, e);
                }

                // replace exceptionConfig with newExceptionConfig
                if (actionConfig != null) {
                    actionConfig.removeExceptionConfig(exceptionConfig);
                    actionConfig.addExceptionConfig(newExceptionConfig);
                } else {
                    moduleConfig.removeExceptionConfig(exceptionConfig);
                    moduleConfig.addExceptionConfig(newExceptionConfig);
                }
                exceptionConfig = newExceptionConfig;
            }
        }

        return exceptionConfig;
    }

    /**
     * <p>Initialize the action configs for the specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.3
     */
    protected void initModuleActions(ModuleConfig config)
        throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing module path '" + config.getPrefix()
                + "' action configs");
        }

        // Process ActionConfig extensions.
        ActionConfig[] actionConfigs = config.findActionConfigs();

        for (int i = 0; i < actionConfigs.length; i++) {
            ActionConfig actionConfig = actionConfigs[i];

            processActionConfigExtension(actionConfig, config);
        }

        for (int i = 0; i < actionConfigs.length; i++) {
            ActionConfig actionConfig = actionConfigs[i];

            // Verify that required fields are all present for the forward
            // configs
            ForwardConfig[] forwards = actionConfig.findForwardConfigs();

            for (int j = 0; j < forwards.length; j++) {
                ForwardConfig forward = forwards[j];

                if (forward.getPath() == null) {
                    handleValueRequiredException("path", forward.getName(),
                        "action forward");
                }
            }

            // ... and the exception configs
            ExceptionConfig[] exceptions = actionConfig.findExceptionConfigs();

            for (int j = 0; j < exceptions.length; j++) {
                ExceptionConfig exception = exceptions[j];

                if (exception.getKey() == null) {
                    handleValueRequiredException("key", exception.getType(),
                        "action exception config");
                }
            }
        }
    }

    /**
     * <p>Extend the action's configuration as necessary.</p>
     *
     * @param actionConfig the configuration to process.
     * @param moduleConfig the module configuration for this module.
     * @throws ServletException if initialization cannot be performed.
     */
    protected void processActionConfigExtension(ActionConfig actionConfig,
        ModuleConfig moduleConfig)
        throws ServletException {
        try {
            if (!actionConfig.isExtensionProcessed()) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing extensions for '"
                        + actionConfig.getPath() + "'");
                }

                actionConfig =
                    processActionConfigClass(actionConfig, moduleConfig);

                actionConfig.processExtends(moduleConfig);
            }

            // Process forwards extensions.
            ForwardConfig[] forwards = actionConfig.findForwardConfigs();
            for (int i = 0; i < forwards.length; i++) {
                ForwardConfig forward = forwards[i];
                processForwardExtension(forward, moduleConfig, actionConfig);
            }

            // Process exception extensions.
            ExceptionConfig[] exceptions = actionConfig.findExceptionConfigs();
            for (int i = 0; i < exceptions.length; i++) {
                ExceptionConfig exception = exceptions[i];
                processExceptionExtension(exception, moduleConfig,
                    actionConfig);
            }
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            handleGeneralExtensionException("Action", actionConfig.getPath(), e);
        }
    }

    /**
     * <p>Checks if the current actionConfig is using the correct class based
     * on the class of its ancestor ActionConfig.</p>
     *
     * @param actionConfig The action config to check.
     * @param moduleConfig The config for the current module.
     * @return The config object using the correct class as determined by the
     *         config's ancestor and its own overridden value.
     * @throws ServletException if an instance of the action config class
     *                          cannot be created.
     */
    protected ActionConfig processActionConfigClass(ActionConfig actionConfig,
        ModuleConfig moduleConfig)
        throws ServletException {
        String ancestor = actionConfig.getExtends();

        if (ancestor == null) {
            // Nothing to do, then
            return actionConfig;
        }

        // Make sure that this config is of the right class
        ActionConfig baseConfig = moduleConfig.findActionConfig(ancestor);

        if (baseConfig == null) {
            throw new UnavailableException("Unable to find "
                + "action config for '" + ancestor + "' to extend.");
        }

        // Was our actionConfig's class overridden already?
        if (actionConfig.getClass().equals(ActionMapping.class)) {
            // Ensure that our config is using the correct class
            if (!baseConfig.getClass().equals(actionConfig.getClass())) {
                // Replace the config with an instance of the correct class
                ActionConfig newActionConfig = null;
                String baseConfigClassName = baseConfig.getClass().getName();

                try {
                    newActionConfig =
                        (ActionConfig) RequestUtils.applicationInstance(baseConfigClassName);

                    // copy the values
                    BeanUtils.copyProperties(newActionConfig, actionConfig);

                    // copy the forward and exception configs, too
                    ForwardConfig[] forwards =
                        actionConfig.findForwardConfigs();

                    for (int i = 0; i < forwards.length; i++) {
                        newActionConfig.addForwardConfig(forwards[i]);
                    }

                    ExceptionConfig[] exceptions =
                        actionConfig.findExceptionConfigs();

                    for (int i = 0; i < exceptions.length; i++) {
                        newActionConfig.addExceptionConfig(exceptions[i]);
                    }
                } catch (Exception e) {
                    handleCreationException(baseConfigClassName, e);
                }

                // replace actionConfig with newActionConfig
                moduleConfig.removeActionConfig(actionConfig);
                moduleConfig.addActionConfig(newActionConfig);
                actionConfig = newActionConfig;
            }
        }

        return actionConfig;
    }

    /**
     * <p>Initialize the application <code>MessageResources</code> for the
     * specified module.</p>
     *
     * @param config ModuleConfig information for this module
     * @throws ServletException if initialization cannot be performed
     * @since Struts 1.1
     */
    protected void initModuleMessageResources(ModuleConfig config)
        throws ServletException {
        MessageResourcesConfig[] mrcs = config.findMessageResourcesConfigs();

        for (int i = 0; i < mrcs.length; i++) {
            if ((mrcs[i].getFactory() == null)
                || (mrcs[i].getParameter() == null)) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("Initializing module path '" + config.getPrefix()
                    + "' message resources from '" + mrcs[i].getParameter()
                    + "'");
            }

            String factory = mrcs[i].getFactory();

            MessageResourcesFactory.setFactoryClass(factory);

            MessageResourcesFactory factoryObject =
                MessageResourcesFactory.createFactory();

            factoryObject.setConfig(mrcs[i]);

            MessageResources resources =
                factoryObject.createResources(mrcs[i].getParameter());

            resources.setReturnNull(mrcs[i].getNull());
            resources.setEscape(mrcs[i].isEscape());
            getServletContext().setAttribute(mrcs[i].getKey()
                + config.getPrefix(), resources);
        }
    }

    /**
     * <p>Create (if needed) and return a new <code>Digester</code> instance
     * that has been initialized to process Struts module configuration files
     * and configure a corresponding <code>ModuleConfig</code> object (which
     * must be pushed on to the evaluation stack before parsing begins).</p>
     *
     * @return A new configured <code>Digester</code> instance.
     * @throws ServletException if a Digester cannot be configured
     * @since Struts 1.1
     */
    protected Digester initConfigDigester()
        throws ServletException {
        // :FIXME: Where can ServletException be thrown?
        // Do we have an existing instance?
        if (configDigester != null) {
            return (configDigester);
        }

        // Create a new Digester instance with standard capabilities
        configDigester = new Digester();
        configDigester.setNamespaceAware(true);
        configDigester.setValidating(this.isValidating());
        configDigester.setUseContextClassLoader(true);
        configDigester.addRuleSet(new ConfigRuleSet());

        for (int i = 0; i < registrations.length; i += 2) {
            URL url = this.getClass().getResource(registrations[i + 1]);

            if (url != null) {
                configDigester.register(registrations[i], url.toString());
            }
        }

        this.addRuleSets();

        // Return the completely configured Digester instance
        return (configDigester);
    }

    /**
     * <p>Add any custom RuleSet instances to configDigester that have been
     * specified in the <code>rulesets</code> init parameter.</p>
     *
     * @throws ServletException if an error occurs
     */
    private void addRuleSets()
        throws ServletException {
        String rulesets = getServletConfig().getInitParameter("rulesets");

        if (rulesets == null) {
            rulesets = "";
        }

        rulesets = rulesets.trim();

        String ruleset;

        while (rulesets.length() > 0) {
            int comma = rulesets.indexOf(",");

            if (comma < 0) {
                ruleset = rulesets.trim();
                rulesets = "";
            } else {
                ruleset = rulesets.substring(0, comma).trim();
                rulesets = rulesets.substring(comma + 1).trim();
            }

            if (log.isDebugEnabled()) {
                log.debug("Configuring custom Digester Ruleset of type "
                    + ruleset);
            }

            try {
                RuleSet instance =
                    (RuleSet) RequestUtils.applicationInstance(ruleset);

                this.configDigester.addRuleSet(instance);
            } catch (Exception e) {
                log.error("Exception configuring custom Digester RuleSet", e);
                throw new ServletException(e);
            }
        }
    }

    /**
     * <p>Check the status of the <code>validating</code> initialization
     * parameter.</p>
     *
     * @return true if the module Digester should validate.
     */
    private boolean isValidating() {
        boolean validating = true;
        String value = getServletConfig().getInitParameter("validating");

        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)
            || "n".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value)) {
            validating = false;
        }

        return validating;
    }

    /**
     * <p>Initialize our internal MessageResources bundle.</p>
     *
     * @throws ServletException     if we cannot initialize these resources
     * @throws UnavailableException if we cannot load  resources
     */
    protected void initInternal()
        throws ServletException {
        try {
            internal = MessageResources.getMessageResources(internalName);
        } catch (MissingResourceException e) {
            log.error("Cannot load internal resources from '" + internalName
                + "'", e);
            throw new UnavailableException(
                "Cannot load internal resources from '" + internalName + "'");
        }
    }

    /**
     * <p>Parse the configuration documents specified by the
     * <code>chainConfig</code> init-param to configure the default {@link
     * org.apache.commons.chain.Catalog} that is registered in the {@link
     * CatalogFactory} instance for this application.</p>
     *
     * @throws ServletException if an error occurs.
     */
    protected void initChain()
        throws ServletException {
        // Parse the configuration file specified by path or resource
        try {
            String value;

            value = getServletConfig().getInitParameter("chainConfig");

            if (value != null) {
                chainConfig = value;
            }

            ConfigParser parser = new ConfigParser();
            List urls = splitAndResolvePaths(chainConfig);
            URL resource;

            for (Iterator i = urls.iterator(); i.hasNext();) {
                resource = (URL) i.next();
                log.info("Loading chain catalog from " + resource);
                parser.parse(resource);
            }
        } catch (Exception e) {
            log.error("Exception loading resources", e);
            throw new ServletException(e);
        }
    }

    /**
     * <p>Initialize other global characteristics of the controller
     * servlet.</p>
     *
     * @throws ServletException if we cannot initialize these resources
     */
    protected void initOther()
        throws ServletException {
        String value;

        value = getServletConfig().getInitParameter("config");

        if (value != null) {
            config = value;
        }

        // Backwards compatibility for form beans of Java wrapper classes
        // Set to true for strict Struts 1.0 compatibility
        value = getServletConfig().getInitParameter("convertNull");

        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
            || "on".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value)
            || "1".equalsIgnoreCase(value)) {
            convertNull = true;
        }

        if (convertNull) {
            ConvertUtils.deregister();
            ConvertUtils.register(new BigDecimalConverter(null),
                BigDecimal.class);
            ConvertUtils.register(new BigIntegerConverter(null),
                BigInteger.class);
            ConvertUtils.register(new BooleanConverter(null), Boolean.class);
            ConvertUtils.register(new ByteConverter(null), Byte.class);
            ConvertUtils.register(new CharacterConverter(null), Character.class);
            ConvertUtils.register(new DoubleConverter(null), Double.class);
            ConvertUtils.register(new FloatConverter(null), Float.class);
            ConvertUtils.register(new IntegerConverter(null), Integer.class);
            ConvertUtils.register(new LongConverter(null), Long.class);
            ConvertUtils.register(new ShortConverter(null), Short.class);
        }
    }

    /**
     * <p>Initialize the servlet mapping under which our controller servlet is
     * being accessed.  This will be used in the <code>&html:form&gt;</code>
     * tag to generate correct destination URLs for form submissions.</p>
     *
     * @throws ServletException if error happens while scanning web.xml
     */
    protected void initServlet()
        throws ServletException {
        // Remember our servlet name
        this.servletName = getServletConfig().getServletName();

        // Prepare a Digester to scan the web application deployment descriptor
        Digester digester = new Digester();

        digester.push(this);
        digester.setNamespaceAware(true);
        digester.setValidating(false);

        // Register our local copy of the DTDs that we can find
        for (int i = 0; i < registrations.length; i += 2) {
            URL url = this.getClass().getResource(registrations[i + 1]);

            if (url != null) {
                digester.register(registrations[i], url.toString());
            }
        }

        // Configure the processing rules that we need
        digester.addCallMethod("web-app/servlet-mapping", "addServletMapping", 2);
        digester.addCallParam("web-app/servlet-mapping/servlet-name", 0);
        digester.addCallParam("web-app/servlet-mapping/url-pattern", 1);

        // Process the web application deployment descriptor
        if (log.isDebugEnabled()) {
            log.debug("Scanning web.xml for controller servlet mapping");
        }

        InputStream input =
            getServletContext().getResourceAsStream("/WEB-INF/web.xml");

        if (input == null) {
            log.error(internal.getMessage("configWebXml"));
            throw new ServletException(internal.getMessage("configWebXml"));
        }

        try {
            digester.parse(input);
        } catch (IOException e) {
            log.error(internal.getMessage("configWebXml"), e);
            throw new ServletException(e);
        } catch (SAXException e) {
            log.error(internal.getMessage("configWebXml"), e);
            throw new ServletException(e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                log.error(internal.getMessage("configWebXml"), e);
                throw new ServletException(e);
            }
        }

        // Record a servlet context attribute (if appropriate)
        if (log.isDebugEnabled()) {
            log.debug("Mapping for servlet '" + servletName + "' = '"
                + servletMapping + "'");
        }

        if (servletMapping != null) {
            getServletContext().setAttribute(Globals.SERVLET_KEY, servletMapping);
        }
    }

    /**
     * <p>Takes a comma-delimited string and splits it into paths, then
     * resolves those paths using the ServletContext and appropriate
     * ClassLoader.  When loading from the classloader, multiple resources per
     * path are supported to support, for example, multiple jars containing
     * the same named config file.</p>
     *
     * @param paths A comma-delimited string of paths
     * @return A list of resolved URL's for all found resources
     * @throws ServletException if a servlet exception is thrown
     */
    protected List splitAndResolvePaths(String paths)
        throws ServletException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }

        ArrayList resolvedUrls = new ArrayList();

        URL resource;
        String path = null;

        try {
            // Process each specified resource path
            while (paths.length() > 0) {
                resource = null;

                int comma = paths.indexOf(',');

                if (comma >= 0) {
                    path = paths.substring(0, comma).trim();
                    paths = paths.substring(comma + 1);
                } else {
                    path = paths.trim();
                    paths = "";
                }

                if (path.length() < 1) {
                    break;
                }

                if (path.charAt(0) == '/') {
                    resource = getServletContext().getResource(path);
                }

                if (resource == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to locate " + path
                            + " in the servlet context, "
                            + "trying classloader.");
                    }

                    Enumeration e = loader.getResources(path);

                    if (!e.hasMoreElements()) {
                        String msg = internal.getMessage("configMissing", path);

                        log.error(msg);
                        throw new UnavailableException(msg);
                    } else {
                        while (e.hasMoreElements()) {
                            resolvedUrls.add(e.nextElement());
                        }
                    }
                } else {
                    resolvedUrls.add(resource);
                }
            }
        } catch (MalformedURLException e) {
            handleConfigException(path, e);
        } catch (IOException e) {
            handleConfigException(path, e);
        }

        return resolvedUrls;
    }

    /**
     * <p>Perform the standard request processing for this request, and create
     * the corresponding response.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception is thrown
     */
    protected void process(HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {
        ModuleUtils.getInstance().selectModule(request, getServletContext());

        ModuleConfig config = getModuleConfig(request);

        RequestProcessor processor = getProcessorForModule(config);

        if (processor == null) {
            processor = getRequestProcessor(config);
        }

        processor.process(request, response);
    }
}

/*
 * $Id: RequestUtils.java 524895 2007-04-02 19:29:21Z germuska $
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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ActionServletWrapper;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
import org.apache.struts.upload.MultipartRequestWrapper;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <p>General purpose utility methods related to processing a servlet request
 * in the Struts controller framework.</p>
 *
 * @version $Rev: 524895 $ $Date: 2007-04-02 21:29:21 +0200 (Mon, 02 Apr 2007) $
 */
public class RequestUtils {
    // ------------------------------------------------------- Static Variables

    /**
     * <p>Commons Logging instance.</p>
     */
    protected static Log log = LogFactory.getLog(RequestUtils.class);

    // --------------------------------------------------------- Public Methods

    /**
     * <p>Create and return an absolute URL for the specified context-relative
     * path, based on the server and context information in the specified
     * request.</p>
     *
     * @param request The servlet request we are processing
     * @param path    The context-relative path (must start with '/')
     * @return absolute URL based on context-relative path
     * @throws MalformedURLException if we cannot create an absolute URL
     */
    public static URL absoluteURL(HttpServletRequest request, String path)
        throws MalformedURLException {
        return (new URL(serverURL(request), request.getContextPath() + path));
    }

    /**
     * <p>Return the <code>Class</code> object for the specified fully
     * qualified class name, from this web application's class loader.</p>
     *
     * @param className Fully qualified class name to be loaded
     * @return Class object
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class applicationClass(String className)
        throws ClassNotFoundException {
        return applicationClass(className, null);
    }

    /**
     * <p>Return the <code>Class</code> object for the specified fully
     * qualified class name, from this web application's class loader.</p>
     *
     * @param className   Fully qualified class name to be loaded
     * @param classLoader The desired classloader to use
     * @return Class object
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class applicationClass(String className,
        ClassLoader classLoader)
        throws ClassNotFoundException {
        if (classLoader == null) {
            // Look up the class loader to be used
            classLoader = Thread.currentThread().getContextClassLoader();

            if (classLoader == null) {
                classLoader = RequestUtils.class.getClassLoader();
            }
        }

        // Attempt to load the specified class
        return (classLoader.loadClass(className));
    }

    /**
     * <p>Return a new instance of the specified fully qualified class name,
     * after loading the class from this web application's class loader. The
     * specified class <strong>MUST</strong> have a public zero-arguments
     * constructor.</p>
     *
     * @param className Fully qualified class name to use
     * @return new instance of class
     * @throws ClassNotFoundException if the class cannot be found
     * @throws IllegalAccessException if the class or its constructor is not
     *                                accessible
     * @throws InstantiationException if this class represents an abstract
     *                                class, an interface, an array class, a
     *                                primitive type, or void
     * @throws InstantiationException if this class has no zero-arguments
     *                                constructor
     */
    public static Object applicationInstance(String className)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        return applicationInstance(className, null);
    }

    /**
     * <p>Return a new instance of the specified fully qualified class name,
     * after loading the class from this web application's class loader. The
     * specified class <strong>MUST</strong> have a public zero-arguments
     * constructor.</p>
     *
     * @param className   Fully qualified class name to use
     * @param classLoader The desired classloader to use
     * @return new instance of class
     * @throws ClassNotFoundException if the class cannot be found
     * @throws IllegalAccessException if the class or its constructor is not
     *                                accessible
     * @throws InstantiationException if this class represents an abstract
     *                                class, an interface, an array class, a
     *                                primitive type, or void
     * @throws InstantiationException if this class has no zero-arguments
     *                                constructor
     */
    public static Object applicationInstance(String className,
        ClassLoader classLoader)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        return (applicationClass(className, classLoader).newInstance());
    }

    /**
     * <p>Create (if necessary) and return an <code>ActionForm</code> instance
     * appropriate for this request.  If no <code>ActionForm</code> instance
     * is required, return <code>null</code>.</p>
     *
     * @param request      The servlet request we are processing
     * @param mapping      The action mapping for this request
     * @param moduleConfig The configuration for this module
     * @param servlet      The action servlet
     * @return ActionForm instance associated with this request
     */
    public static ActionForm createActionForm(HttpServletRequest request,
        ActionMapping mapping, ModuleConfig moduleConfig, ActionServlet servlet) {
        // Is there a form bean associated with this mapping?
        String attribute = mapping.getAttribute();

        if (attribute == null) {
            return (null);
        }

        // Look up the form bean configuration information to use
        String name = mapping.getName();
        FormBeanConfig config = moduleConfig.findFormBeanConfig(name);

        if (config == null) {
            log.warn("No FormBeanConfig found under '" + name + "'");

            return (null);
        }

        ActionForm instance =
            lookupActionForm(request, attribute, mapping.getScope());

        // Can we recycle the existing form bean instance (if there is one)?
        if ((instance != null) && config.canReuse(instance)) {
            return (instance);
        }

        return createActionForm(config, servlet);
    }

    private static ActionForm lookupActionForm(HttpServletRequest request,
        String attribute, String scope) {
        // Look up any existing form bean instance
        if (log.isDebugEnabled()) {
            log.debug(" Looking for ActionForm bean instance in scope '"
                + scope + "' under attribute key '" + attribute + "'");
        }

        ActionForm instance = null;
        HttpSession session = null;

        if ("request".equals(scope)) {
            instance = (ActionForm) request.getAttribute(attribute);
        } else {
            session = request.getSession();
            instance = (ActionForm) session.getAttribute(attribute);
        }

        return (instance);
    }

    /**
     * <p>Create and return an <code>ActionForm</code> instance appropriate to
     * the information in <code>config</code>.</p>
     *
     * <p>Does not perform any checks to see if an existing ActionForm exists
     * which could be reused.</p>
     *
     * @param config  The configuration for the Form bean which is to be
     *                created.
     * @param servlet The action servlet
     * @return ActionForm instance associated with this request
     */
    public static ActionForm createActionForm(FormBeanConfig config,
        ActionServlet servlet) {
        if (config == null) {
            return (null);
        }

        ActionForm instance = null;

        // Create and return a new form bean instance
        try {
            instance = config.createActionForm(servlet);

            if (log.isDebugEnabled()) {
                log.debug(" Creating new "
                    + (config.getDynamic() ? "DynaActionForm" : "ActionForm")
                    + " instance of type '" + config.getType() + "'");
                log.trace(" --> " + instance);
            }
        } catch (Throwable t) {
            log.error(servlet.getInternal().getMessage("formBean",
                    config.getType()), t);
        }

        return (instance);
    }

    /**
     * <p>Retrieves the servlet mapping pattern for the specified {@link ActionServlet}.</p>
     *
     * @return the servlet mapping
     * @see Globals#SERVLET_KEY
     * @since Struts 1.3.6
     */
    public static String getServletMapping(ActionServlet servlet) {
        ServletContext servletContext = servlet.getServletConfig().getServletContext();
        return (String)servletContext.getAttribute(Globals.SERVLET_KEY);
    }

    /**
     * <p>Look up and return current user locale, based on the specified
     * parameters.</p>
     *
     * @param request The request used to lookup the Locale
     * @param locale  Name of the session attribute for our user's Locale.  If
     *                this is <code>null</code>, the default locale key is
     *                used for the lookup.
     * @return current user locale
     * @since Struts 1.2
     */
    public static Locale getUserLocale(HttpServletRequest request, String locale) {
        Locale userLocale = null;
        HttpSession session = request.getSession(false);

        if (locale == null) {
            locale = Globals.LOCALE_KEY;
        }

        // Only check session if sessions are enabled
        if (session != null) {
            userLocale = (Locale) session.getAttribute(locale);
        }

        if (userLocale == null) {
            // Returns Locale based on Accept-Language header or the server default
            userLocale = request.getLocale();
        }

        return userLocale;
    }

    /**
     * <p>Populate the properties of the specified JavaBean from the specified
     * HTTP request, based on matching each parameter name against the
     * corresponding JavaBeans "property setter" methods in the bean's class.
     * Suitable conversion is done for argument types as described under
     * <code>convert()</code>.</p>
     *
     * @param bean    The JavaBean whose properties are to be set
     * @param request The HTTP request whose parameters are to be used to
     *                populate bean properties
     * @throws ServletException if an exception is thrown while setting
     *                          property values
     */
    public static void populate(Object bean, HttpServletRequest request)
        throws ServletException {
        populate(bean, null, null, request);
    }

    /**
     * <p>Populate the properties of the specified JavaBean from the specified
     * HTTP request, based on matching each parameter name (plus an optional
     * prefix and/or suffix) against the corresponding JavaBeans "property
     * setter" methods in the bean's class. Suitable conversion is done for
     * argument types as described under <code>setProperties</code>.</p>
     *
     * <p>If you specify a non-null <code>prefix</code> and a non-null
     * <code>suffix</code>, the parameter name must match
     * <strong>both</strong> conditions for its value(s) to be used in
     * populating bean properties. If the request's content type is
     * "multipart/form-data" and the method is "POST", the
     * <code>HttpServletRequest</code> object will be wrapped in a
     * <code>MultipartRequestWrapper</code object.</p>
     *
     * @param bean    The JavaBean whose properties are to be set
     * @param prefix  The prefix (if any) to be prepend to bean property names
     *                when looking for matching parameters
     * @param suffix  The suffix (if any) to be appended to bean property
     *                names when looking for matching parameters
     * @param request The HTTP request whose parameters are to be used to
     *                populate bean properties
     * @throws ServletException if an exception is thrown while setting
     *                          property values
     */
    public static void populate(Object bean, String prefix, String suffix,
        HttpServletRequest request)
        throws ServletException {
        // Build a list of relevant request parameters from this request
        HashMap properties = new HashMap();

        // Iterator of parameter names
        Enumeration names = null;

        // Map for multipart parameters
        Map multipartParameters = null;

        String contentType = request.getContentType();
        String method = request.getMethod();
        boolean isMultipart = false;

        if (bean instanceof ActionForm) {
            ((ActionForm) bean).setMultipartRequestHandler(null);
        }

        MultipartRequestHandler multipartHandler = null;
        if ((contentType != null)
            && (contentType.startsWith("multipart/form-data"))
            && (method.equalsIgnoreCase("POST"))) {
            // Get the ActionServletWrapper from the form bean
            ActionServletWrapper servlet;

            if (bean instanceof ActionForm) {
                servlet = ((ActionForm) bean).getServletWrapper();
            } else {
                throw new ServletException("bean that's supposed to be "
                    + "populated from a multipart request is not of type "
                    + "\"org.apache.struts.action.ActionForm\", but type "
                    + "\"" + bean.getClass().getName() + "\"");
            }

            // Obtain a MultipartRequestHandler
            multipartHandler = getMultipartHandler(request);

            if (multipartHandler != null) {
                isMultipart = true;

                // Set servlet and mapping info
                servlet.setServletFor(multipartHandler);
                multipartHandler.setMapping((ActionMapping) request
                    .getAttribute(Globals.MAPPING_KEY));

                // Initialize multipart request class handler
                multipartHandler.handleRequest(request);

                //stop here if the maximum length has been exceeded
                Boolean maxLengthExceeded =
                    (Boolean) request.getAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);

                if ((maxLengthExceeded != null)
                    && (maxLengthExceeded.booleanValue())) {
                    ((ActionForm) bean).setMultipartRequestHandler(multipartHandler);
                    return;
                }

                //retrieve form values and put into properties
                multipartParameters =
                    getAllParametersForMultipartRequest(request,
                        multipartHandler);
                names = Collections.enumeration(multipartParameters.keySet());
            }
        }

        if (!isMultipart) {
            names = request.getParameterNames();
        }

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String stripped = name;

            if (prefix != null) {
                if (!stripped.startsWith(prefix)) {
                    continue;
                }

                stripped = stripped.substring(prefix.length());
            }

            if (suffix != null) {
                if (!stripped.endsWith(suffix)) {
                    continue;
                }

                stripped =
                    stripped.substring(0, stripped.length() - suffix.length());
            }

            Object parameterValue = null;

            if (isMultipart) {
                parameterValue = multipartParameters.get(name);
                parameterValue = rationalizeMultipleFileProperty(bean, name, parameterValue);
            } else {
                parameterValue = request.getParameterValues(name);
            }

            // Populate parameters, except "standard" struts attributes
            // such as 'org.apache.struts.action.CANCEL'
            if (!(stripped.startsWith("org.apache.struts."))) {
                properties.put(stripped, parameterValue);
            }
        }

        // Set the corresponding properties of our bean
        try {
            BeanUtils.populate(bean, properties);
        } catch (Exception e) {
            throw new ServletException("BeanUtils.populate", e);
        } finally {
            if (multipartHandler != null) {
                // Set the multipart request handler for our ActionForm.
                // If the bean isn't an ActionForm, an exception would have been
                // thrown earlier, so it's safe to assume that our bean is
                // in fact an ActionForm.
                ((ActionForm) bean).setMultipartRequestHandler(multipartHandler);
            }
        }
    }

    /**
     * <p>If the given form bean can accept multiple FormFile objects but the user only uploaded a single, then 
     * the property will not match the form bean type.  This method performs some simple checks to try to accommodate
     * that situation.</p>
     * @param bean
     * @param name
     * @param parameterValue
     * @return 
     * @throws ServletException if the introspection has any errors.
     */
    private static Object rationalizeMultipleFileProperty(Object bean, String name, Object parameterValue) throws ServletException {
    	if (!(parameterValue instanceof FormFile)) return parameterValue;

    	FormFile formFileValue = (FormFile) parameterValue;
    	try {
			Class propertyType = PropertyUtils.getPropertyType(bean, name);

			if (propertyType.isAssignableFrom(List.class)) {
				ArrayList list = new ArrayList(1);
				list.add(formFileValue);
				return list;
			}

			if (propertyType.isArray() && propertyType.getComponentType().equals(FormFile.class)) {
				return new FormFile[] { formFileValue };
			}

    	} catch (IllegalAccessException e) {
			throw new ServletException(e);
		} catch (InvocationTargetException e) {
			throw new ServletException(e);
		} catch (NoSuchMethodException e) {
			throw new ServletException(e);
		}
    	
		// no changes
    	return parameterValue;
    	
	}

	/**
     * <p>Try to locate a multipart request handler for this request. First,
     * look for a mapping-specific handler stored for us under an attribute.
     * If one is not present, use the global multipart handler, if there is
     * one.</p>
     *
     * @param request The HTTP request for which the multipart handler should
     *                be found.
     * @return the multipart handler to use, or null if none is found.
     * @throws ServletException if any exception is thrown while attempting to
     *                          locate the multipart handler.
     */
    private static MultipartRequestHandler getMultipartHandler(
        HttpServletRequest request)
        throws ServletException {
        MultipartRequestHandler multipartHandler = null;
        String multipartClass =
            (String) request.getAttribute(Globals.MULTIPART_KEY);

        request.removeAttribute(Globals.MULTIPART_KEY);

        // Try to initialize the mapping specific request handler
        if (multipartClass != null) {
            try {
                multipartHandler =
                    (MultipartRequestHandler) applicationInstance(multipartClass);
            } catch (ClassNotFoundException cnfe) {
                log.error("MultipartRequestHandler class \"" + multipartClass
                    + "\" in mapping class not found, "
                    + "defaulting to global multipart class");
            } catch (InstantiationException ie) {
                log.error("InstantiationException when instantiating "
                    + "MultipartRequestHandler \"" + multipartClass + "\", "
                    + "defaulting to global multipart class, exception: "
                    + ie.getMessage());
            } catch (IllegalAccessException iae) {
                log.error("IllegalAccessException when instantiating "
                    + "MultipartRequestHandler \"" + multipartClass + "\", "
                    + "defaulting to global multipart class, exception: "
                    + iae.getMessage());
            }

            if (multipartHandler != null) {
                return multipartHandler;
            }
        }

        ModuleConfig moduleConfig =
            ModuleUtils.getInstance().getModuleConfig(request);

        multipartClass = moduleConfig.getControllerConfig().getMultipartClass();

        // Try to initialize the global request handler
        if (multipartClass != null) {
            try {
                multipartHandler =
                    (MultipartRequestHandler) applicationInstance(multipartClass);
            } catch (ClassNotFoundException cnfe) {
                throw new ServletException("Cannot find multipart class \""
                    + multipartClass + "\"" + ", exception: "
                    + cnfe.getMessage());
            } catch (InstantiationException ie) {
                throw new ServletException(
                    "InstantiationException when instantiating "
                    + "multipart class \"" + multipartClass + "\", exception: "
                    + ie.getMessage());
            } catch (IllegalAccessException iae) {
                throw new ServletException(
                    "IllegalAccessException when instantiating "
                    + "multipart class \"" + multipartClass + "\", exception: "
                    + iae.getMessage());
            }

            if (multipartHandler != null) {
                return multipartHandler;
            }
        }

        return multipartHandler;
    }

    /**
     * <p>Create a <code>Map</code> containing all of the parameters supplied
     * for a multipart request, keyed by parameter name. In addition to text
     * and file elements from the multipart body, query string parameters are
     * included as well.</p>
     *
     * @param request          The (wrapped) HTTP request whose parameters are
     *                         to be added to the map.
     * @param multipartHandler The multipart handler used to parse the
     *                         request.
     * @return the map containing all parameters for this multipart request.
     */
    private static Map getAllParametersForMultipartRequest(
        HttpServletRequest request, MultipartRequestHandler multipartHandler) {
        Map parameters = new HashMap();
        Hashtable elements = multipartHandler.getAllElements();
        Enumeration e = elements.keys();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();

            parameters.put(key, elements.get(key));
        }

        if (request instanceof MultipartRequestWrapper) {
            request =
                (HttpServletRequest) ((MultipartRequestWrapper) request)
                .getRequest();
            e = request.getParameterNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();

                parameters.put(key, request.getParameterValues(key));
            }
        } else {
            log.debug("Gathering multipart parameters for unwrapped request");
        }

        return parameters;
    }

    /**
     * <p>Compute the printable representation of a URL, leaving off the
     * scheme/host/port part if no host is specified. This will typically be
     * the case for URLs that were originally created from relative or
     * context-relative URIs.</p>
     *
     * @param url URL to render in a printable representation
     * @return printable representation of a URL
     */
    public static String printableURL(URL url) {
        if (url.getHost() != null) {
            return (url.toString());
        }

        String file = url.getFile();
        String ref = url.getRef();

        if (ref == null) {
            return (file);
        } else {
            StringBuffer sb = new StringBuffer(file);

            sb.append('#');
            sb.append(ref);

            return (sb.toString());
        }
    }

    /**
     * <p>Return the context-relative URL that corresponds to the specified
     * {@link ActionConfig}, relative to the module associated with the
     * current modules's {@link ModuleConfig}.</p>
     *
     * @param request The servlet request we are processing
     * @param action  ActionConfig to be evaluated
     * @param pattern URL pattern used to map the controller servlet
     * @return context-relative URL relative to the module
     * @since Struts 1.1
     */
    public static String actionURL(HttpServletRequest request,
        ActionConfig action, String pattern) {
        StringBuffer sb = new StringBuffer();

        if (pattern.endsWith("/*")) {
            sb.append(pattern.substring(0, pattern.length() - 2));
            sb.append(action.getPath());
        } else if (pattern.startsWith("*.")) {
            ModuleConfig appConfig =
                ModuleUtils.getInstance().getModuleConfig(request);

            sb.append(appConfig.getPrefix());
            sb.append(action.getPath());
            sb.append(pattern.substring(1));
        } else {
            throw new IllegalArgumentException(pattern);
        }

        return sb.toString();
    }

    /**
     * <p>Return the context-relative URL that corresponds to the specified
     * <code>ForwardConfig</code>. The URL is calculated based on the
     * properties of the {@link ForwardConfig} instance as follows:</p>
     *
     * <ul>
     *
     *
     * <li>If the <code>contextRelative</code> property is set, it is assumed
     * that the <code>path</code> property contains a path that is already
     * context-relative:
     *
     * <ul>
     *
     * <li>If the <code>path</code> property value starts with a slash, it is
     * returned unmodified.</li> <li>If the <code>path</code> property value
     * does not start with a slash, a slash is prepended.</li>
     *
     * </ul></li>
     *
     * <li>Acquire the <code>forwardPattern</code> property from the
     * <code>ControllerConfig</code> for the application module used to
     * process this request. If no pattern was configured, default to a
     * pattern of <code>$M$P</code>, which is compatible with the hard-coded
     * mapping behavior in Struts 1.0.</li>
     *
     * <li>Process the acquired <code>forwardPattern</code>, performing the
     * following substitutions:
     *
     * <ul>
     *
     * <li><strong>$M</strong> - Replaced by the module prefix for the
     * application module processing this request.</li>
     *
     * <li><strong>$P</strong> - Replaced by the <code>path</code> property of
     * the specified {@link ForwardConfig}, prepended with a slash if it does
     * not start with one.</li>
     *
     * <li><strong>$$</strong> - Replaced by a single dollar sign
     * character.</li>
     *
     * <li><strong>$x</strong> (where "x" is any charater not listed above) -
     * Silently omit these two characters from the result value.  (This has
     * the side effect of causing all other $+letter combinations to be
     * reserved.)</li>
     *
     * </ul></li>
     *
     * </ul>
     *
     * @param request The servlet request we are processing
     * @param forward ForwardConfig to be evaluated
     * @return context-relative URL
     * @since Struts 1.1
     */
    public static String forwardURL(HttpServletRequest request,
        ForwardConfig forward) {
        return forwardURL(request, forward, null);
    }

    /**
     * <p>Return the context-relative URL that corresponds to the specified
     * <code>ForwardConfig</code>. The URL is calculated based on the
     * properties of the {@link ForwardConfig} instance as follows:</p>
     *
     * <ul>
     *
     * <li>If the <code>contextRelative</code> property is set, it is assumed
     * that the <code>path</code> property contains a path that is already
     * context-relative: <ul>
     *
     * <li>If the <code>path</code> property value starts with a slash, it is
     * returned unmodified.</li> <li>If the <code>path</code> property value
     * does not start with a slash, a slash is prepended.</li>
     *
     * </ul></li>
     *
     * <li>Acquire the <code>forwardPattern</code> property from the
     * <code>ControllerConfig</code> for the application module used to
     * process this request. If no pattern was configured, default to a
     * pattern of <code>$M$P</code>, which is compatible with the hard-coded
     * mapping behavior in Struts 1.0.</li>
     *
     * <li>Process the acquired <code>forwardPattern</code>, performing the
     * following substitutions: <ul> <li><strong>$M</strong> - Replaced by the
     * module prefix for the application module processing this request.</li>
     *
     * <li><strong>$P</strong> - Replaced by the <code>path</code> property of
     * the specified {@link ForwardConfig}, prepended with a slash if it does
     * not start with one.</li>
     *
     * <li><strong>$$</strong> - Replaced by a single dollar sign
     * character.</li>
     *
     * <li><strong>$x</strong> (where "x" is any charater not listed above) -
     * Silently omit these two characters from the result value.  (This has
     * the side effect of causing all other $+letter combinations to be
     * reserved.)</li>
     *
     * </ul></li></ul>
     *
     * @param request      The servlet request we are processing
     * @param forward      ForwardConfig to be evaluated
     * @param moduleConfig Base forward on this module config.
     * @return context-relative URL
     * @since Struts 1.2
     */
    public static String forwardURL(HttpServletRequest request,
        ForwardConfig forward, ModuleConfig moduleConfig) {
        //load the current moduleConfig, if null
        if (moduleConfig == null) {
            moduleConfig = ModuleUtils.getInstance().getModuleConfig(request);
        }

        String path = forward.getPath();

        //load default prefix
        String prefix = moduleConfig.getPrefix();

        //override prefix if supplied by forward
        if (forward.getModule() != null) {
            prefix = forward.getModule();

            if ("/".equals(prefix)) {
                prefix = "";
            }
        }

        StringBuffer sb = new StringBuffer();

        // Calculate a context relative path for this ForwardConfig
        String forwardPattern =
            moduleConfig.getControllerConfig().getForwardPattern();

        if (forwardPattern == null) {
            // Performance optimization for previous default behavior
            sb.append(prefix);

            // smoothly insert a '/' if needed
            if (!path.startsWith("/")) {
                sb.append("/");
            }

            sb.append(path);
        } else {
            boolean dollar = false;

            for (int i = 0; i < forwardPattern.length(); i++) {
                char ch = forwardPattern.charAt(i);

                if (dollar) {
                    switch (ch) {
                    case 'M':
                        sb.append(prefix);

                        break;

                    case 'P':

                        // add '/' if needed
                        if (!path.startsWith("/")) {
                            sb.append("/");
                        }

                        sb.append(path);

                        break;

                    case '$':
                        sb.append('$');

                        break;

                    default:
                        ; // Silently swallow
                    }

                    dollar = false;

                    continue;
                } else if (ch == '$') {
                    dollar = true;
                } else {
                    sb.append(ch);
                }
            }
        }

        return (sb.toString());
    }

    /**
     * <p>Return the URL representing the current request. This is equivalent
     * to <code>HttpServletRequest.getRequestURL</code> in Servlet 2.3.</p>
     *
     * @param request The servlet request we are processing
     * @return URL representing the current request
     * @throws MalformedURLException if a URL cannot be created
     */
    public static URL requestURL(HttpServletRequest request)
        throws MalformedURLException {
        StringBuffer url = requestToServerUriStringBuffer(request);

        return (new URL(url.toString()));
    }

    /**
     * <p>Return the URL representing the scheme, server, and port number of
     * the current request. Server-relative URLs can be created by simply
     * appending the server-relative path (starting with '/') to this.</p>
     *
     * @param request The servlet request we are processing
     * @return URL representing the scheme, server, and port number of the
     *         current request
     * @throws MalformedURLException if a URL cannot be created
     */
    public static URL serverURL(HttpServletRequest request)
        throws MalformedURLException {
        StringBuffer url = requestToServerStringBuffer(request);

        return (new URL(url.toString()));
    }

    /**
     * <p>Return the string representing the scheme, server, and port number
     * of the current request. Server-relative URLs can be created by simply
     * appending the server-relative path (starting with '/') to this.</p>
     *
     * @param request The servlet request we are processing
     * @return URL representing the scheme, server, and port number of the
     *         current request
     * @since Struts 1.2.0
     */
    public static StringBuffer requestToServerUriStringBuffer(
        HttpServletRequest request) {
        StringBuffer serverUri =
            createServerUriStringBuffer(request.getScheme(),
                request.getServerName(), request.getServerPort(),
                request.getRequestURI());

        return serverUri;
    }

    /**
     * <p>Return <code>StringBuffer</code> representing the scheme, server,
     * and port number of the current request. Server-relative URLs can be
     * created by simply appending the server-relative path (starting with
     * '/') to this.</p>
     *
     * @param request The servlet request we are processing
     * @return URL representing the scheme, server, and port number of the
     *         current request
     * @since Struts 1.2.0
     */
    public static StringBuffer requestToServerStringBuffer(
        HttpServletRequest request) {
        return createServerStringBuffer(request.getScheme(),
            request.getServerName(), request.getServerPort());
    }

    /**
     * <p>Return <code>StringBuffer</code> representing the scheme, server,
     * and port number of the current request.</p>
     *
     * @param scheme The scheme name to use
     * @param server The server name to use
     * @param port   The port value to use
     * @return StringBuffer in the form scheme: server: port
     * @since Struts 1.2.0
     */
    public static StringBuffer createServerStringBuffer(String scheme,
        String server, int port) {
        StringBuffer url = new StringBuffer();

        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }

        url.append(scheme);
        url.append("://");
        url.append(server);

        if ((scheme.equals("http") && (port != 80))
            || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }

        return url;
    }

    /**
     * <p>Return <code>StringBuffer</code> representing the scheme, server,
     * and port number of the current request.</p>
     *
     * @param scheme The scheme name to use
     * @param server The server name to use
     * @param port   The port value to use
     * @param uri    The uri value to use
     * @return StringBuffer in the form scheme: server: port
     * @since Struts 1.2.0
     */
    public static StringBuffer createServerUriStringBuffer(String scheme,
        String server, int port, String uri) {
        StringBuffer serverUri = createServerStringBuffer(scheme, server, port);

        serverUri.append(uri);

        return serverUri;
    }

    /**
     * <p>Returns the true path of the destination action if the specified forward
     * is an action-aliased URL. This method version forms the URL based on
     * the current request; selecting the current module if the forward does not
     * explicitly contain a module path.</p>
     *
     * @param forward the forward config
     * @param request the current request
     * @param servlet the servlet handling the current request
     * @return the context-relative URL of the action if the forward has an action identifier; otherwise <code>null</code>.
     * @since Struts 1.3.6
     */
    public static String actionIdURL(ForwardConfig forward, HttpServletRequest request, ActionServlet servlet) {
        ModuleConfig moduleConfig = null;
        if (forward.getModule() != null) {
            String prefix = forward.getModule();
            moduleConfig = ModuleUtils.getInstance().getModuleConfig(prefix, servlet.getServletContext());
        } else {
            moduleConfig = ModuleUtils.getInstance().getModuleConfig(request);
        }
        return actionIdURL(forward.getPath(), moduleConfig, servlet);
    }

    /**
     * <p>Returns the true path of the destination action if the specified forward
     * is an action-aliased URL. This method version forms the URL based on
     * the specified module.
     *
     * @param originalPath the action-aliased path
     * @param moduleConfig the module config for this request
     * @param servlet the servlet handling the current request
     * @return the context-relative URL of the action if the path has an action identifier; otherwise <code>null</code>.
     * @since Struts 1.3.6
     */
    public static String actionIdURL(String originalPath, ModuleConfig moduleConfig, ActionServlet servlet) {
        if (originalPath.startsWith("http") || originalPath.startsWith("/")) {
            return null;
        }

        // Split the forward path into the resource and query string;
        // it is possible a forward (or redirect) has added parameters.
        String actionId = null;
        String qs = null;
        int qpos = originalPath.indexOf("?");
        if (qpos == -1) {
            actionId = originalPath;
        } else {
            actionId = originalPath.substring(0, qpos);
            qs = originalPath.substring(qpos);
        }

        // Find the action of the given actionId
        ActionConfig actionConfig = moduleConfig.findActionConfigId(actionId);
        if (actionConfig == null) {
            if (log.isDebugEnabled()) {
                log.debug("No actionId found for " + actionId);
            }
            return null;
        }

        String path = actionConfig.getPath();
        String mapping = RequestUtils.getServletMapping(servlet);
        StringBuffer actionIdPath = new StringBuffer();

        // Form the path based on the servlet mapping pattern
        if (mapping.startsWith("*")) {
            actionIdPath.append(path);
            actionIdPath.append(mapping.substring(1));
        } else if (mapping.startsWith("/")) {  // implied ends with a *
            mapping = mapping.substring(0, mapping.length() - 1);
            if (mapping.endsWith("/") && path.startsWith("/")) {
                actionIdPath.append(mapping);
                actionIdPath.append(path.substring(1));
            } else {
                actionIdPath.append(mapping);
                actionIdPath.append(path);
            }
        } else {
            log.warn("Unknown servlet mapping pattern");
            actionIdPath.append(path);
        }

        // Lastly add any query parameters (the ? is part of the query string)
        if (qs != null) {
            actionIdPath.append(qs);
        }

        // Return the path
        if (log.isDebugEnabled()) {
            log.debug(originalPath + " unaliased to " + actionIdPath.toString());
        }
        return actionIdPath.toString();
    }
}

/*
 * $Id: RequestProcessor.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * <p><strong>RequestProcessor</strong> contains the processing logic that the
 * {@link ActionServlet} performs as it receives each servlet request from the
 * container. You can customize the request processing behavior by subclassing
 * this class and overriding the method(s) whose behavior you are interested
 * in changing.</p>
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 * @since Struts 1.1
 */
public class RequestProcessor {
    // ----------------------------------------------------- Manifest Constants

    /**
     * <p>The request attribute under which the path information is stored for
     * processing during a <code>RequestDispatcher.include</code> call.</p>
     */
    public static final String INCLUDE_PATH_INFO =
        "javax.servlet.include.path_info";

    /**
     * <p>The request attribute under which the servlet path information is
     * stored for processing during a <code>RequestDispatcher.include</code>
     * call.</p>
     */
    public static final String INCLUDE_SERVLET_PATH =
        "javax.servlet.include.servlet_path";

    /**
     * <p>Commons Logging instance.</p>
     */
    protected static Log log = LogFactory.getLog(RequestProcessor.class);

    // ----------------------------------------------------- Instance Variables

    /**
     * <p>The set of <code>Action</code> instances that have been created and
     * initialized, keyed by the fully qualified Java class name of the
     * <code>Action</code> class.</p>
     */
    protected HashMap actions = new HashMap();

    /**
     * <p>The <code>ModuleConfiguration</code> with which we are
     * associated.</p>
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * <p>The servlet with which we are associated.</p>
     */
    protected ActionServlet servlet = null;

    // --------------------------------------------------------- Public Methods

    /**
     * <p>Clean up in preparation for a shutdown of this application.</p>
     */
    public void destroy() {
        synchronized (this.actions) {
            Iterator actions = this.actions.values().iterator();

            while (actions.hasNext()) {
                Action action = (Action) actions.next();

                action.setServlet(null);
            }

            this.actions.clear();
        }

        this.servlet = null;
    }

    /**
     * <p>Initialize this request processor instance.</p>
     *
     * @param servlet      The ActionServlet we are associated with
     * @param moduleConfig The ModuleConfig we are associated with.
     * @throws ServletException If an error occor during initialization
     */
    public void init(ActionServlet servlet, ModuleConfig moduleConfig)
        throws ServletException {
        synchronized (actions) {
            actions.clear();
        }

        this.servlet = servlet;
        this.moduleConfig = moduleConfig;
    }

    /**
     * <p>Process an <code>HttpServletRequest</code> and create the
     * corresponding <code>HttpServletResponse</code> or dispatch to another
     * resource.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a processing exception occurs
     */
    public void process(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        // Wrap multipart requests with a special wrapper
        request = processMultipart(request);

        // Identify the path component we will use to select a mapping
        String path = processPath(request, response);

        if (path == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing a '" + request.getMethod() + "' for path '"
                + path + "'");
        }

        // Select a Locale for the current user if requested
        processLocale(request, response);

        // Set the content type and no-caching headers if requested
        processContent(request, response);
        processNoCache(request, response);

        // General purpose preprocessing hook
        if (!processPreprocess(request, response)) {
            return;
        }

        this.processCachedMessages(request, response);

        // Identify the mapping for this request
        ActionMapping mapping = processMapping(request, response, path);

        if (mapping == null) {
            return;
        }

        // Check for any role required to perform this action
        if (!processRoles(request, response, mapping)) {
            return;
        }

        // Process any ActionForm bean related to this request
        ActionForm form = processActionForm(request, response, mapping);

        processPopulate(request, response, form, mapping);

        // Validate any fields of the ActionForm bean, if applicable
        try {
            if (!processValidate(request, response, form, mapping)) {
                return;
            }
        } catch (InvalidCancelException e) {
            ActionForward forward = processException(request, response, e, form, mapping);
            processForwardConfig(request, response, forward);
            return;
        } catch (IOException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        }

        // Process a forward or include specified by this mapping
        if (!processForward(request, response, mapping)) {
            return;
        }

        if (!processInclude(request, response, mapping)) {
            return;
        }

        // Create or acquire the Action instance to process this request
        Action action = processActionCreate(request, response, mapping);

        if (action == null) {
            return;
        }

        // Call the Action instance itself
        ActionForward forward =
            processActionPerform(request, response, action, form, mapping);

        // Process the returned ActionForward instance
        processForwardConfig(request, response, forward);
    }

    // ----------------------------------------------------- Processing Methods

    /**
     * <p>Return an <code>Action</code> instance that will be used to process
     * the current request, creating a new one if necessary.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param mapping  The mapping we are using
     * @return An <code>Action</code> instance that will be used to process
     *         the current request.
     * @throws IOException if an input/output error occurs
     */
    protected Action processActionCreate(HttpServletRequest request,
        HttpServletResponse response, ActionMapping mapping)
        throws IOException {
        // Acquire the Action instance we will be using (if there is one)
        String className = mapping.getType();

        if (log.isDebugEnabled()) {
            log.debug(" Looking for Action instance for class " + className);
        }

        // If there were a mapping property indicating whether
        // an Action were a singleton or not ([true]),
        // could we just instantiate and return a new instance here?
        Action instance;

        synchronized (actions) {
            // Return any existing Action instance of this class
            instance = (Action) actions.get(className);

            if (instance != null) {
                if (log.isTraceEnabled()) {
                    log.trace("  Returning existing Action instance");
                }

                return (instance);
            }

            // Create and return a new Action instance
            if (log.isTraceEnabled()) {
                log.trace("  Creating new Action instance");
            }

            try {
                instance = (Action) RequestUtils.applicationInstance(className);

                // Maybe we should propagate this exception
                // instead of returning null.
            } catch (Exception e) {
                log.error(getInternal().getMessage("actionCreate",
                        mapping.getPath()), e);

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    getInternal().getMessage("actionCreate", mapping.getPath()));

                return (null);
            }

            actions.put(className, instance);
        }

        if (instance.getServlet() == null) {
            instance.setServlet(this.servlet);
        }

        return (instance);
    }

    /**
     * <p>Retrieve and return the <code>ActionForm</code> associated with this
     * mapping, creating and retaining one if necessary. If there is no
     * <code>ActionForm</code> associated with this mapping, return
     * <code>null</code>.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param mapping  The mapping we are using
     * @return The <code>ActionForm</code> associated with this mapping.
     */
    protected ActionForm processActionForm(HttpServletRequest request,
        HttpServletResponse response, ActionMapping mapping) {
        // Create (if necessary) a form bean to use
        ActionForm instance =
            RequestUtils.createActionForm(request, mapping, moduleConfig,
                servlet);

        if (instance == null) {
            return (null);
        }

        // Store the new instance in the appropriate scope
        if (log.isDebugEnabled()) {
            log.debug(" Storing ActionForm bean instance in scope '"
                + mapping.getScope() + "' under attribute key '"
                + mapping.getAttribute() + "'");
        }

        if ("request".equals(mapping.getScope())) {
            request.setAttribute(mapping.getAttribute(), instance);
        } else {
            HttpSession session = request.getSession();

            session.setAttribute(mapping.getAttribute(), instance);
        }

        return (instance);
    }

    /**
     * <p>Forward or redirect to the specified destination, by the specified
     * mechanism.  This method uses a <code>ForwardConfig</code> object
     * instead an <code>ActionForward</code>.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param forward  The ForwardConfig controlling where we go next
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    protected void processForwardConfig(HttpServletRequest request,
        HttpServletResponse response, ForwardConfig forward)
        throws IOException, ServletException {
        if (forward == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("processForwardConfig(" + forward + ")");
        }

        String forwardPath = forward.getPath();
        String uri;

        // If the forward can be unaliased into an action, then use the path of the action
        String actionIdPath = RequestUtils.actionIdURL(forward, request, servlet);
        if (actionIdPath != null) {
            forwardPath = actionIdPath;
            ForwardConfig actionIdForward = new ForwardConfig(forward);
            actionIdForward.setPath(actionIdPath);
            forward = actionIdForward;
        }

        // paths not starting with / should be passed through without any
        // processing (ie. they're absolute)
        if (forwardPath.startsWith("/")) {
            // get module relative uri
            uri = RequestUtils.forwardURL(request, forward, null);
        } else {
            uri = forwardPath;
        }

        if (forward.getRedirect()) {
            // only prepend context path for relative uri
            if (uri.startsWith("/")) {
                uri = request.getContextPath() + uri;
            }

            response.sendRedirect(response.encodeRedirectURL(uri));
        } else {
            doForward(uri, request, response);
        }
    }

    // :FIXME: if Action.execute throws Exception, and Action.process has been
    // removed, should the process* methods still throw IOException,
    // ServletException?

    /**
     * <P>Ask the specified <code>Action</code> instance to handle this
     * request. Return the <code>ActionForward</code> instance (if any)
     * returned by the called <code>Action</code> for further processing.
     * </P>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param action   The Action instance to be used
     * @param form     The ActionForm instance to pass to this Action
     * @param mapping  The ActionMapping instance to pass to this Action
     * @return The <code>ActionForward</code> instance (if any) returned by
     *         the called <code>Action</code>.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    protected ActionForward processActionPerform(HttpServletRequest request,
        HttpServletResponse response, Action action, ActionForm form,
        ActionMapping mapping)
        throws IOException, ServletException {
        try {
            return (action.execute(mapping, form, request, response));
        } catch (Exception e) {
            return (processException(request, response, e, form, mapping));
        }
    }

    /**
     * <p>Removes any <code>ActionMessages</code> object stored in the session
     * under <code>Globals.MESSAGE_KEY</code> and <code>Globals.ERROR_KEY</code>
     * if the messages' <code>isAccessed</code> method returns true.  This
     * allows messages to be stored in the session, display one time, and be
     * released here.</p>
     *
     * @param request  The servlet request we are processing.
     * @param response The servlet response we are creating.
     * @since Struts 1.2
     */
    protected void processCachedMessages(HttpServletRequest request,
        HttpServletResponse response) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        // Remove messages as needed
        ActionMessages messages =
            (ActionMessages) session.getAttribute(Globals.MESSAGE_KEY);

        if (messages != null) {
            if (messages.isAccessed()) {
                session.removeAttribute(Globals.MESSAGE_KEY);
            }
        }

        // Remove error messages as needed
        messages = (ActionMessages) session.getAttribute(Globals.ERROR_KEY);

        if (messages != null) {
            if (messages.isAccessed()) {
                session.removeAttribute(Globals.ERROR_KEY);
            }
        }
    }

    /**
     * <p>Set the default content type (with optional character encoding) for
     * all responses if requested.  <strong>NOTE</strong> - This header will
     * be overridden automatically if a <code>RequestDispatcher.forward</code>
     * call is ultimately invoked.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     */
    protected void processContent(HttpServletRequest request,
        HttpServletResponse response) {
        String contentType =
            moduleConfig.getControllerConfig().getContentType();

        if (contentType != null) {
            response.setContentType(contentType);
        }
    }

    /**
     * <p>Ask our exception handler to handle the exception. Return the
     * <code>ActionForward</code> instance (if any) returned by the called
     * <code>ExceptionHandler</code>.</p>
     *
     * @param request   The servlet request we are processing
     * @param response  The servlet response we are processing
     * @param exception The exception being handled
     * @param form      The ActionForm we are processing
     * @param mapping   The ActionMapping we are using
     * @return The <code>ActionForward</code> instance (if any) returned by
     *         the called <code>ExceptionHandler</code>.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    protected ActionForward processException(HttpServletRequest request,
        HttpServletResponse response, Exception exception, ActionForm form,
        ActionMapping mapping)
        throws IOException, ServletException {
        // Is there a defined handler for this exception?
        ExceptionConfig config = mapping.findException(exception.getClass());

        if (config == null) {
            log.warn(getInternal().getMessage("unhandledException",
                    exception.getClass()));

            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else if (exception instanceof ServletException) {
                throw (ServletException) exception;
            } else {
                throw new ServletException(exception);
            }
        }

        // Use the configured exception handling
        try {
            ExceptionHandler handler =
                (ExceptionHandler) RequestUtils.applicationInstance(config
                    .getHandler());

            return (handler.execute(exception, config, mapping, form, request,
                response));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * <p>Process a forward requested by this mapping (if any). Return
     * <code>true</code> if standard processing should continue, or
     * <code>false</code> if we have already handled this request.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param mapping  The ActionMapping we are using
     * @return <code>true</code> to continue normal processing;
     *         <code>false</code> if a response has been created.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    protected boolean processForward(HttpServletRequest request,
        HttpServletResponse response, ActionMapping mapping)
        throws IOException, ServletException {
        // Are we going to processing this request?
        String forward = mapping.getForward();

        if (forward == null) {
            return (true);
        }

        // If the forward can be unaliased into an action, then use the path of the action
        String actionIdPath = RequestUtils.actionIdURL(forward, this.moduleConfig, this.servlet);
        if (actionIdPath != null) {
            forward = actionIdPath;
        }

        internalModuleRelativeForward(forward, request, response);

        return (false);
    }

    /**
     * <p>Process an include requested by this mapping (if any). Return
     * <code>true</code> if standard processing should continue, or
     * <code>false</code> if we have already handled this request.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param mapping  The ActionMapping we are using
     * @return <code>true</code> to continue normal processing;
     *         <code>false</code> if a response has been created.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if thrown by invoked methods
     */
    protected boolean processInclude(HttpServletRequest request,
        HttpServletResponse response, ActionMapping mapping)
        throws IOException, ServletException {
        // Are we going to processing this request?
        String include = mapping.getInclude();

        if (include == null) {
            return (true);
        }

        // If the forward can be unaliased into an action, then use the path of the action
        String actionIdPath = RequestUtils.actionIdURL(include, this.moduleConfig, this.servlet);
        if (actionIdPath != null) {
            include = actionIdPath;
        }

        internalModuleRelativeInclude(include, request, response);

        return (false);
    }

    /**
     * <p>Automatically select a <code>Locale</code> for the current user, if
     * requested. <strong>NOTE</strong> - configuring Locale selection will
     * trigger the creation of a new <code>HttpSession</code> if
     * necessary.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     */
    protected void processLocale(HttpServletRequest request,
        HttpServletResponse response) {
        // Are we configured to select the Locale automatically?
        if (!moduleConfig.getControllerConfig().getLocale()) {
            return;
        }

        // Has a Locale already been selected?
        HttpSession session = request.getSession();

        if (session.getAttribute(Globals.LOCALE_KEY) != null) {
            return;
        }

        // Use the Locale returned by the servlet container (if any)
        Locale locale = request.getLocale();

        if (locale != null) {
            if (log.isDebugEnabled()) {
                log.debug(" Setting user locale '" + locale + "'");
            }

            session.setAttribute(Globals.LOCALE_KEY, locale);
        }
    }

    /**
     * <p>Select the mapping used to process the selection path for this
     * request. If no mapping can be identified, create an error response and
     * return <code>null</code>.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param path     The portion of the request URI for selecting a mapping
     * @return The mapping used to process the selection path for this
     *         request.
     * @throws IOException if an input/output error occurs
     */
    protected ActionMapping processMapping(HttpServletRequest request,
        HttpServletResponse response, String path)
        throws IOException {
        // Is there a mapping for this path?
        ActionMapping mapping =
            (ActionMapping) moduleConfig.findActionConfig(path);

        // If a mapping is found, put it in the request and return it
        if (mapping != null) {
            request.setAttribute(Globals.MAPPING_KEY, mapping);

            return (mapping);
        }

        // Locate the mapping for unknown paths (if any)
        ActionConfig[] configs = moduleConfig.findActionConfigs();

        for (int i = 0; i < configs.length; i++) {
            if (configs[i].getUnknown()) {
                mapping = (ActionMapping) configs[i];
                request.setAttribute(Globals.MAPPING_KEY, mapping);

                return (mapping);
            }
        }

        // No mapping can be found to process this request
        String msg = getInternal().getMessage("processInvalid");

        log.error(msg + " " + path);
        response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);

        return null;
    }

    /**
     * <p>If this is a multipart request, wrap it with a special wrapper.
     * Otherwise, return the request unchanged.</p>
     *
     * @param request The HttpServletRequest we are processing
     * @return A wrapped request, if the request is multipart; otherwise the
     *         original request.
     */
    protected HttpServletRequest processMultipart(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return (request);
        }

        String contentType = request.getContentType();

        if ((contentType != null)
            && contentType.startsWith("multipart/form-data")) {
            return (new MultipartRequestWrapper(request));
        } else {
            return (request);
        }
    }

    /**
     * <p>Set the no-cache headers for all responses, if requested.
     * <strong>NOTE</strong> - This header will be overridden automatically if
     * a <code>RequestDispatcher.forward</code> call is ultimately
     * invoked.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     */
    protected void processNoCache(HttpServletRequest request,
        HttpServletResponse response) {
        if (moduleConfig.getControllerConfig().getNocache()) {
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
            response.setDateHeader("Expires", 1);
        }
    }

    /**
     * <p>Identify and return the path component (from the request URI) that
     * we will use to select an <code>ActionMapping</code> with which to
     * dispatch. If no such path can be identified, create an error response
     * and return <code>null</code>.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @return The path that will be used to select an action mapping.
     * @throws IOException if an input/output error occurs
     */
    protected String processPath(HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {
        String path;

        // For prefix matching, match on the path info (if any)
        path = (String) request.getAttribute(INCLUDE_PATH_INFO);

        if (path == null) {
            path = request.getPathInfo();
        }

        if ((path != null) && (path.length() > 0)) {
            return (path);
        }

        // For extension matching, strip the module prefix and extension
        path = (String) request.getAttribute(INCLUDE_SERVLET_PATH);

        if (path == null) {
            path = request.getServletPath();
        }

        String prefix = moduleConfig.getPrefix();

        if (!path.startsWith(prefix)) {
            String msg = getInternal().getMessage("processPath");

            log.error(msg + " " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);

            return null;
        }

        path = path.substring(prefix.length());

        int slash = path.lastIndexOf("/");
        int period = path.lastIndexOf(".");

        if ((period >= 0) && (period > slash)) {
            path = path.substring(0, period);
        }

        return (path);
    }

    /**
     * <p>Populate the properties of the specified <code>ActionForm</code>
     * instance from the request parameters included with this request.  In
     * addition, request attribute <code>Globals.CANCEL_KEY</code> will be set
     * if the request was submitted with a button created by
     * <code>CancelTag</code>.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param form     The ActionForm instance we are populating
     * @param mapping  The ActionMapping we are using
     * @throws ServletException if thrown by RequestUtils.populate()
     */
    protected void processPopulate(HttpServletRequest request,
        HttpServletResponse response, ActionForm form, ActionMapping mapping)
        throws ServletException {
        if (form == null) {
            return;
        }

        // Populate the bean properties of this ActionForm instance
        if (log.isDebugEnabled()) {
            log.debug(" Populating bean properties from this request");
        }

        form.setServlet(this.servlet);
        form.reset(mapping, request);

        if (mapping.getMultipartClass() != null) {
            request.setAttribute(Globals.MULTIPART_KEY,
                mapping.getMultipartClass());
        }

        RequestUtils.populate(form, mapping.getPrefix(), mapping.getSuffix(),
            request);

        // Set the cancellation request attribute if appropriate
        if ((request.getParameter(Globals.CANCEL_PROPERTY) != null)
            || (request.getParameter(Globals.CANCEL_PROPERTY_X) != null)) {
            request.setAttribute(Globals.CANCEL_KEY, Boolean.TRUE);
        }
    }

    /**
     * <p>General-purpose preprocessing hook that can be overridden as
     * required by subclasses. Return <code>true</code> if you want standard
     * processing to continue, or <code>false</code> if the response has
     * already been completed. The default implementation does nothing.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @return <code>true</code> to continue normal processing;
     *         <code>false</code> if a response has been created.
     */
    protected boolean processPreprocess(HttpServletRequest request,
        HttpServletResponse response) {
        return (true);
    }

    /**
     * <p>If this action is protected by security roles, make sure that the
     * current user possesses at least one of them.  Return <code>true</code>
     * to continue normal processing, or <code>false</code> if an appropriate
     * response has been created and processing should terminate.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param mapping  The mapping we are using
     * @return <code>true</code> to continue normal processing;
     *         <code>false</code> if a response has been created.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    protected boolean processRoles(HttpServletRequest request,
        HttpServletResponse response, ActionMapping mapping)
        throws IOException, ServletException {
        // Is this action protected by role requirements?
        String[] roles = mapping.getRoleNames();

        if ((roles == null) || (roles.length < 1)) {
            return (true);
        }

        // Check the current user against the list of required roles
        for (int i = 0; i < roles.length; i++) {
            if (request.isUserInRole(roles[i])) {
                if (log.isDebugEnabled()) {
                    log.debug(" User '" + request.getRemoteUser()
                        + "' has role '" + roles[i] + "', granting access");
                }

                return (true);
            }
        }

        // The current user is not authorized for this action
        if (log.isDebugEnabled()) {
            log.debug(" User '" + request.getRemoteUser()
                + "' does not have any required role, denying access");
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            getInternal().getMessage("notAuthorized", mapping.getPath()));

        return (false);
    }

    /**
     * <p>If this request was not cancelled, and the request's {@link
     * ActionMapping} has not disabled validation, call the
     * <code>validate</code> method of the specified {@link ActionForm}, and
     * forward to the input path if there were any errors. Return
     * <code>true</code> if we should continue processing, or
     * <code>false</code> if we have already forwarded control back to the
     * input form.</p>
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param form     The ActionForm instance we are populating
     * @param mapping  The ActionMapping we are using
     * @return <code>true</code> to continue normal processing;
     *         <code>false</code> if a response has been created.
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @throws InvalidCancelException if a cancellation is attempted
     *         without the proper action configuration.
     */
    protected boolean processValidate(HttpServletRequest request,
        HttpServletResponse response, ActionForm form, ActionMapping mapping)
        throws IOException, ServletException, InvalidCancelException {
        if (form == null) {
            return (true);
        }

        // Has validation been turned off for this mapping?
        if (!mapping.getValidate()) {
            return (true);
        }

        // Was this request cancelled? If it has been, the mapping also
        // needs to state whether the cancellation is permissable; otherwise
        // the cancellation is considered to be a symptom of a programmer
        // error or a spoof.
        if (request.getAttribute(Globals.CANCEL_KEY) != null) {
            if (mapping.getCancellable()) {
                if (log.isDebugEnabled()) {
                    log.debug(" Cancelled transaction, skipping validation");
                }
                return (true);
            } else {
                request.removeAttribute(Globals.CANCEL_KEY);
                throw new InvalidCancelException();
            }
        }

        // Call the form bean's validation method
        if (log.isDebugEnabled()) {
            log.debug(" Validating input form properties");
        }

        ActionMessages errors = form.validate(mapping, request);

        if ((errors == null) || errors.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("  No errors detected, accepting input");
            }

            return (true);
        }

        // Special handling for multipart request
        if (form.getMultipartRequestHandler() != null) {
            if (log.isTraceEnabled()) {
                log.trace("  Rolling back multipart request");
            }

            form.getMultipartRequestHandler().rollback();
        }

        // Was an input path (or forward) specified for this mapping?
        String input = mapping.getInput();

        if (input == null) {
            if (log.isTraceEnabled()) {
                log.trace("  Validation failed but no input form available");
            }

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                getInternal().getMessage("noInput", mapping.getPath()));

            return (false);
        }

        // Save our error messages and return to the input form if possible
        if (log.isDebugEnabled()) {
            log.debug(" Validation failed, returning to '" + input + "'");
        }

        request.setAttribute(Globals.ERROR_KEY, errors);

        if (moduleConfig.getControllerConfig().getInputForward()) {
            ForwardConfig forward = mapping.findForward(input);

            processForwardConfig(request, response, forward);
        } else {
            internalModuleRelativeForward(input, request, response);
        }

        return (false);
    }

    /**
     * <p>Do a module relative forward to specified URI using request
     * dispatcher. URI is relative to the current module. The real URI is
     * compute by prefixing the module name.</p> <p>This method is used
     * internally and is not part of the public API. It is advised to not use
     * it in subclasses. </p>
     *
     * @param uri      Module-relative URI to forward to
     * @param request  Current page request
     * @param response Current page response
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @since Struts 1.1
     */
    protected void internalModuleRelativeForward(String uri,
        HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        // Construct a request dispatcher for the specified path
        uri = moduleConfig.getPrefix() + uri;

        // Delegate the processing of this request
        // :FIXME: - exception handling?
        if (log.isDebugEnabled()) {
            log.debug(" Delegating via forward to '" + uri + "'");
        }

        doForward(uri, request, response);
    }

    /**
     * <p>Do a module relative include to specified URI using request
     * dispatcher. URI is relative to the current module. The real URI is
     * compute by prefixing the module name.</p> <p>This method is used
     * internally and is not part of the public API. It is advised to not use
     * it in subclasses.</p>
     *
     * @param uri      Module-relative URI to include
     * @param request  Current page request
     * @param response Current page response
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @since Struts 1.1
     */
    protected void internalModuleRelativeInclude(String uri,
        HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        // Construct a request dispatcher for the specified path
        uri = moduleConfig.getPrefix() + uri;

        // Delegate the processing of this request
        // FIXME - exception handling?
        if (log.isDebugEnabled()) {
            log.debug(" Delegating via include to '" + uri + "'");
        }

        doInclude(uri, request, response);
    }

    /**
     * <p>Do a forward to specified URI using a <code>RequestDispatcher</code>.
     * This method is used by all internal method needing to do a
     * forward.</p>
     *
     * @param uri      Context-relative URI to forward to
     * @param request  Current page request
     * @param response Current page response
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @since Struts 1.1
     */
    protected void doForward(String uri, HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {
        RequestDispatcher rd = getServletContext().getRequestDispatcher(uri);

        if (rd == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                getInternal().getMessage("requestDispatcher", uri));

            return;
        }

        rd.forward(request, response);
    }

    /**
     * <p>Do an include of specified URI using a <code>RequestDispatcher</code>.
     * This method is used by all internal method needing to do an
     * include.</p>
     *
     * @param uri      Context-relative URI to include
     * @param request  Current page request
     * @param response Current page response
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     * @since Struts 1.1
     */
    protected void doInclude(String uri, HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {
        RequestDispatcher rd = getServletContext().getRequestDispatcher(uri);

        if (rd == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                getInternal().getMessage("requestDispatcher", uri));

            return;
        }

        rd.include(request, response);
    }

    // -------------------------------------------------------- Support Methods

    /**
     * <p>Return the <code>MessageResources</code> instance containing our
     * internal message strings.</p>
     *
     * @return The <code>MessageResources</code> instance containing our
     *         internal message strings.
     */
    protected MessageResources getInternal() {
        return (servlet.getInternal());
    }

    /**
     * <p>Return the <code>ServletContext</code> for the web application in
     * which we are running.</p>
     *
     * @return The <code>ServletContext</code> for the web application.
     */
    protected ServletContext getServletContext() {
        return (servlet.getServletContext());
    }
}

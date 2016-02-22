/*
 * $Id: ExceptionHandler.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.ModuleException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <p>An <strong>ExceptionHandler</strong> is configured in the Struts
 * configuration file to handle a specific type of exception thrown by an
 * <code>Action.execute</code> method.</p>
 *
 * @since Struts 1.1
 */
public class ExceptionHandler {
    /**
     * <p>The name of a configuration property which can be set to specify an
     * alternative path which should be used when the HttpServletResponse has
     * already been committed.</p> <p>To use this, in your
     * <code>struts-config.xml</code> specify the exception handler like
     * this:
     * <pre>
     *   &lt;exception
     *       key="GlobalExceptionHandler.default"
     *       type="java.lang.Exception"
     *       path="/ErrorPage.jsp"&gt;
     *       &lt;set-property key="INCLUDE_PATH" value="/error.jsp" /&gt;
     *   &lt;/exception&gt;
     *  </pre>
     * </p> <p>You would want to use this when your normal ExceptionHandler
     * path is a Tiles definition or otherwise unsuitable for use in an
     * <code>include</code> context.  If you do not use this, and you do not
     * specify "SILENT_IF_COMMITTED" then the ExceptionHandler will attempt to
     * forward to the same path which would be used in normal circumstances,
     * specified using the "path" attribute in the &lt;exception&gt;
     * element.</p>
     *
     * @since Struts 1.3
     */
    public static final String INCLUDE_PATH = "INCLUDE_PATH";

    /**
     * <p>The name of a configuration property which indicates that Struts
     * should do nothing if the response has already been committed.  This
     * suppresses the default behavior, which is to use an "include" rather
     * than a "forward" in this case in hopes of providing some meaningful
     * information to the browser.</p> <p>To use this, in your
     * <code>struts-config.xml</code> specify the exception handler like
     * this:
     * <pre>
     *   &lt;exception
     *       key="GlobalExceptionHandler.default"
     *       type="java.lang.Exception"
     *       path="/ErrorPage.jsp"&gt;
     *       &lt;set-property key="SILENT_IF_COMMITTED" value="true" /&gt;
     *   &lt;/exception&gt;
     *  </pre>
     * To be effective, this value must be defined to the literal String
     * "true". If it is not defined or defined to any other value, the default
     * behavior will be used. </p> <p>You only need to use this if you do not
     * want error information displayed in the browser when Struts intercepts
     * an exception after the response has been committed.</p>
     *
     * @since Struts 1.3
     */
    public static final String SILENT_IF_COMMITTED = "SILENT_IF_COMMITTED";

    /**
     * <p>Commons logging instance.</p>
     */
    private static final Log LOG = LogFactory.getLog(ExceptionHandler.class);

    /**
     * <p>The message resources for this package.</p>
     */
    private static MessageResources messages =
        MessageResources.getMessageResources(
            "org.apache.struts.action.LocalStrings");

    /**
     * <p> Handle the Exception. Return the ActionForward instance (if any)
     * returned by the called ExceptionHandler. </p>
     *
     * @param ex           The exception to handle
     * @param ae           The ExceptionConfig corresponding to the exception
     * @param mapping      The ActionMapping we are processing
     * @param formInstance The ActionForm we are processing
     * @param request      The servlet request we are processing
     * @param response     The servlet response we are creating
     * @return The <code>ActionForward</code> instance (if any) returned by
     *         the called <code>ExceptionHandler</code>.
     * @throws ServletException if a servlet exception occurs
     * @since Struts 1.1
     */
    public ActionForward execute(Exception ex, ExceptionConfig ae,
        ActionMapping mapping, ActionForm formInstance,
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        LOG.debug("ExceptionHandler executing for exception " + ex);

        ActionForward forward;
        ActionMessage error;
        String property;

        // Build the forward from the exception mapping if it exists
        // or from the form input
        if (ae.getPath() != null) {
            forward = new ActionForward(ae.getPath());
        } else {
            forward = mapping.getInputForward();
        }

        // Figure out the error
        if (ex instanceof ModuleException) {
            error = ((ModuleException) ex).getActionMessage();
            property = ((ModuleException) ex).getProperty();
        } else {
            error = new ActionMessage(ae.getKey(), ex.getMessage());
            property = error.getKey();
        }

        this.logException(ex);

        // Store the exception
        request.setAttribute(Globals.EXCEPTION_KEY, ex);
        this.storeException(request, property, error, forward, ae.getScope());

        if (!response.isCommitted()) {
            return forward;
        }

        LOG.debug("Response is already committed, so forwarding will not work."
            + " Attempt alternate handling.");

        if (!silent(ae)) {
            handleCommittedResponse(ex, ae, mapping, formInstance, request,
                response, forward);
        } else {
            LOG.warn("ExceptionHandler configured with " + SILENT_IF_COMMITTED
                + " and response is committed.", ex);
        }

        return null;
    }

    /**
     * <p>Attempt to give good information when the response has already been
     * committed when the exception was thrown. This happens often when Tiles
     * is used. Base implementation will see if the INCLUDE_PATH property has
     * been set, or if not, it will attempt to use the same path to which
     * control would have been forwarded.</p>
     *
     * @param ex            The exception to handle
     * @param config        The ExceptionConfig we are processing
     * @param mapping       The ActionMapping we are processing
     * @param formInstance  The ActionForm we are processing
     * @param request       The servlet request we are processing
     * @param response      The servlet response we are creating
     * @param actionForward The ActionForward we are processing
     * @since Struts 1.3
     */
    protected void handleCommittedResponse(Exception ex,
        ExceptionConfig config, ActionMapping mapping, ActionForm formInstance,
        HttpServletRequest request, HttpServletResponse response,
        ActionForward actionForward) {
        String includePath = determineIncludePath(config, actionForward);

        if (includePath != null) {
            if (includePath.startsWith("/")) {
                LOG.debug("response committed, "
                    + "but attempt to include results "
                    + "of actionForward path");

                RequestDispatcher requestDispatcher =
                    request.getRequestDispatcher(includePath);

                try {
                    requestDispatcher.include(request, response);

                    return;
                } catch (IOException e) {
                    LOG.error("IOException when trying to include "
                        + "the error page path " + includePath, e);
                } catch (ServletException e) {
                    LOG.error("ServletException when trying to include "
                        + "the error page path " + includePath, e);
                }
            } else {
                LOG.warn("Suspicious includePath doesn't seem likely to work, "
                    + "so skipping it: " + includePath
                    + "; expected path to start with '/'");
            }
        }

        LOG.debug("Include not available or failed; "
            + "try writing to the response directly.");

        try {
            response.getWriter().println("Unexpected error: " + ex);
            response.getWriter().println("<!-- ");
            ex.printStackTrace(response.getWriter());
            response.getWriter().println("-->");
        } catch (IOException e) {
            LOG.error("Error giving minimal information about exception", e);
            LOG.error("Original exception: ", ex);
        }
    }

    /**
     * <p>Return a path to which an include should be attempted in the case
     * when the response was committed before the <code>ExceptionHandler</code>
     * was invoked.  </p> <p>If the <code>ExceptionConfig</code> has the
     * property <code>INCLUDE_PATH</code> defined, then the value of that
     * property will be returned. Otherwise, the ActionForward path is
     * returned. </p>
     *
     * @param config        Configuration element
     * @param actionForward Forward to use on error
     * @return Path of resource to include
     * @since Struts 1.3
     */
    protected String determineIncludePath(ExceptionConfig config,
        ActionForward actionForward) {
        String includePath = config.getProperty("INCLUDE_PATH");

        if (includePath == null) {
            includePath = actionForward.getPath();
        }

        return includePath;
    }

    /**
     * <p>Logs the <code>Exception</code> using commons-logging.</p>
     *
     * @param e The Exception to LOG.
     * @since Struts 1.2
     */
    protected void logException(Exception e) {
        LOG.debug(messages.getMessage("exception.LOG"), e);
    }

    /**
     * <p>Default implementation for handling an <code>ActionMessage</code>
     * generated from an <code>Exception</code> during <code>Action</code>
     * delegation. The default implementation is to set an attribute of the
     * request or session, as defined by the scope provided (the scope from
     * the exception mapping). An <code>ActionMessages</code> instance is
     * created, the error is added to the collection and the collection is set
     * under the <code>Globals.ERROR_KEY</code>.</p>
     *
     * @param request  The request we are handling
     * @param property The property name to use for this error
     * @param error    The error generated from the exception mapping
     * @param forward  The forward generated from the input path (from the
     *                 form or exception mapping)
     * @param scope    The scope of the exception mapping.
     * @since Struts 1.2
     */
    protected void storeException(HttpServletRequest request, String property,
        ActionMessage error, ActionForward forward, String scope) {
        ActionMessages errors = new ActionMessages();

        errors.add(property, error);

        if ("request".equals(scope)) {
            request.setAttribute(Globals.ERROR_KEY, errors);
        } else {
            request.getSession().setAttribute(Globals.ERROR_KEY, errors);
        }
    }

    /**
     * <p>Indicate whether this Handler has been configured to be silent.  In
     * the base implementation, this is done by specifying the value
     * <code>"true"</code> for the property "SILENT_IF_COMMITTED" in the
     * ExceptionConfig.</p>
     *
     * @param config The ExceptionConfiguration we are handling
     * @return True if Handler is silent
     * @since Struts 1.3
     */
    private boolean silent(ExceptionConfig config) {
        return "true".equals(config.getProperty(SILENT_IF_COMMITTED));
    }
}

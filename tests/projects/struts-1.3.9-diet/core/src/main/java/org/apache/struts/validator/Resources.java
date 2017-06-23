/*
 * $Id: Resources.java 476419 2006-11-18 02:28:07Z niallp $
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
package org.apache.struts.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.Arg;
import org.apache.commons.validator.Field;
import org.apache.commons.validator.Msg;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.Var;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.ModuleUtils;
import org.apache.struts.util.RequestUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * This class helps provides some useful methods for retrieving objects from
 * different scopes of the application.
 *
 * @version $Rev: 476419 $ $Date: 2005-09-16 23:34:41 -0400 (Fri, 16 Sep 2005)
 *          $
 * @since Struts 1.1
 */
public class Resources {
    /**
     * The message resources for this package.
     */
    private static MessageResources sysmsgs =
        MessageResources.getMessageResources(
            "org.apache.struts.validator.LocalStrings");

    /**
     * <p>Commons Logging instance.</p>
     */
    private static Log log = LogFactory.getLog(Resources.class);

    /**
     * Resources key the <code>ServletContext</code> is stored under.
     */
    private static String SERVLET_CONTEXT_PARAM =
        "javax.servlet.ServletContext";

    /**
     * Resources key the <code>HttpServletRequest</code> is stored under.
     */
    private static String HTTP_SERVLET_REQUEST_PARAM =
        "javax.servlet.http.HttpServletRequest";

    /**
     * Resources key the <code>ActionMessages</code> is stored under.
     */
    private static String ACTION_MESSAGES_PARAM =
        "org.apache.struts.action.ActionMessages";

    /**
     * Retrieve <code>ValidatorResources</code> for the current module.
     *
     * @param application Application Context
     * @param request     The ServletRequest
     */
    public static ValidatorResources getValidatorResources(
        ServletContext application, HttpServletRequest request) {
        String prefix =
            ModuleUtils.getInstance().getModuleConfig(request, application)
                       .getPrefix();

        return (ValidatorResources) application.getAttribute(ValidatorPlugIn.VALIDATOR_KEY
            + prefix);
    }

    /**
     * Retrieve <code>MessageResources</code> for the module.
     *
     * @param request the servlet request
     */
    public static MessageResources getMessageResources(
        HttpServletRequest request) {
        return (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
    }

    /**
     * Retrieve <code>MessageResources</code> for the module and bundle.
     *
     * @param application the servlet context
     * @param request     the servlet request
     * @param bundle      the bundle key
     */
    public static MessageResources getMessageResources(
        ServletContext application, HttpServletRequest request, String bundle) {
        if (bundle == null) {
            bundle = Globals.MESSAGES_KEY;
        }

        MessageResources resources =
            (MessageResources) request.getAttribute(bundle);

        if (resources == null) {
            ModuleConfig moduleConfig =
                ModuleUtils.getInstance().getModuleConfig(request, application);

            resources =
                (MessageResources) application.getAttribute(bundle
                    + moduleConfig.getPrefix());
        }

        if (resources == null) {
            resources = (MessageResources) application.getAttribute(bundle);
        }

        if (resources == null) {
            throw new NullPointerException(
                "No message resources found for bundle: " + bundle);
        }

        return resources;
    }

    /**
     * Get the value of a variable.
     *
     * @param varName   The variable name
     * @param field     the validator Field
     * @param validator The Validator
     * @param request   the servlet request
     * @param required  Whether the variable is mandatory
     * @return The variable's value
     */
    public static String getVarValue(String varName, Field field,
        Validator validator, HttpServletRequest request, boolean required) {
        Var var = field.getVar(varName);

        if (var == null) {
            String msg = sysmsgs.getMessage("variable.missing", varName);

            if (required) {
                throw new IllegalArgumentException(msg);
            }

            if (log.isDebugEnabled()) {
                log.debug(field.getProperty() + ": " + msg);
            }

            return null;
        }

        ServletContext application =
            (ServletContext) validator.getParameterValue(SERVLET_CONTEXT_PARAM);

        return getVarValue(var, application, request, required);
    }

    /**
     * Get the value of a variable.
     *
     * @param var         the validator variable
     * @param application The ServletContext
     * @param request     the servlet request
     * @param required    Whether the variable is mandatory
     * @return The variables values
     */
    public static String getVarValue(Var var, ServletContext application,
        HttpServletRequest request, boolean required) {
        String varName = var.getName();
        String varValue = var.getValue();

        // Non-resource variable
        if (!var.isResource()) {
            return varValue;
        }

        // Get the message resources
        String bundle = var.getBundle();
        MessageResources messages =
            getMessageResources(application, request, bundle);

        // Retrieve variable's value from message resources
        Locale locale = RequestUtils.getUserLocale(request, null);
        String value = messages.getMessage(locale, varValue, null);

        // Not found in message resources
        if ((value == null) && required) {
            throw new IllegalArgumentException(sysmsgs.getMessage(
                    "variable.resource.notfound", varName, varValue, bundle));
        }

        if (log.isDebugEnabled()) {
            log.debug("Var=[" + varName + "], " + "bundle=[" + bundle + "], "
                + "key=[" + varValue + "], " + "value=[" + value + "]");
        }

        return value;
    }

    /**
     * Gets the <code>Locale</code> sensitive value based on the key passed
     * in.
     *
     * @param messages The Message resources
     * @param locale   The locale.
     * @param key      Key used to lookup the message
     */
    public static String getMessage(MessageResources messages, Locale locale,
        String key) {
        String message = null;

        if (messages != null) {
            message = messages.getMessage(locale, key);
        }

        return (message == null) ? "" : message;
    }

    /**
     * Gets the <code>Locale</code> sensitive value based on the key passed
     * in.
     *
     * @param request servlet request
     * @param key     the request key
     */
    public static String getMessage(HttpServletRequest request, String key) {
        MessageResources messages = getMessageResources(request);

        return getMessage(messages, RequestUtils.getUserLocale(request, null),
            key);
    }

    /**
     * Gets the locale sensitive message based on the <code>ValidatorAction</code>
     * message and the <code>Field</code>'s arg objects.
     *
     * @param messages The Message resources
     * @param locale   The locale
     * @param va       The Validator Action
     * @param field    The Validator Field
     */
    public static String getMessage(MessageResources messages, Locale locale,
        ValidatorAction va, Field field) {
        String[] args = getArgs(va.getName(), messages, locale, field);
        String msg =
            (field.getMsg(va.getName()) != null) ? field.getMsg(va.getName())
                                                 : va.getMsg();

        return messages.getMessage(locale, msg, args);
    }

    /**
     * Gets the <code>Locale</code> sensitive value based on the key passed
     * in.
     *
     * @param application     the servlet context
     * @param request         the servlet request
     * @param defaultMessages The default Message resources
     * @param locale          The locale
     * @param va              The Validator Action
     * @param field           The Validator Field
     */
    public static String getMessage(ServletContext application,
        HttpServletRequest request, MessageResources defaultMessages,
        Locale locale, ValidatorAction va, Field field) {
        Msg msg = field.getMessage(va.getName());

        if ((msg != null) && !msg.isResource()) {
            return msg.getKey();
        }

        String msgKey = null;
        String msgBundle = null;
        MessageResources messages = defaultMessages;

        if (msg == null) {
            msgKey = va.getMsg();
        } else {
            msgKey = msg.getKey();
            msgBundle = msg.getBundle();

            if (msg.getBundle() != null) {
                messages =
                    getMessageResources(application, request, msg.getBundle());
            }
        }

        if ((msgKey == null) || (msgKey.length() == 0)) {
            return "??? " + va.getName() + "." + field.getProperty() + " ???";
        }

        // Get the arguments
        Arg[] args = field.getArgs(va.getName());
        String[] argValues =
            getArgValues(application, request, messages, locale, args);

        // Return the message
        return messages.getMessage(locale, msgKey, argValues);
    }

    /**
     * Gets the <code>ActionMessage</code> based on the
     * <code>ValidatorAction</code> message and the <code>Field</code>'s arg
     * objects.
     * <p>
     * <strong>Note:</strong> this method does not respect bundle information
     * stored with the field's &lt;msg&gt; or &lt;arg&gt; elements, and localization
     * will not work for alternative resource bundles. This method is
     * deprecated for this reason, and you should use
     * {@link #getActionMessage(Validator,HttpServletRequest,ValidatorAction,Field)}
     * instead. 
     *
     * @param request the servlet request
     * @param va      Validator action
     * @param field   the validator Field
     * @deprecated Use getActionMessage(Validator, HttpServletRequest,
     *    ValidatorAction, Field) method instead
     */
    public static ActionMessage getActionMessage(HttpServletRequest request,
        ValidatorAction va, Field field) {
        String[] args =
            getArgs(va.getName(), getMessageResources(request),
                RequestUtils.getUserLocale(request, null), field);

        String msg =
            (field.getMsg(va.getName()) != null) ? field.getMsg(va.getName())
                                                 : va.getMsg();

        return new ActionMessage(msg, args);
    }

    /**
     * Gets the <code>ActionMessage</code> based on the
     * <code>ValidatorAction</code> message and the <code>Field</code>'s arg
     * objects.
     *
     * @param validator the Validator
     * @param request   the servlet request
     * @param va        Validator action
     * @param field     the validator Field
     */
    public static ActionMessage getActionMessage(Validator validator,
        HttpServletRequest request, ValidatorAction va, Field field) {
        Msg msg = field.getMessage(va.getName());

        if ((msg != null) && !msg.isResource()) {
            return new ActionMessage(msg.getKey(), false);
        }

        String msgKey = null;
        String msgBundle = null;

        if (msg == null) {
            msgKey = va.getMsg();
        } else {
            msgKey = msg.getKey();
            msgBundle = msg.getBundle();
        }

        if ((msgKey == null) || (msgKey.length() == 0)) {
            return new ActionMessage("??? " + va.getName() + "."
                + field.getProperty() + " ???", false);
        }

        ServletContext application =
            (ServletContext) validator.getParameterValue(SERVLET_CONTEXT_PARAM);
        MessageResources messages =
            getMessageResources(application, request, msgBundle);
        Locale locale = RequestUtils.getUserLocale(request, null);

        Arg[] args = field.getArgs(va.getName());
        String[] argValues =
            getArgValues(application, request, messages, locale, args);

        ActionMessage actionMessage = null;

        if (msgBundle == null) {
            actionMessage = new ActionMessage(msgKey, argValues);
        } else {
            String message = messages.getMessage(locale, msgKey, argValues);

            actionMessage = new ActionMessage(message, false);
        }

        return actionMessage;
    }

    /**
     * Gets the message arguments based on the current <code>ValidatorAction</code>
     * and <code>Field</code>.
     *
     * @param actionName action name
     * @param messages   message resources
     * @param locale     the locale
     * @param field      the validator field
     */
    public static String[] getArgs(String actionName,
        MessageResources messages, Locale locale, Field field) {
        String[] argMessages = new String[4];

        Arg[] args =
            new Arg[] {
                field.getArg(actionName, 0), field.getArg(actionName, 1),
                field.getArg(actionName, 2), field.getArg(actionName, 3)
            };

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }

            if (args[i].isResource()) {
                argMessages[i] = getMessage(messages, locale, args[i].getKey());
            } else {
                argMessages[i] = args[i].getKey();
            }
        }

        return argMessages;
    }

    /**
     * Gets the message arguments based on the current <code>ValidatorAction</code>
     * and <code>Field</code>.
     *
     * @param application     the servlet context
     * @param request         the servlet request
     * @param defaultMessages Default message resources
     * @param locale          the locale
     * @param args            The arguments for the message
     */
    private static String[] getArgValues(ServletContext application,
        HttpServletRequest request, MessageResources defaultMessages,
        Locale locale, Arg[] args) {
        if ((args == null) || (args.length == 0)) {
            return null;
        }

        String[] values = new String[args.length];

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (args[i].isResource()) {
                    MessageResources messages = defaultMessages;

                    if (args[i].getBundle() != null) {
                        messages =
                            getMessageResources(application, request,
                                args[i].getBundle());
                    }

                    values[i] = messages.getMessage(locale, args[i].getKey());
                } else {
                    values[i] = args[i].getKey();
                }
            }
        }

        return values;
    }

    /**
     * Initialize the <code>Validator</code> to perform validation.
     *
     * @param key         The key that the validation rules are under (the
     *                    form elements name attribute).
     * @param bean        The bean validation is being performed on.
     * @param application servlet context
     * @param request     The current request object.
     * @param errors      The object any errors will be stored in.
     * @param page        This in conjunction with  the page property of a
     *                    <code>Field<code> can control the processing of
     *                    fields.  If the field's page is less than or equal
     *                    to this page value, it will be processed.
     */
    public static Validator initValidator(String key, Object bean,
        ServletContext application, HttpServletRequest request,
        ActionMessages errors, int page) {
        ValidatorResources resources =
            Resources.getValidatorResources(application, request);

        Locale locale = RequestUtils.getUserLocale(request, null);

        Validator validator = new Validator(resources, key);

        validator.setUseContextClassLoader(true);

        validator.setPage(page);

        validator.setParameter(SERVLET_CONTEXT_PARAM, application);
        validator.setParameter(HTTP_SERVLET_REQUEST_PARAM, request);
        validator.setParameter(Validator.LOCALE_PARAM, locale);
        validator.setParameter(ACTION_MESSAGES_PARAM, errors);
        validator.setParameter(Validator.BEAN_PARAM, bean);

        return validator;
    }
}

/*
 * $Id: WrappingLookupCommand.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.commands.generic;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.CatalogFactory;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.Filter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.chain.commands.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>Variant on chain LookupCommand which can optionally wrap the context it
 * passes to the looked up command in an alternative class.</p>
 */
public class WrappingLookupCommand implements Filter {
    /**
     * Provide Commons Logging instance for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(WrappingLookupCommand.class);

    // ------------------------------------------------------ Instance Variables

    /**
     * <p>Field for property.</p>
     */
    private String catalogName = null;

    /**
     * <p>Field for property.</p>
     */
    private String name = null;

    /**
     * <p>Field for property.</p>
     */
    private String nameKey = null;

    /**
     * <p>Field for property.</p>
     */
    private String wrapperClassName = null;

    /**
     * <p>Field for property.</p>
     */
    private boolean optional = false;

    /**
     * <p>Zero-argument constructor.</p>
     */
    public WrappingLookupCommand() {
        catalogName = null;
        name = null;
        nameKey = null;
        optional = false;
    }

    /**
     * <p>Return CatalogName property.  </p>
     *
     * @return Value of CatalogName property.
     */
    public String getCatalogName() {
        return catalogName;
    }

    /**
     * <p>Set CatalogName property.</p>
     *
     * @param catalogName New value for CatalogName
     */
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    /**
     * <p>Retrieve Name property.</p>
     *
     * @return Value of Name property
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Set Name property.</p>
     *
     * @param name New value for Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Return NameKey property.</p>
     *
     * @return Value of NameKey property.
     */
    public String getNameKey() {
        return nameKey;
    }

    /**
     * <p>Set NameKey property.</p>
     *
     * @param nameKey New value for NameKey
     */
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    /**
     * <p>Test Optional property.</p>
     *
     * @return TRUE if Optional is TRUE.
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * <p>Set Optional property.</p>
     *
     * @param optional New value for Optional
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * <p>Return the WrapperClass property.</p>
     *
     * @return The WrapperClass property
     */
    public String getWrapperClassName() {
        return wrapperClassName;
    }

    /**
     * <p>Set WrappClassName property. </p>
     *
     * @param wrapperClassName The name of a WrapperClass
     */
    public void setWrapperClassName(String wrapperClassName) {
        this.wrapperClassName = wrapperClassName;
    }

    /**
     * <p>Invoke the Command for a Context, returning TRUE if processing
     * should halt.</p>
     *
     * @param context Our ActionContext
     * @return TRUE if processing should halt
     * @throws Exception On any error
     */
    public boolean execute(Context context)
        throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("execute [" + this + "]");
        }

        Command command = getCommand(context);

        if (command != null) {
            return command.execute(getContext(context));
        } else {
            return false;
        }
    }

    /**
     * <p>Process the Exception for any Command that is a filter.</p>
     *
     * @param context   Our ActionContext
     * @param exception The Exception thrown by another Comamnd in a Chain
     * @return TRUE if there is a Filter to process
     */
    public boolean postprocess(Context context, Exception exception) {
        Command command = getCommand(context);

        if ((command != null) && (command instanceof Filter)) {
            try {
                return ((Filter) command).postprocess(getContext(context),
                    exception);
            } catch (NoSuchMethodException ex) {
                LOG.error("Error wrapping context in postprocess", ex);
            } catch (IllegalAccessException ex) {
                LOG.error("Error wrapping context in postprocess", ex);
            } catch (InvocationTargetException ex) {
                LOG.error("Error wrapping context in postprocess", ex);
            } catch (InstantiationException ex) {
                LOG.error("Error wrapping context in postprocess", ex);
            } catch (ClassNotFoundException ex) {
                LOG.error("Error wrapping context in postprocess", ex);
            }
        }

        return false;
    }

    /**
     * <p>Return the Command to process for this Context.</p>
     *
     * @param context The Context we are processing
     * @return The Command to process for this Context
     */
    protected Command getCommand(Context context) {
        CatalogFactory catalogFactory = CatalogFactory.getInstance();
        String catalogName = getCatalogName();
        Catalog catalog;

        if (catalogName == null) {
            catalog = catalogFactory.getCatalog();
            catalogName = "{default}"; // for debugging purposes
        } else {
            catalog = catalogFactory.getCatalog(catalogName);
        }

        if (catalog == null) {
            throw new IllegalArgumentException("Cannot find catalog '"
                + catalogName + "'");
        }

        Command command;
        String name = getName();

        if (name == null) {
            name = (String) context.get(getNameKey());
        }

        if (name != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Lookup command " + name + " in catalog "
                    + catalogName);
            }

            command = catalog.getCommand(name);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Found command " + command + ";" + " optional: "
                    + isOptional());
            }

            if ((command == null) && !isOptional()) {
                throw new IllegalArgumentException("Cannot find command " + "'"
                    + name + "' in catalog '" + catalogName + "'");
            } else {
                return command;
            }
        } else {
            throw new IllegalArgumentException("No command name");
        }
    }

    /**
     * <p>If the wrapperClassName property is not null, return a Context of
     * the type specified by wrapperClassName, instantiated using a single-arg
     * constructor which takes the context passed as an argument to this
     * method.</p>
     *
     * <p>This method throws an exception if the wrapperClass cannot be found,
     * or if there are any errors instantiating the wrapping context.</p>
     *
     * @param context Context we are processing
     * @return Context wrapper
     * @throws ClassNotFoundException    On failed instantiation
     * @throws InstantiationException    On failed instantiation
     * @throws InvocationTargetException On failed instantiation
     * @throws IllegalAccessException    On failed instantiation
     * @throws NoSuchMethodException     On failed instantiation
     */
    protected Context getContext(Context context)
        throws ClassNotFoundException, InstantiationException,
            InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {
        if (wrapperClassName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No defined wrapper class; "
                    + "returning original context.");
            }

            return context;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Looking for wrapper class: " + wrapperClassName);
        }

        Class wrapperClass = ClassUtils.getApplicationClass(wrapperClassName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Instantiating wrapper class");
        }

        return (Context) ConstructorUtils.invokeConstructor(wrapperClass,
            new Object[] { context });
    }
}

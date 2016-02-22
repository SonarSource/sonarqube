/*
 * $Id: ExecuteForwardCommand.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.commands;

import org.apache.commons.chain.Command;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.config.ForwardConfig;

/**
 * <p>Look up and execute a commons-chain <code>Command</code> based on
 * properties of the ActionContext's <code>forwardConfig</code> property.
 * </p>
 */
public class ExecuteForwardCommand extends ExecuteCommand {
    /**
     * <p>Return the command specified by the <code>command</code> and
     * <code>catalog</code> properties of the <code>forwardConfig</code>
     * property of the given <code>ActionContext</code>.  If
     * <code>forwardConfig</code> is null, return null.</p>
     *
     * @param context Our ActionContext
     * @return Command to execute or null
     */
    protected Command getCommand(ActionContext context) {
        ForwardConfig forwardConfig = context.getForwardConfig();

        if (forwardConfig == null) {
            return null;
        }

        return getCommand(forwardConfig.getCommand(), forwardConfig.getCatalog());
    }

    /**
     * <p> Determine whether the forwardConfig should be processed. </p>
     *
     * @param context The ActionContext we are processing
     * @return <p><code>true</code> if the given <code>ActionContext</code>
     *         has a non-null <code>forwardConfig</code> property.</p>
     */
    protected boolean shouldProcess(ActionContext context) {
        return (context.getForwardConfig() != null);
    }
}

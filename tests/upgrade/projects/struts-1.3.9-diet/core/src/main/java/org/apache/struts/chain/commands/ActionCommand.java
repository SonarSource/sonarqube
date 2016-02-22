/*
 * $Id: ActionCommand.java 471754 2006-11-06 14:55:09Z husted $
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

/**
 * <p>Marks a commons-chain <code>Command</code> which expects to operate upon
 * a Struts <code>ActionContext</code>.</p>
 */
public interface ActionCommand extends Command {
    /**
     * @param actionContext The <code>Context</code> for the current request
     * @return TRUE if processing should halt
     * @throws Exception On any error
     */
    boolean execute(ActionContext actionContext)
        throws Exception;
}

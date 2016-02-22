/*
 * $Id: RemoveCachedMessages.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.Map;
import org.apache.struts.Globals;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.action.ActionMessages;

/**
 * <p>Remove cached messages stored in the session.</p>
 *
 * @version $Id: RemoveCachedMessages.java 471754 2006-11-06 14:55:09Z husted $
 * @since Struts 1.3.5
 */
public class RemoveCachedMessages extends ActionCommandBase {

    /**
     * <p>Removes any <code>ActionMessages</code> object stored in the session
     * under <code>Globals.MESSAGE_KEY</code> and <code>Globals.ERROR_KEY</code>
     * if the messages' <code>isAccessed</code> method returns true.  This
     * allows messages to be stored in the session, displayed one time, and be
     * released here.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> so that processing continues
     * @throws Exception on any error
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {

        // Get session scope
        Map session = actionCtx.getSessionScope();

        // Remove messages as needed
        removeAccessedMessages(session, Globals.MESSAGE_KEY);

        // Remove error messages as needed
        removeAccessedMessages(session, Globals.ERROR_KEY);

        return false;
    }

    /**
     * <p>Removes any <code>ActionMessages</code> object from the specified
     * scope stored under the specified key if the messages'
     * <code>isAccessed</code> method returns true.
     *
     * @param scope The scope to check for messages in.
     * @param key The key the messages are stored under.
     */
    private void removeAccessedMessages(Map scope, String key) {
        ActionMessages messages = (ActionMessages)scope.get(key);
        if (messages != null && messages.isAccessed()) {
            scope.remove(key);
        }
    }
}

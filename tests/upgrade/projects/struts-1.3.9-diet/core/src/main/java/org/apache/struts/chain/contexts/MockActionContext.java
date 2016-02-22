/*
 * $Id: MockActionContext.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.contexts;

import java.util.HashMap;
import java.util.Map;

//  ISSUE: Are there any useful "assert" type methods we could add to this?

/**
 * <p> Implement <code>ActionContext</code> with empty maps for
 * <code>applicationScope</code>, <code>sessionScope</code>,
 * <code>requestScope</code>, and <code>parameterMap</code> properties. </p>
 */
public class MockActionContext extends ActionContextBase {
    private Map applicationScope = new HashMap();
    private Map requestScope = new HashMap();
    private Map sessionScope = new HashMap();
    private Map parameterMap = new HashMap();

    public Map getApplicationScope() {
        return applicationScope;
    }

    public void setApplicationScope(Map applicationScope) {
        this.applicationScope = applicationScope;
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map parameterMap) {
        this.parameterMap = parameterMap;
    }

    public Map getRequestScope() {
        return requestScope;
    }

    public void setRequestScope(Map requestScope) {
        this.requestScope = requestScope;
    }

    public Map getSessionScope() {
        return sessionScope;
    }

    public void setSessionScope(Map sessionScope) {
        this.sessionScope = sessionScope;
    }
}

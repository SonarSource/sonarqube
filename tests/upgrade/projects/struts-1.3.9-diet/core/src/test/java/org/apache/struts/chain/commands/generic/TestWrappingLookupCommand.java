/*
 * $Id: TestWrappingLookupCommand.java 471754 2006-11-06 14:55:09Z husted $
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

import junit.framework.TestCase;

import org.apache.commons.chain.Context;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.chain.web.servlet.ServletWebContext;
import org.apache.struts.chain.contexts.ServletActionContext;

/* JUnitTest case for class: org.apache.struts.chain.commands.generic.WrappingLookupCommand */
public class TestWrappingLookupCommand extends TestCase {
    public TestWrappingLookupCommand(String _name) {
        super(_name);
    }

    /* setUp method for test case */
    protected void setUp() {
    }

    /* tearDown method for test case */
    protected void tearDown() {
    }

    public void testSame() throws Exception {
        WrappingLookupCommand command = new WrappingLookupCommand();
        Context testContext = new ContextBase();

        Context wrapped = command.getContext(testContext);

        assertNotNull(wrapped);
        assertSame(testContext, wrapped);
    }

    public void testWrapContextSubclass()
        throws Exception {
        WrappingLookupCommand command = new WrappingLookupCommand();

        command.setWrapperClassName(ServletActionContext.class.getName());

        Context testContext = new ServletWebContext();

        Context wrapped = command.getContext(testContext);

        assertNotNull(wrapped);
        assertTrue(wrapped instanceof ServletActionContext);
    }

    /* Executes the test case */
    public static void main(String[] argv) {
        String[] testCaseList = { TestWrappingLookupCommand.class.getName() };

        junit.textui.TestRunner.main(testCaseList);
    }
}

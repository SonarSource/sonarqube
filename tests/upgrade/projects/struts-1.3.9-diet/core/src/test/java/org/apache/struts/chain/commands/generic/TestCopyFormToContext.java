/*
 * $Id: TestCopyFormToContext.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.chain.contexts.MockActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.FormPropertyConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.mock.MockFormBean;

/**
 * @version $Id: TestCopyFormToContext.java 161516 2005-04-15 19:22:47Z
 *          germuska $
 */
public class TestCopyFormToContext extends TestCase {
    private static final String POST_EXECUTION_CONTEXT_KEY = "afterTest";
    private MockActionContext context = null;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestCopyFormToContext.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        context = new MockActionContext();

        ModuleConfigImpl moduleConfig = new ModuleConfigImpl("/");

        context.setModuleConfig(moduleConfig);

        FormBeanConfig fooFBC = new FormBeanConfig();

        fooFBC.setName("foo");
        fooFBC.setType("org.apache.struts.mock.MockFormBean");
        moduleConfig.addFormBeanConfig(fooFBC);

        FormBeanConfig barFBC = new FormBeanConfig();

        barFBC.setName("bar");
        barFBC.setType("org.apache.struts.action.DynaActionForm"); // use a different type so we can verify lookups better

        FormPropertyConfig fpc = new FormPropertyConfig();

        fpc.setName("property");
        fpc.setType("java.lang.String");
        fpc.setInitial("test");
        barFBC.addFormPropertyConfig(fpc);
        moduleConfig.addFormBeanConfig(barFBC);

        ActionConfig testActionConfig = new ActionConfig();

        testActionConfig.setPath("/Test");
        testActionConfig.setName("foo");
        testActionConfig.setScope("request");
        moduleConfig.addActionConfig(testActionConfig);

        moduleConfig.freeze(); // otherwise, ActionConfigMatcher will be null and we'll get an NPE...
    }

    public void testLookupByNameAndRequestScope()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();
        String formName = "foo";

        command.setFormName(formName);
        command.setScope("request");
        command.setToKey(POST_EXECUTION_CONTEXT_KEY);

        assertNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        command.execute(context);

        assertNotNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNotNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        assertSame(context.get(POST_EXECUTION_CONTEXT_KEY),
            context.getRequestScope().get(formName));

        ActionForm theForm =
            (ActionForm) context.get(POST_EXECUTION_CONTEXT_KEY);

        assertTrue(theForm instanceof MockFormBean);
    }

    public void testLookupByActionPath()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();

        command.setActionPath("/Test");
        command.setToKey(POST_EXECUTION_CONTEXT_KEY);

        String formName = "foo"; // we know this, even though it's not being used for the lookup.

        assertNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        command.execute(context);

        assertNotNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNotNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        assertSame(context.get(POST_EXECUTION_CONTEXT_KEY),
            context.getRequestScope().get(formName));

        ActionForm theForm =
            (ActionForm) context.get(POST_EXECUTION_CONTEXT_KEY);

        assertTrue(theForm instanceof MockFormBean);
    }

    public void testLookupByNameAndSessionScope()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();
        String formName = "bar";

        command.setFormName(formName);
        command.setScope("session");
        command.setToKey(POST_EXECUTION_CONTEXT_KEY);

        assertNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        command.execute(context);

        assertNotNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNotNull(context.getSessionScope().get(formName));

        assertSame(context.get(POST_EXECUTION_CONTEXT_KEY),
            context.getSessionScope().get(formName));

        ActionForm theForm =
            (ActionForm) context.get(POST_EXECUTION_CONTEXT_KEY);

        assertTrue(theForm instanceof DynaActionForm);

        DynaActionForm dForm = (DynaActionForm) theForm;

        assertEquals("test", dForm.get("property"));
    }

    public void testExceptionHandlingWithNullFormName()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();
        String formName = "bar";

        // skip setting form name to test exception
        // command.setFormName(formName);
        command.setScope("session");
        command.setToKey(POST_EXECUTION_CONTEXT_KEY);

        assertNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        try {
            command.execute(context);
            fail(
                "Execution should throw an exception when form name is not set.");
        } catch (IllegalStateException e) {
            ; // expected.
        }
    }

    public void testExceptionHandlingWithNullEverything()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();
        String formName = "bar";

        // skip setting form name to test exception
        // command.setFormName(formName);
        // command.setScope("session");
        // command.setToKey(POST_EXECUTION_CONTEXT_KEY);
        assertNull(context.get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        try {
            command.execute(context);
            fail(
                "Execution should throw an exception when no properties are set.");
        } catch (IllegalStateException e) {
            ; // expected.
        }
    }

    public void testCopyToDefaultContextKey()
        throws Exception {
        CopyFormToContext command = new CopyFormToContext();
        String formName = "foo";

        command.setFormName(formName);
        command.setScope("request");

        assertNull(context.getActionForm());
        assertNull(context.getRequestScope().get(POST_EXECUTION_CONTEXT_KEY));
        assertNull(context.getSessionScope().get(POST_EXECUTION_CONTEXT_KEY));

        command.execute(context);

        assertNotNull(context.getActionForm());
        assertNotNull(context.getRequestScope().get(formName));
        assertNull(context.getSessionScope().get(formName));

        assertSame(context.getActionForm(),
            context.getRequestScope().get(formName));

        ActionForm theForm = (ActionForm) context.getActionForm();

        assertTrue(theForm instanceof MockFormBean);
    }
}

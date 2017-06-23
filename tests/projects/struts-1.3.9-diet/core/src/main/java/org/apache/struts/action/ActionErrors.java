/*
 * $Id: ActionErrors.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.Serializable;

/**
 * <p>A class that encapsulates the error messages being reported by the
 * <code>validate()</code> method of an <code>ActionForm</code>. Validation
 * errors are either global to the entire <code>ActionForm</code> bean they
 * are associated with, or they are specific to a particular bean property
 * (and, therefore, a particular input field on the corresponding form).</p>
 *
 * <p>Each individual error is described by an <code>ActionMessage</code>
 * object, which contains a message key (to be looked up in an appropriate
 * message resources database), and up to four placeholder arguments used for
 * parametric substitution in the resulting message.</p>
 *
 * <p><strong>IMPLEMENTATION NOTE</strong> - It is assumed that these objects
 * are created and manipulated only within the context of a single thread.
 * Therefore, no synchronization is required for access to internal
 * collections.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-06 18:03:30 -0400 (Sat, 06 Aug 2005)
 *          $
 */
public class ActionErrors extends ActionMessages implements Serializable {
    // --------------------------------------------------------- Public Methods

    /**
     * <p>Create an empty <code>ActionErrors</code> object.</p>
     */
    public ActionErrors() {
        super();
    }

    /**
     * <p>Create an <code>ActionErrors</code> object initialized with the
     * given messages.</p>
     *
     * @param messages The messages to be initially added to this object. This
     *                 parameter can be <code>null</code>.
     * @since Struts 1.1
     */
    public ActionErrors(ActionErrors messages) {
        super(messages);
    }
}

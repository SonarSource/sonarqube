/*
 * $Id: ActionFormBean.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.config.FormBeanConfig;

/**
 * <p>An <strong>ActionFormBean</strong> is the definition of a form bean that
 * is loaded from a <code>&lt;form-bean&gt;</code> element in the Struts
 * configuration file. It can be subclassed as necessary to add additional
 * properties.</p>
 *
 * <p>Since Struts 1.1 <code>ActionFormBean</code> extends
 * <code>FormBeanConfig</code>.</p>
 *
 * <p><strong>NOTE</strong> - This class would have been deprecated and
 * replaced by <code>org.apache.struts.config.FormBeanConfig</code> except for
 * the fact that it is part of the public API that existing applications are
 * using.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-14 01:09:32 -0400 (Sat, 14 May 2005)
 *          $
 */
public class ActionFormBean extends FormBeanConfig {
    /**
     * <p>Construct an instance with default vaslues.</p>
     */
    public ActionFormBean() {
        super();
    }

    /**
     * <p>Construct an instance with the specified values.</p>
     *
     * @param name Form bean name
     * @param type Fully qualified class name
     */
    public ActionFormBean(String name, String type) {
        super();
        setName(name);
        setType(type);
    }
}

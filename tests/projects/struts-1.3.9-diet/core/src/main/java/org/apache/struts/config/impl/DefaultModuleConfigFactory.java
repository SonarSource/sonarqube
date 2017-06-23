/*
 * $Id: DefaultModuleConfigFactory.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.config.impl;

import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;

import java.io.Serializable;

/**
 * <p>A factory for creating {@link ModuleConfig} instances.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:45:39 -0400 (Sat, 07 May 2005)
 *          $
 * @see ModuleConfig
 * @see ModuleConfigFactory
 */
public class DefaultModuleConfigFactory extends ModuleConfigFactory
    implements Serializable {
    // --------------------------------------------------------- Public Methods

    /**
     * <p>Create and return a newly instansiated {@link ModuleConfig}. This
     * method must be implemented by concrete subclasses.</p>
     *
     * @param prefix Module prefix for Configuration
     */
    public ModuleConfig createModuleConfig(String prefix) {
        return new ModuleConfigImpl(prefix);
    }
}

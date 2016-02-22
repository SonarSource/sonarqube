/*
 * $Id: PropertyMessageResourcesFactory.java 480549 2006-11-29 12:16:15Z niallp $
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
package org.apache.struts.util;


/**
 * Factory for <code>PropertyMessageResources</code> instances.  The
 * configuration paramter for such instances is the base Java package name of
 * the resources entries from which our keys and values will be loaded.
 *
 * @version $Rev: 480549 $ $Date: 2006-11-29 13:16:15 +0100 (Wed, 29 Nov 2006) $
 */
public class PropertyMessageResourcesFactory extends MessageResourcesFactory {
    // --------------------------------------------------------- Public Methods

    /**
     * Create and return a newly instansiated <code>MessageResources</code>.
     * This method must be implemented by concrete subclasses.
     *
     * @param config Configuration parameter(s) for the requested bundle
     */
    public MessageResources createResources(String config) {
        PropertyMessageResources messageResources =
               new PropertyMessageResources(this, config, this.returnNull);
        String mode = null;
        if (getConfig() != null) {
            mode = getConfig().getProperty("mode");
        }
        messageResources.setMode(mode);
        return messageResources;
    }
}

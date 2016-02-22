/*
 * $Id: NestedPropertySupport.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested;


/**
 * This interface is for managing classes of the nested extension, so they can
 * know to set the tag's <i>property</i> property.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public interface NestedPropertySupport extends NestedTagSupport {
    /**
     * The getters and setters required to set a tags <i>property</i>
     * property.
     *
     * @return String value of the tags' property property
     */
    public String getProperty();

    /**
     * The setter for the poroperty property
     *
     * @param newProperty new String value to set the property property to
     */
    public void setProperty(String newProperty);
}

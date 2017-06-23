/*
 * $Id: NestedReference.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.Serializable;

/**
 * So that a nested hierarchy can penetrate a dynamic JSP include, this class
 * will hold the details of a bean name and nested property.
 *
 * @version $Rev: 471754 $
 * @since Struts 1.1
 */
public class NestedReference implements Serializable {
    /* Usual member variables */
    private String beanName;
    private String property;

    /**
     * Empty constructor.
     */
    public NestedReference() {
    }

    /**
     * Constructor takes the all the relevant details to init the object.
     *
     * @param name     String name of the bean that the include is to
     *                 reference
     * @param property String nested property value that the include will be
     *                 continuing on with.
     */
    public NestedReference(String name, String property) {
        this.beanName = name;
        this.property = property;
    }

    /**
     * Getter for the bean name
     *
     * @return String value that will be the bean's reference
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Setter for the bean name
     *
     * @param newName String value to set the bean reference.
     */
    public void setBeanName(String newName) {
        this.beanName = newName;
    }

    /**
     * Getter for the nested property
     *
     * @return String value that is the nested property for the current
     *         nesting
     */
    public String getNestedProperty() {
        return this.property;
    }

    /**
     * Setter for the nested property
     *
     * @param newProperty String value of the new current nesting level
     */
    public void setNestedProperty(String newProperty) {
        this.property = newProperty;
    }
}

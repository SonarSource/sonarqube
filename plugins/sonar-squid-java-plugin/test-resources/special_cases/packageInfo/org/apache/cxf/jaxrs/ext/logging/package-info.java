/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * JAX-RS specific logging support. Based on <tt>java.util.logging</tt> (JUL)
 * with use of different logging frameworks factored out; assumes that client 
 * with source code logging to other systems, like Log4J, can bridge 
 * to this implementation applying <a href="www.slf4j.org">SLF4J</a> 
 * that JAXRS already depends on.
 */
@javax.xml.bind.annotation.XmlSchema(xmlns = {
        @javax.xml.bind.annotation.XmlNs(namespaceURI = "http://cxf.apache.org/jaxrs/log", prefix = "log")
            })
package org.apache.cxf.jaxrs.ext.logging;


/*
 * $Id: NestedDefineTei.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested.bean;

import org.apache.struts.taglib.logic.IterateTei;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * NestedDefineTei to make sure that the implied setting of the name property
 * of a nested tag is properly handed in the casting of the defined object.
 * Currently goes to String, but for the purposes of most nested objects they
 * will benefit more from a simple Object casting.
 *
 * @version $Rev: 471754 $
 * @since Struts 1.1
 */
public class NestedDefineTei extends IterateTei {
    /**
     * Return information about the scripting variables to be created.
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        // get the type
        String type = (String) data.getAttribute("type");

        // make it an object if none supplied
        if (type == null) {
            type = "java.lang.Object";
        }

        // return the infor about the deined object
        VariableInfo[] vinfo = new VariableInfo[1];

        vinfo[0] =
            new VariableInfo(data.getAttributeString("id"), type, true,
                VariableInfo.AT_END);

        /* return the results */
        return vinfo;
    }
}

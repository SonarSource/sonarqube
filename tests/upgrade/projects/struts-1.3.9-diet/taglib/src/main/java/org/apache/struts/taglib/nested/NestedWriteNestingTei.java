/*
 * $Id: NestedWriteNestingTei.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * NestedWriteNestingTei
 *
 * This class will allow the nested:writeNesting tag to actually do what the
 * doc says and make a scripting variable as an option (when "id" is
 * supplied).
 *
 * @version $Rev: 471754 $
 * @since Struts 1.2
 */
public class NestedWriteNestingTei extends TagExtraInfo {
    /**
     * Return information about the scripting variables to be created.
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        /* the id parameter */
        String id = data.getAttributeString("id");

        VariableInfo[] vi = null;

        if (id != null) {
            vi = new VariableInfo[1];
            vi[0] =
                new VariableInfo(id, "java.lang.String", true,
                    VariableInfo.AT_END);
        } else {
            vi = new VariableInfo[0];
        }

        // job done
        return vi;
    }
}

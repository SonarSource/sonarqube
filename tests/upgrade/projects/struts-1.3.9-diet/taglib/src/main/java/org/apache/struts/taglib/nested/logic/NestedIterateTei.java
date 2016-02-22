/*
 * $Id: NestedIterateTei.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.taglib.nested.logic;

import org.apache.struts.taglib.logic.IterateTei;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * NestedIterateTei Extending the original tag's tei class, so that we can
 * make the "id" attribute optional, so that those who want to script can add
 * it if they need it otherwise we can maintain the nice lean tag markup.
 *
 * TODO - Look at deleting this class. Potentially a pointless existance now
 * that the super class is towing the line. Left alone because it's not
 * hurting anything as-is. Note: When done, it requires pointing the tei
 * reference in the struts-nested.tld to org.apache.struts.taglib.logic.IterateTei
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NestedIterateTei extends IterateTei {
    /**
     * Return information about the scripting variables to be created.
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        /* It just lets the result through. */
        return super.getVariableInfo(data);
    }
}

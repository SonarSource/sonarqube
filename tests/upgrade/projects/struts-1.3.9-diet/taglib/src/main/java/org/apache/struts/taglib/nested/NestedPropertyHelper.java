/*
 * $Id: NestedPropertyHelper.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.taglib.html.Constants;
import org.apache.struts.taglib.html.FormTag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.Tag;

import java.util.StringTokenizer;

/**
 * <p>A simple helper class that does everything that needs to be done to get
 * the nested tag extension to work. The tags will pass in their relative
 * properties and this class will leverage the accessibility of the request
 * object to calculate the nested references and manage them from a central
 * place.</p>
 *
 * <p>The helper method {@link #setNestedProperties} takes a reference to the
 * tag itself so all the simpler tags can have their references managed from a
 * central location. From here, the reference to a provided name is also
 * preserved for use.</p>
 *
 * <p>With all tags keeping track of themselves, we only have to seek to the
 * next level, or parent tag, were a tag will append a dot and it's own
 * property.</p>
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:38:42 -0400 (Sat, 16 Oct 2004)
 *          $
 * @since Struts 1.1
 */
public class NestedPropertyHelper {
    /* key that the tags can rely on to set the details against */
    public static final String NESTED_INCLUDES_KEY = "<nested-includes-key/>";

    /**
     * Returns the current nesting property from the request object.
     *
     * @param request object to fetch the property reference from
     * @return String of the bean name to nest against
     */
    public static final String getCurrentProperty(HttpServletRequest request) {
        // get the old one if any
        NestedReference nr =
            (NestedReference) request.getAttribute(NESTED_INCLUDES_KEY);

        // return null or the property
        return (nr == null) ? null : nr.getNestedProperty();
    }

    /**
     * <p>Returns the bean name from the request object that the properties
     * are nesting against.</p>
     *
     * <p>The requirement of the tag itself could be removed in the future,
     * but is required if support for the <html:form> tag is maintained.</p>
     *
     * @param request object to fetch the bean reference from
     * @param nested  tag from which to start the search from
     * @return the string of the bean name to be nesting against
     */
    public static final String getCurrentName(HttpServletRequest request,
        NestedNameSupport nested) {
        // get the old one if any
        NestedReference nr =
            (NestedReference) request.getAttribute(NESTED_INCLUDES_KEY);

        // return null or the property
        if (nr != null) {
            return nr.getBeanName();
        } else {
            // need to look for a form tag...
            Tag tag = (Tag) nested;
            Tag formTag = null;

            // loop all parent tags until we get one that can be nested against
            do {
                tag = tag.getParent();

                if ((tag != null) && tag instanceof FormTag) {
                    formTag = tag;
                }
            } while ((formTag == null) && (tag != null));

            if (formTag == null) {
                return "";
            }

            // return the form's name
            return ((FormTag) formTag).getBeanName();
        }
    }

    /**
     * Get the adjusted property. Apply the provided property, to the property
     * already stored in the request object.
     *
     * @param request  to pull the reference from
     * @param property to retrieve the evaluated nested property with
     * @return String of the final nested property reference.
     */
    public static final String getAdjustedProperty(HttpServletRequest request,
        String property) {
        // get the old one if any
        String parent = getCurrentProperty(request);

        return calculateRelativeProperty(property, parent);
    }

    /**
     * Sets the provided property into the request object for reference by the
     * other nested tags.
     *
     * @param request  object to set the new property into
     * @param property String to set the property to
     */
    public static final void setProperty(HttpServletRequest request,
        String property) {
        // get the old one if any
        NestedReference nr = referenceInstance(request);

        nr.setNestedProperty(property);
    }

    /**
     * Sets the provided name into the request object for reference by the
     * other nested tags.
     *
     * @param request object to set the new name into
     * @param name    String to set the name to
     */
    public static final void setName(HttpServletRequest request, String name) {
        // get the old one if any
        NestedReference nr = referenceInstance(request);

        nr.setBeanName(name);
    }

    /**
     * Deletes the nested reference from the request object.
     *
     * @param request object to remove the reference from
     */
    public static final void deleteReference(HttpServletRequest request) {
        // delete the reference
        request.removeAttribute(NESTED_INCLUDES_KEY);
    }

    /**
     * Helper method that will set all the relevant nesting properties for the
     * provided tag reference depending on the implementation.
     *
     * @param request object to pull references from
     * @param tag     to set the nesting values into
     */
    public static void setNestedProperties(HttpServletRequest request,
        NestedPropertySupport tag) {
        boolean adjustProperty = true;

        /* if the tag implements NestedNameSupport, set the name for the tag also */
        if (tag instanceof NestedNameSupport) {
            NestedNameSupport nameTag = (NestedNameSupport) tag;

            if ((nameTag.getName() == null)
                || Constants.BEAN_KEY.equals(nameTag.getName())) {
                nameTag.setName(getCurrentName(request, (NestedNameSupport) tag));
            } else {
                adjustProperty = false;
            }
        }

        /* get and set the relative property, adjust if required */
        String property = tag.getProperty();

        if (adjustProperty) {
            property = getAdjustedProperty(request, property);
        }

        tag.setProperty(property);
    }

    /**
     * Pulls the current nesting reference from the request object, and if
     * there isn't one there, then it will create one and set it.
     *
     * @param request object to manipulate the reference into
     * @return current nesting reference as stored in the request object
     */
    private static final NestedReference referenceInstance(
        HttpServletRequest request) {
        /* get the old one if any */
        NestedReference nr =
            (NestedReference) request.getAttribute(NESTED_INCLUDES_KEY);

        // make a new one if required
        if (nr == null) {
            nr = new NestedReference();
            request.setAttribute(NESTED_INCLUDES_KEY, nr);
        }

        // return the reference
        return nr;
    }

    /* This property, providing the property to be appended, and the parent tag
    * to append the property to, will calculate the stepping of the property
    * and return the qualified nested property
    *
    * @param property the property which is to be appended nesting style
    * @param parent the "dot notated" string representing the structure
    * @return qualified nested property that the property param is to the parent
    */
    private static String calculateRelativeProperty(String property,
        String parent) {
        if (parent == null) {
            parent = "";
        }

        if (property == null) {
            property = "";
        }

        /* Special case... reference my parent's nested property.
        Otherwise impossible for things like indexed properties */
        if ("./".equals(property) || "this/".equals(property)) {
            return parent;
        }

        /* remove the stepping from the property */
        String stepping;

        /* isolate a parent reference */
        if (property.endsWith("/")) {
            stepping = property;
            property = "";
        } else {
            stepping = property.substring(0, property.lastIndexOf('/') + 1);

            /* isolate the property */
            property =
                property.substring(property.lastIndexOf('/') + 1,
                    property.length());
        }

        if (stepping.startsWith("/")) {
            /* return from root */
            return property;
        } else {
            /* tokenize the nested property */
            StringTokenizer proT = new StringTokenizer(parent, ".");
            int propCount = proT.countTokens();

            /* tokenize the stepping */
            StringTokenizer strT = new StringTokenizer(stepping, "/");
            int count = strT.countTokens();

            if (count >= propCount) {
                /* return from root */
                return property;
            } else {
                /* append the tokens up to the token difference */
                count = propCount - count;

                StringBuffer result = new StringBuffer();

                for (int i = 0; i < count; i++) {
                    result.append(proT.nextToken());
                    result.append('.');
                }

                result.append(property);

                /* parent reference will have a dot on the end. Leave it off */
                if (result.charAt(result.length() - 1) == '.') {
                    return result.substring(0, result.length() - 1);
                } else {
                    return result.toString();
                }
            }
        }
    }
}

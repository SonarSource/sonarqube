/*
 * $Id: LazyValidatorForm.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.validator;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.LazyDynaBean;

import java.util.List;
import java.util.Map;

/**
 * <p>Struts <i>Lazy</i> <code>ActionForm</code> which <i>wraps</i> a
 * <code>LazyDynaBean</code>.</p>
 *
 * <p>There isn't really that much to this implementation as most of the
 * <i>lazy</i> behaviour is in <code>LazyDynaBean</code> and <i>wrapping</i>
 * the <code>LazyDynaBean<code> is handled in the parent
 * <code>BeanValidatorForm</code>. The only thing it really does is populate
 * <i>indexed</i> properties which are a <code>List<code> type with a
 * <code>LazyDynaBean<code> in the <code>get(name, index)</code> method.</p>
 *
 * <p><i>Lazy</i> DynaBeans provide several types of <i>lazy</i>
 * behaviour:</p>
 *
 * <ul>
 *
 * <li><b><i>lazy</i> property addition</b> - properties which do not exist
 * are automatically added.</li>
 *
 * <li><b><i>lazy</i> List facilities</b> - automatically <i>grows</i> a
 * <code>List</code> or <code>Array</code> to accomodate the index value being
 * set.</li>
 *
 * <li><b><i>lazy</i> List creation</b> - automatic creation of a
 * <code>List</code> or <code>Array</code> for <i>indexed</i> properties, if
 * it doesn't exist.</li> <li><b><i>lazy</i> Map creation</b> - automatic
 * creation of a <code>Map</code> for <i>mapped</i> properties, if it doesn't
 * exist.</li>
 *
 * </ul>
 *
 * <p>Using this <i>lazy</i> <code>ActionForm</code> means that you don't have
 * to define the ActionForm's properties in the <code>struts-config.xml</code>.
 * However, a word of warning, everything in the Request gets populated into
 * this <code>ActionForm</code> circumventing the normal <i>firewall</i>
 * function of Struts forms. Therefore you should only <i>take out</i> of this
 * form properties you expect to be there rather than blindly populating all
 * the properties into the business tier.</p>
 *
 * <p>Having said that it is not necessary to pre-define properties in the
 * <code>struts-config.xml</code>, it is useful to sometimes do so for
 * <i>mapped</i> or <i>indexed</i> properties. For example, if you want to use
 * a different <code>Map<code> implementation from the default
 * <code>HashMap</code> or an array for indexed properties, rather than the
 * default <code>List</code> type:</p>
 *
 * <pre><code>
 *   &lt;form-bean name="myForm" type="org.apache.struts.validator.LazyValidatorForm"&gt;
 *     &lt;form-property name="myMap" type="java.util.TreeMap" /&gt;
 *     &lt;form-property name="myBeans" type="org.apache.commons.beanutils.LazyDynaBean[]"
 * /&gt;
 *   &lt;/form-bean&gt;
 * </code></pre>
 *
 * <p>Another reason for defining <i>indexed</i> properties in the
 * <code>struts-config.xml</code> is that if you are validating indexed
 * properties using the Validator and none are submitted then the indexed
 * property will be <code>null</code> which causes validator to fail.
 * Pre-defining them in the <code>struts-config.xml</code> will result in a
 * zero-length indexed property (array or List) being instantiated, avoiding
 * an issue with validator in that circumstance.</p>
 *
 * <p>This implementation validates using the ActionForm <i>name</i>. If you
 * require a version that validates according to the <i>path</i> then it can
 * be easily created in the following manner:</p>
 *
 * <pre><code>
 *    public class MyLazyForm extends LazyValidatorForm {
 *
 *        public MyLazyForm () {
 *            super();
 *            setPathValidation(true);
 *        }
 *
 *    }
 * </code></pre>
 *
 * <p>Rather than using this class, another alternative is to either use a
 * <code>LazyDynaBean</code> or custom version of <code>LazyDynaBean</code>
 * directly. Struts now automatically <i>wraps</i> objects which are not
 * <code>ActionForms</code> in a <code>BeanValidatorForm</code>. For
 * example:</p>
 *
 * <pre><code>
 *   &lt;form-bean name="myForm" type="org.apache.commons.beanutils.LazyDynaBean"&gt;
 *     &lt;form-property name="myBeans" type="org.apache.commons.beanutils.LazyDynaBean[]"
 * /&gt;
 *   &lt;/form-bean&gt;
 * </code></pre>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 * @see <a href="http://jakarta.apache.org/commons/beanutils/apidocs/org/apache/commons/beanutils/package-summary.html#dynamic.lazy">Commons
 *      BeanUtils JavaDoc</a>
 * @since Struts 1.2.6
 */
public class LazyValidatorForm extends BeanValidatorForm {
    // ------------------- Constructors ----------------------------------

    /**
     * Default Constructor which creates a <code>LazyDynaBean</code> to
     * <i>back</i> this form.
     */
    public LazyValidatorForm() {
        super(new LazyDynaBean());
    }

    /**
     */
    public LazyValidatorForm(DynaBean bean) {
        super(bean);
    }

    // ------------------- DynaBean methods ----------------------------------

    /**
     * <p>Return an indexed property value.</p>
     *
     * <p>If the "indexed" property is a <code>List</code> type then any
     * missing values are populated with a bean (created in the
     * <code>newIndexedBean(name)</code> method - in this implementation this
     * is a <code>LazyDynaBean</code> type.</p>
     */
    public Object get(String name, int index) {
        int size = size(name);

        // Get the indexed property
        Object value = dynaBean.get(name, index);

        // Create missing beans for Lists
        if (value == null) {
            Object indexedValue = dynaBean.get(name);

            if (List.class.isAssignableFrom(indexedValue.getClass())) {
                for (int i = size; i <= index; i++) {
                    value = newIndexedBean(name);
                    set(name, i, value);
                }
            }
        }

        return value;
    }

    // ------------------- Public methods ----------------------------------

    /**
     * <p>Return the <code>Map</code> containing the property values.</p>
     *
     * <p>Provided so that properties can be access using JSTL.</p>
     */
    public Map getMap() {
        return ((LazyDynaBean) dynaBean).getMap();
    }

    // ------------------- Protected methods ----------------------------------

    /**
     * <p>Creates new <code>DynaBean</code> instances to populate an 'indexed'
     * property of beans - defaults to <code>LazyDynaBean</code> type.</p>
     *
     * <p>Override this method if you require a different type of
     * <code>DynaBean</code>.</p>
     */
    protected DynaBean newIndexedBean(String name) {
        return new LazyDynaBean();
    }
}

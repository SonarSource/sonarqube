/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated since 6.2, this extension is ignored as dashboards have been removed
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WidgetProperty {

  String key();

  WidgetPropertyType type() default WidgetPropertyType.STRING;

  String defaultValue() default "";

  String description() default "";

  boolean optional() default true;

  /**
   * @since 3.3  Options for property of type {@link WidgetPropertyType#METRIC}.
   *
   * If no option is specified, any metric will match.
   * If options are specified, all must match for the metric to be displayed.
   * Three types of filter are supported <code>key:REGEXP</code>, <code>domain:REGEXP</code> and <code>type:comma_separated__list_of_types</code>.
   * For example <code>key:new_.*</code> will match any metric which key starts by <code>new_</code>.
   * For example <code>type:INT,FLOAT</code> will match any metric of type <code>INT</code> or <code>FLOAT</code>.
   * For example <code>type:NUMERIC</code> will match any metric of numerictype.
   *
   * @since 3.5  Options for property of type {@link WidgetPropertyType#SINGLE_SELECT_LIST}.
   * For example {"property_1", "property_3", "property_3"}).
   *
   */
  String[] options() default {};
}

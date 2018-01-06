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
package org.sonar.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Property field.
 *
 * @since 3.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PropertyField {
  /**
   * Unique key within a property. It shouldn't be prefixed.
   * Settings for this field are stored into the database with a composite key
   * <code>{key of parent property}.{key of the set}.{key of this field}</code>
   * eg. <code>sonar.jira.servers.JIRA1.url</code>
   */
  String key();

  /**
   * This name will be displayed on the Settings page. This can be overridden/translated
   * by adding a a value for: <code>field.{key of parent property}.{key of this field}.name</code> in the language bundle.
   */
  String name();

  /**
   * If not empty, this description will be displayed on the Settings page. This can be overridden/translated
   * by adding a a value for: <code>field.{key of parent property}.{key of this field}.description</code> in the language bundle.
   */
  String description() default "";

  /**
   * Indicative size of the field value in characters. This size is not validated, it is merely used by the GUI
   * to size the different input fields of a property set.
   *
   * @deprecated since 6.1, as it was only used for UI.
   */
  @Deprecated
  int indicativeSize() default 20;

  PropertyType type() default PropertyType.STRING;

  /**
   * Options for *_LIST types
   */
  String[] options() default {};
}

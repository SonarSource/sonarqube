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
package org.sonar.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 2.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RuleProperty {

  /**
   * The default key is the field name, read by reflection. Overriding this key can be useful when
   * obfuscating the code.
   */
  String key() default "";

  /**
   * Optional description
   */
  String description() default "";

  /**
   * Optional default value.
   */
  String defaultValue() default "";

  /**
   * Optional type of property value. Supported values are: STRING, TEXT, PASSWORD, BOOLEAN, INTEGER, FLOAT.
   * If <code>type</code> is omitted, it is guessed from the type of the annotated field.
   *
   * @since 3.2
   */
  String type() default "";
}

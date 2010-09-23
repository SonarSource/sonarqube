/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3. Use @Rule
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
public @interface Check {

  /**
   * The default key is the class name.
   */
  String key() default "";

  /**
   * The path to resource bundles (optional). If not set, then it equals the class name. 
   */
  String bundle() default "";

  /**
   * The check title. If not defined, then the title is the key
   */
  String title() default "";

  /**
   * The check description, optional.
   */
  String description() default "";

  /**
   * Default priority.
   */
  Priority priority() default Priority.MAJOR;


  /**
   * Will probably be deprecated and replaced by tags in version 2.2
   */
  IsoCategory isoCategory();
}

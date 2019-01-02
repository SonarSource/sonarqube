/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
@Target(ElementType.TYPE)
public @interface Rule {

  /**
   * The default key is the class name.
   */
  String key() default "";

  /**
   * The rule name. If not defined, then the name is the key
   */
  String name() default "";

  /**
   * HTML description
   */
  String description() default "";

  /**
   * Default severity used when activating the rule in a Quality profile.
   */
  Priority priority() default Priority.MAJOR;

  Cardinality cardinality() default Cardinality.SINGLE;

  /**
   * The status. Can be READY, BETA or DEPRECATED
   * @since 3.6
   */
  String status() default "READY";

  /**
   * Rule tags
   * @since 4.2
   */
  String[] tags() default {};
}

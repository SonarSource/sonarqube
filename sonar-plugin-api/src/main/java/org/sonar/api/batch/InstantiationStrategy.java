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
package org.sonar.api.batch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define instantiation strategy of batch IoC components. If a component is not annotated, then default value
 * is {@link org.sonar.api.batch.InstantiationStrategy#PER_PROJECT}.
 * @since 4.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstantiationStrategy {

  /**
   * Shared task extension. Available in task container.
   */
  String PER_TASK = "PER_TASK";

  /**
   * Shared extension. Available in top level project container.
   */
  String PER_BATCH = "PER_BATCH";

  /**
   * Created and initialized for each project and sub-project (a project is a module in Maven terminology).
   */
  String PER_PROJECT = "PER_PROJECT";

  String value();
}

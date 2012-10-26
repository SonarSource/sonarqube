/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.batch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define instantiation strategy of batch extensions. If an extension is not annotated, then default value
 * is {@link org.sonar.api.batch.InstantiationStrategy#PROJECT}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InstantiationStrategy {

  /**
   * Shared extension. Lifecycle is the full analysis.
   * @deprecated replaced by the constant {@link org.sonar.api.batch.InstantiationStrategy.BATCH} since version 3.4
   */
  @Deprecated
  String PER_BATCH = "PER_BATCH";

  /**
   * Created and initialized for each project and sub-project (a project is a module in Maven terminology).
   * @deprecated replaced by the constant {@link org.sonar.api.batch.InstantiationStrategy.PROJECT} since version 3.4
   */
  @Deprecated
  String PER_PROJECT = "PER_PROJECT";

  /**
   * @since 3.4
   */
  String BOOTSTRAP = "BOOTSTRAP";

  /**
   * @since 3.4
   */
  String BATCH = "PER_BATCH";

  /**
   * @since 3.4
   */
  String PROJECT = "PER_PROJECT";

  String value();
}

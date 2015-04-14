/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.postjob;

import com.google.common.annotations.Beta;

/**
 * Describe what a {@link PostJob} is doing. Information may be used by the platform
 * to log interesting information or perform some optimization.
 * See {@link PostJob#describe(PostJobDescriptor)}
 * @since 5.2
 */
@Beta
public interface PostJobDescriptor {

  /**
   * Displayable name of the {@link PostJob}. Will be displayed in logs.
   */
  PostJobDescriptor name(String postJobName);

  /**
   * Property this {@link PostJob} depends on. Used by the platform to skip execution of the {@link PostJob} when
   * property is not set.
   */
  PostJobDescriptor requireProperty(String... propertyKey);

  /**
   * List properties this {@link PostJob} depends on. Used by the platform to skip execution of the {@link PostJob} when
   * property is not set.
   */
  PostJobDescriptor requireProperties(String... propertyKeys);

  /**
   * Should this PostJob be disabled in preview mode. Default is to run all PostJobs in preview mode.
   */
  PostJobDescriptor disabledInPreview();

}

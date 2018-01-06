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
package org.sonar.api.batch.postjob;

/**
 * Describe what a {@link PostJob} is doing. Information may be used by the platform
 * to log interesting information or perform some optimization.
 * See {@link PostJob#describe(PostJobDescriptor)}
 * @since 5.2
 */
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

}

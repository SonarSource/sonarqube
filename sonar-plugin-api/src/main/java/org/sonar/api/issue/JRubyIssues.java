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
package org.sonar.api.issue;

import org.sonar.api.ServerComponent;

import java.util.Map;

/**
 * Facade for JRuby on Rails extensions.
 * <p>
 * Reference from Ruby code : <code>Api.issues</code>
 * </p>
 *
 * @since 3.6
 */
public interface JRubyIssues extends ServerComponent {

  /**
   * Search for issues.
   *
   * <p>
   *   Ruby: <code>Api.issues.find(hash)</code>
   * </p>
   *
   * <p>Parameters</p>
   * <ul>
   *   TODO document parameters
   * </ul>
   */
  IssueFinder.Results find(Map<String, Object> parameters);

}

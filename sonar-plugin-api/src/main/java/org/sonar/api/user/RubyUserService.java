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
package org.sonar.api.user;

import org.sonar.api.ServerSide;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

/**
 * @since 3.6
 */
@ServerSide
public interface RubyUserService {

  @CheckForNull
  User findByLogin(String login);

  /**
   * Search for users
   * <p/>
   * Optional parameters are:
   * <ul>
   *   <li><code>s</code> to match all the logins or names containing this search query</li>
   *   <li><code>logins</code>, as an array of strings (['simon', 'julien']) or a comma-separated list of logins ('simon,julien')</li>
   *   <li><code>includeDeactivated</code> as a boolean. By Default deactivated users are excluded from query.</li>
   * </ul>
   */
  List<User> find(Map<String, Object> params);
}

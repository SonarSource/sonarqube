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
package org.sonar.wsclient.user;

import org.sonar.wsclient.internal.EncodingUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class UserQuery {

  private final Map<String, Object> params = new HashMap<String, Object>();

  private UserQuery() {
  }

  public static UserQuery create() {
    return new UserQuery();
  }

  public UserQuery includeDeactivated() {
    params.put("includeDeactivated", "true");
    return this;
  }

  public UserQuery logins(String... s) {
    params.put("logins", EncodingUtils.toQueryParam(s));
    return this;
  }

  public UserQuery searchText(@Nullable String s) {
    if (s != null) {
      params.put("s", s);
    } else {
      params.remove("s");
    }
    return this;
  }

  public Map<String, Object> urlParams() {
    return params;
  }
}

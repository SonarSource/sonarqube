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
package org.sonar.wsclient.project;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.0
 */
public class NewProject {

  private final Map<String, Object> params;

  private NewProject() {
    params = new HashMap<String, Object>();
  }

  public static NewProject create() {
    return new NewProject();
  }

  public Map<String, Object> urlParams() {
    return params;
  }

  public NewProject key(String key) {
    params.put("key", key);
    return this;
  }

  public NewProject name(String name) {
    params.put("name", name);
    return this;
  }
}

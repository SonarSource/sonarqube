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
package org.sonar.wsclient.project.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.project.Project;
import org.sonar.wsclient.project.ProjectClient;

import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#projectClient()}.
 */
public class DefaultProjectClient implements ProjectClient {

  private static final String ROOT_URL = "/api/projects";
  private static final String CREATE_URL = ROOT_URL + "/create";

  private final HttpRequestFactory requestFactory;

  public DefaultProjectClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public Project create(NewProject newProject) {
    String json = requestFactory.post(CREATE_URL, newProject.urlParams());
    return jsonToProject(json);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Project jsonToProject(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultProject(jsonRoot);
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.tester;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;
import org.sonarqube.ws.client.projects.ExportFindingsRequest;
import org.sonarqube.ws.client.projects.ProjectsService;
import org.sonarqube.ws.client.projects.SearchRequest;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

public class ProjectTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  ProjectTester(TesterSession session) {
    this.session = session;
  }

  void deleteAll() {
    ProjectsService service = session.wsClient().projects();
    service.search(new SearchRequest().setQualifiers(singletonList("TRK"))).getComponentsList().forEach(p -> service.delete(new DeleteRequest().setProject(p.getKey())));
  }

  public ProjectsService service() {
    return  session.wsClient().projects();
  }

  public WsResponse exportFindings(String projectKey, @Nullable String branchKey) {
    ProjectsService service = session.wsClient().projects();
    return service.exportFindings(new ExportFindingsRequest(projectKey, branchKey));
  }

  @SafeVarargs
  public final Projects.CreateWsResponse.Project provision(Consumer<CreateRequest>... populators) {
    String key = generateKey();
    CreateRequest request = new CreateRequest()
      .setProject(key)
      .setName("Name " + key);
    stream(populators).forEach(p -> p.accept(request));

    return session.wsClient().projects().create(request).getProject();
  }

  public Components.Component getComponent(String componentKey) {
    try {
      return session.wsClient().components().show(new ShowRequest().setComponent((componentKey))).getComponent();
    } catch (org.sonarqube.ws.client.HttpException e) {
      if (e.code() == 404) {
        return null;
      }
      throw new IllegalStateException(e);
    }
  }

  public boolean exists(String projectKey) {
    try {
      Components.ShowWsResponse response = session.wsClient().components().show(new ShowRequest().setComponent(projectKey));
      return response.getComponent() != null;
    } catch (HttpException e) {
      if (e.code() == 404) {
        return false;
      }
      throw new IllegalStateException(e);
    }
  }

  public String generateKey() {
    int id = ID_GENERATOR.getAndIncrement();
    return "key" + id;
  }
}

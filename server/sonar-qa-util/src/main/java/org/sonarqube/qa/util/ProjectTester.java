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
package org.sonarqube.qa.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.DeleteRequest;
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
    service.search(new SearchRequest().setQualifiers(singletonList("TRK"))).getComponentsList().forEach(p ->
      service.delete(new DeleteRequest().setProject(p.getKey())));
  }

  @SafeVarargs
  public final Projects.CreateWsResponse.Project provision(Consumer<CreateRequest>... populators) {
    return provision(null, populators);
  }

  @SafeVarargs
  public final Projects.CreateWsResponse.Project provision(@Nullable Organizations.Organization organization, Consumer<CreateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setProject("key" + id)
      .setName("Name " + id)
      .setOrganization(organization != null ? organization.getKey() : null);
    stream(populators).forEach(p -> p.accept(request));

    return session.wsClient().projects().create(request).getProject();
  }
}

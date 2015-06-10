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
package org.sonar.server.issue.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;

import static com.google.common.collect.Maps.newHashMap;

/**
 * This class computes some collections of {@link ComponentDto}s used to serialize issues.
 */
public class IssueComponentHelper {

  private final DbClient dbClient;

  public IssueComponentHelper(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public Map<String, ComponentDto> prepareComponentsAndProjects(Set<String> projectUuids, Set<String> componentUuids, Map<String, ComponentDto> componentsByUuid,
      Collection<ComponentDto> componentDtos, List<ComponentDto> projectDtos, DbSession session) {
    Map<String, ComponentDto> projectsByComponentUuid;
    List<ComponentDto> fileDtos = dbClient.componentDao().selectByUuids(session, componentUuids);
    List<ComponentDto> subProjectDtos = dbClient.componentDao().selectSubProjectsByComponentUuids(session, componentUuids);
    componentDtos.addAll(fileDtos);
    componentDtos.addAll(subProjectDtos);
    for (ComponentDto component : componentDtos) {
      projectUuids.add(component.projectUuid());
    }
    projectDtos.addAll(dbClient.componentDao().selectByUuids(session, projectUuids));
    componentDtos.addAll(projectDtos);

    for (ComponentDto componentDto : componentDtos) {
      componentsByUuid.put(componentDto.uuid(), componentDto);
    }

    projectsByComponentUuid = getProjectsByComponentUuid(componentDtos, projectDtos);
    return projectsByComponentUuid;
  }

  private Map<String, ComponentDto> getProjectsByComponentUuid(Collection<ComponentDto> components, Collection<ComponentDto> projects) {
    Map<String, ComponentDto> projectsByUuid = buildProjectsByUuid(projects);
    return buildProjectsByComponentUuid(components, projectsByUuid);
  }

  private static Map<String, ComponentDto> buildProjectsByUuid(Collection<ComponentDto> projects) {
    Map<String, ComponentDto> projectsByUuid = newHashMap();
    for (ComponentDto project : projects) {
      if (project == null) {
        throw new IllegalStateException("Found a null project in issues");
      }
      if (project.uuid() == null) {
        throw new IllegalStateException("Project has no UUID: " + project.getKey());
      }
      projectsByUuid.put(project.uuid(), project);
    }
    return projectsByUuid;
  }

  private static Map<String, ComponentDto> buildProjectsByComponentUuid(Collection<ComponentDto> components, Map<String, ComponentDto> projectsByUuid) {
    Map<String, ComponentDto> projectsByComponentUuid = newHashMap();
    for (ComponentDto component : components) {
      if (component.uuid() == null) {
        throw new IllegalStateException("Component has no UUID: " + component.getKey());
      }
      if (!projectsByUuid.containsKey(component.projectUuid())) {
        throw new IllegalStateException("Project cannot be found for component: " + component.getKey() + " / " + component.uuid());
      }
      projectsByComponentUuid.put(component.uuid(), projectsByUuid.get(component.projectUuid()));
    }
    return projectsByComponentUuid;
  }
}

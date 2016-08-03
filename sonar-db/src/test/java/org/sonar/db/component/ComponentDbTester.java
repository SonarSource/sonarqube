/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.component;

import java.util.List;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ComponentDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public ComponentDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public SnapshotDto insertProjectAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(component));
    db.commit();

    return snapshot;
  }

  public SnapshotDto insertViewAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(component));
    db.commit();

    return snapshot;
  }

  public SnapshotDto insertDeveloperAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(component));
    db.commit();

    return snapshot;
  }

  public ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    db.commit();
    return component;
  }

  public ComponentDto insertProject() {
    ComponentDto project = newProjectDto();
    dbClient.componentDao().insert(dbSession, project);
    db.commit();

    return project;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(dbSession, asList(components));
    db.commit();
  }

  public void indexAllComponents() {
    ComponentQuery dbQuery = ComponentQuery.builder()
      .setQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV")
      .build();
    List<ComponentDto> rootProjects = dbClient.componentDao().selectByQuery(dbSession, dbQuery, 0, Integer.MAX_VALUE);
    for (ComponentDto project : rootProjects) {
      dbClient.componentIndexDao().indexProject(dbSession, project.uuid());
    }
    db.commit();
  }

  public void indexComponents(String... componentUuids) {
    for (String componentUuid : componentUuids) {
      dbClient.componentIndexDao().indexResource(dbSession, componentUuid);
    }
    db.commit();
  }
}

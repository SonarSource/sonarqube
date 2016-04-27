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

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.sonar.db.component.SnapshotTesting.createForComponent;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForDeveloper;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForView;

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
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForProject(component));
    db.commit();

    return snapshot;
  }

  public SnapshotDto insertViewAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForView(component));
    db.commit();

    return snapshot;
  }

  public SnapshotDto insertDeveloperAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, newSnapshotForDeveloper(component));
    db.commit();

    return snapshot;
  }

  public SnapshotDto insertComponentAndSnapshot(ComponentDto component, SnapshotDto parentSnapshot) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, createForComponent(component, parentSnapshot));
    db.commit();

    return snapshot;
  }

  public ComponentDto insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    db.commit();
    return component;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(dbSession, asList(components));
    db.commit();
  }

  public void indexProjects() {
    dbClient.componentIndexDao().indexProjects();
    db.commit();
  }
}

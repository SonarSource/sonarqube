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

package org.sonar.server.db.migrations.v50;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v50.Component;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.util.ProgressTask;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Used in the Active Record Migration 705
 *
 * @since 5.0
 */
public class PopulateProjectsUuidColumnsMigration implements DatabaseMigration {

  private final DbClient db;
  private final AtomicLong counter = new AtomicLong(0L);
  private final ProgressTask progressTask = new ProgressTask(counter, LoggerFactory.getLogger("DbMigration"));

  public PopulateProjectsUuidColumnsMigration(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    Timer timer = new Timer("Db Migration Progress");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);

    final DbSession readSession = db.openSession(false);
    final DbSession writeSession = db.openSession(true);
    try {
      readSession.select("org.sonar.core.persistence.migration.v50.Migration50Mapper.selectRootProjects", new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          Component project = (Component) context.getResultObject();
          Map<Long, String> uuidByComponentId = newHashMap();
          migrateEnabledComponents(readSession, writeSession, project, uuidByComponentId);
          migrateDisabledComponents(readSession, writeSession, project, uuidByComponentId);
        }
      });
      writeSession.commit();
      readSession.commit();

      migrateComponentsWithoutUuid(readSession, writeSession);
      writeSession.commit();

      // log the total number of process rows
      progressTask.log();
    } finally {
      readSession.close();
      writeSession.close();
      timer.cancel();
      timer.purge();
    }
  }

  private void migrateEnabledComponents(DbSession readSession, DbSession writeSession, Component project, Map<Long, String> uuidByComponentId) {
    Map<Long, Component> componentsBySnapshotId = newHashMap();

    List<Component> components = readSession.getMapper(Migration50Mapper.class).selectComponentChildrenForProjects(project.getId());
    components.add(project);
    List<Component> componentsToMigrate = newArrayList();
    for (Component component : components) {
      componentsBySnapshotId.put(component.getSnapshotId(), component);

      // Not migrate components already having an UUID
      if (component.getUuid() == null) {
        component.setUuid(getOrCreateUuid(component, uuidByComponentId));
        component.setProjectUuid(getOrCreateUuid(project, uuidByComponentId));
        componentsToMigrate.add(component);
      }
    }

    for (Component component : componentsToMigrate) {
      updateComponent(component, project, componentsBySnapshotId, uuidByComponentId);
      writeSession.getMapper(Migration50Mapper.class).updateComponentUuids(component);
      counter.getAndIncrement();
    }
  }

  private void migrateDisabledComponents(DbSession readSession, DbSession writeSession, Component project, Map<Long, String> uuidByComponentId) {
    String projectUuid = getOrCreateUuid(project, uuidByComponentId);
    for (Component component : readSession.getMapper(Migration50Mapper.class).selectDisabledDirectComponentChildrenForProjects(project.getId())) {
      component.setUuid(getOrCreateUuid(component, uuidByComponentId));
      component.setProjectUuid(projectUuid);

      writeSession.getMapper(Migration50Mapper.class).updateComponentUuids(component);
      counter.getAndIncrement();
    }
    for (Component component : readSession.getMapper(Migration50Mapper.class).selectDisabledNoneDirectComponentChildrenForProjects(project.getId())) {
      component.setUuid(getOrCreateUuid(component, uuidByComponentId));
      component.setProjectUuid(projectUuid);

      writeSession.getMapper(Migration50Mapper.class).updateComponentUuids(component);
      counter.getAndIncrement();
    }
  }

  private void updateComponent(Component component, Component project, Map<Long, Component> componentsBySnapshotId, Map<Long, String> uuidByComponentId) {
    String snapshotPath = component.getSnapshotPath();
    StringBuilder moduleUuidPath = new StringBuilder();
    Component lastModule = null;
    if (!Strings.isNullOrEmpty(snapshotPath)) {
      for (String s : Splitter.on(".").omitEmptyStrings().split(snapshotPath)) {
        Long snapshotId = Long.valueOf(s);
        Component currentComponent = componentsBySnapshotId.get(snapshotId);
        if (currentComponent.getScope().equals(Scopes.PROJECT)) {
          lastModule = currentComponent;
          moduleUuidPath.append(currentComponent.getUuid()).append(".");
        }
      }
    }
    if (moduleUuidPath.length() > 0) {
      // Remove last '.'
      moduleUuidPath.deleteCharAt(moduleUuidPath.length() - 1);
      component.setModuleUuidPath(moduleUuidPath.toString());
    }

    // Module UUID contains direct module of a component
    if (lastModule != null) {
      component.setModuleUuid(getOrCreateUuid(lastModule, uuidByComponentId));
    }
  }

  private void migrateComponentsWithoutUuid(DbSession readSession, DbSession writeSession) {
    for (Component component : readSession.getMapper(Migration50Mapper.class).selectComponentsWithoutUuid()) {
      String uuid = Uuids.create();
      component.setUuid(uuid);
      component.setProjectUuid(uuid);

      writeSession.getMapper(Migration50Mapper.class).updateComponentUuids(component);
      counter.getAndIncrement();
    }
  }

  private static String getOrCreateUuid(Component component, Map<Long, String> uuidByComponentId) {
    String existingUuid = component.getUuid();
    String uuid = existingUuid == null ? uuidByComponentId.get(component.getId()) : existingUuid;
    if (uuid == null) {
      String newUuid = Uuids.create();
      uuidByComponentId.put(component.getId(), newUuid);
      return newUuid;
    }
    return uuid;
  }

}

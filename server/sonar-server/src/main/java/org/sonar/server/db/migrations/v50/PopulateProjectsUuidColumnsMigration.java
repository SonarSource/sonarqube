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
import org.sonar.api.resources.Scopes;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v50.Component;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdate;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Used in the Active Record Migration 705
 *
 * @since 5.0
 */
public class PopulateProjectsUuidColumnsMigration implements DatabaseMigration {

  private final DbClient db;
  private final AtomicLong counter = new AtomicLong(0L);
  private final MassUpdate.ProgressTask progressTask = new MassUpdate.ProgressTask(counter);

  public PopulateProjectsUuidColumnsMigration(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    Timer timer = new Timer("Db Migration Progress");
    timer.schedule(progressTask, MassUpdate.ProgressTask.PERIOD_MS, MassUpdate.ProgressTask.PERIOD_MS);

    DbSession session = db.openSession(true);
    try {
      Migration50Mapper mapper = session.getMapper(Migration50Mapper.class);

      for (Component project : mapper.selectRootProjects()) {
        Map<Long, String> uuidByComponentId = newHashMap();
        migrateEnabledComponents(session, mapper, project, uuidByComponentId);
        migrateDisabledComponents(session, mapper, project, uuidByComponentId);
      }
      migrateComponentsWithoutUuid(session, mapper);

      session.commit();
      // log the total number of process rows
      progressTask.log();

    } finally {
      session.close();
      timer.cancel();
      timer.purge();
    }
  }

  private void migrateEnabledComponents(DbSession session, Migration50Mapper mapper, Component project, Map<Long, String> uuidByComponentId) {
    Map<Long, Component> componentsBySnapshotId = newHashMap();

    List<Component> components = mapper.selectComponentChildrenForProjects(project.getId());
    components.add(project);
    for (Component component : components) {
      componentsBySnapshotId.put(component.getSnapshotId(), component);

      component.setUuid(getOrCreateUuid(component, uuidByComponentId));
      component.setProjectUuid(getOrCreateUuid(project, uuidByComponentId));
    }

    for (Component component : components) {
      updateComponent(component, project, componentsBySnapshotId, uuidByComponentId);
      mapper.updateComponentUuids(component);
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
      moduleUuidPath.deleteCharAt(moduleUuidPath.length()-1);
      component.setModuleUuidPath(moduleUuidPath.toString());
    }

    // Module UUID contains direct module of a component
    if (lastModule != null) {
      component.setModuleUuid(getOrCreateUuid(lastModule, uuidByComponentId));
    }
  }

  private void migrateDisabledComponents(DbSession session, Migration50Mapper mapper, Component project, Map<Long, String> uuidByComponentId) {
    for (Component component : mapper.selectDisabledComponentChildrenForProjects(project.getId())) {
      component.setUuid(getOrCreateUuid(component, uuidByComponentId));
      component.setProjectUuid(getOrCreateUuid(project, uuidByComponentId));

      mapper.updateComponentUuids(component);
      counter.getAndIncrement();
    }
  }

  private void migrateComponentsWithoutUuid(DbSession session, Migration50Mapper mapper) {
    for (Component component : mapper.selectComponentsWithoutUuid()) {
      String uuid = UUID.randomUUID().toString();
      component.setUuid(uuid);

      mapper.updateComponentUuids(component);
      counter.getAndIncrement();
    }
  }

  private static String getOrCreateUuid(Component component, Map<Long, String> uuidByComponentId) {
    String existingUuid = component.getUuid();
    String uuid = existingUuid == null ? uuidByComponentId.get(component.getId()) : existingUuid;
    if (uuid == null) {
      String newUuid = UUID.randomUUID().toString();
      uuidByComponentId.put(component.getId(), newUuid);
      return newUuid;
    }
    return uuid;
  }

}

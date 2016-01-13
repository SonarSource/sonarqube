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
package org.sonar.db.version.v50;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.ProgressLogger;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.version.MigrationStep;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Used in the Active Record Migration 705
 *
 * @since 5.0
 */
public class PopulateProjectsUuidColumnsMigrationStep implements MigrationStep {

  private static final Logger LOG = Loggers.get(PopulateProjectsUuidColumnsMigrationStep.class);

  private final DbClient db;
  private final AtomicLong counter = new AtomicLong(0L);

  public PopulateProjectsUuidColumnsMigrationStep(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();

    final DbSession readSession = db.openSession(false);
    final DbSession writeSession = db.openSession(true);
    try {
      readSession.select("org.sonar.db.version.v50.Migration50Mapper.selectRootProjects", new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          Component project = (Component) context.getResultObject();
          List<Component> components = readSession.getMapper(Migration50Mapper.class).selectComponentChildrenForProjects(project.getId());
          MigrationContext migrationContext = new MigrationContext(readSession, writeSession, project, components);
          migrateEnabledComponents(migrationContext);
          migrateDisabledComponents(migrationContext);
        }
      });
      writeSession.commit(true);
      readSession.commit(true);

      migrateComponentsWithoutUuid(readSession, writeSession);
      writeSession.commit(true);

      // log the total number of process rows
      progress.log();
    } finally {
      readSession.close();
      writeSession.close();
      progress.stop();
    }
  }

  private void migrateEnabledComponents(MigrationContext migrationContext) {
    saveComponent(migrationContext.writeSession, migrationContext.project);
    for (Component component : migrationContext.componentsToMigrate) {
      migrationContext.updateComponent(component);
      if (Strings.isNullOrEmpty(component.getModuleUuidPath())) {
        LOG.warn(String.format("Ignoring component id '%s' because the module uuid path could not be created", component.getId()));
      } else {
        migrationContext.updateComponent(component);
        saveComponent(migrationContext.writeSession, component);
      }
    }
  }

  private void migrateDisabledComponents(MigrationContext migrationContext) {
    for (Component component : migrationContext.readSession.getMapper(Migration50Mapper.class).selectDisabledDirectComponentChildrenForProjects(migrationContext.project.getId())) {
      migrationContext.updateComponent(component);
      saveComponent(migrationContext.writeSession, component);
    }
    for (Component component : migrationContext.readSession.getMapper(Migration50Mapper.class).selectDisabledNoneDirectComponentChildrenForProjects(
      migrationContext.project.getId())) {
      migrationContext.updateComponent(component);
      saveComponent(migrationContext.writeSession, component);
    }
  }

  private void migrateComponentsWithoutUuid(DbSession readSession, DbSession writeSession) {
    for (Component component : readSession.getMapper(Migration50Mapper.class).selectComponentsWithoutUuid()) {
      String uuid = Uuids.create();
      component.setUuid(uuid);
      component.setProjectUuid(uuid);
      saveComponent(writeSession, component);
    }
  }

  private void saveComponent(DbSession writeSession, Component component) {
    writeSession.getMapper(Migration50Mapper.class).updateComponentUuids(component);
    counter.getAndIncrement();
  }

  private static class MigrationContext {
    private final DbSession readSession;
    private final DbSession writeSession;
    private final Component project;
    private final Map<Long, Component> componentsBySnapshotId = newHashMap();
    private final Map<Long, String> uuidByComponentId = newHashMap();
    private final List<Component> componentsToMigrate = newArrayList();

    private MigrationContext(DbSession readSession, DbSession writeSession, Component project, List<Component> components) {
      this.readSession = readSession;
      this.writeSession = writeSession;
      this.project = project;

      project.setUuid(getOrCreateUuid(project));
      project.setProjectUuid(project.getUuid());

      componentsBySnapshotId.put(project.getSnapshotId(), project);
      for (Component component : components) {
        componentsBySnapshotId.put(component.getSnapshotId(), component);
        if (component.getUuid() == null) {
          componentsToMigrate.add(component);
        }
      }
    }

    public void updateComponent(Component component) {
      component.setUuid(getOrCreateUuid(component));
      component.setProjectUuid(getOrCreateUuid(project));

      String snapshotPath = component.getSnapshotPath();
      StringBuilder moduleUuidPath = new StringBuilder();
      String lastModuleUuid = null;
      if (!Strings.isNullOrEmpty(snapshotPath)) {
        for (String s : Splitter.on(".").omitEmptyStrings().split(snapshotPath)) {
          Long snapshotId = Long.valueOf(s);
          Component currentComponent = componentsBySnapshotId.get(snapshotId);
          if (currentComponent != null && currentComponent.getScope().equals(Scopes.PROJECT)) {
            lastModuleUuid = getOrCreateUuid(currentComponent);
            moduleUuidPath.append(lastModuleUuid).append(".");
          }
        }
      }

      if (moduleUuidPath.length() > 0 && lastModuleUuid != null) {
        // Remove last '.'
        moduleUuidPath.deleteCharAt(moduleUuidPath.length() - 1);

        component.setModuleUuidPath(moduleUuidPath.toString());
        component.setModuleUuid(lastModuleUuid);
      }
    }

    private String getOrCreateUuid(Component component) {
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

}

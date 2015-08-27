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

package org.sonar.server.computation.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.core.util.Uuids;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.computation.component.TreeRootHolder;

/**
 * Read all components from the batch report and fill component UUID and key.
 * This step loads the tree of components from database. It does not insert or update database.
 */
public class FillComponentsStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;

  public FillComponentsStep(DbClient dbClient, BatchReportReader reportReader, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    BatchReport.Metadata reportMetadata = reportReader.readMetadata();
    String branch = reportMetadata.hasBranch() ? reportMetadata.getBranch() : null;
    BatchReport.Component reportProject = reportReader.readComponent(reportMetadata.getRootComponentRef());
    String projectKey = ComponentKeys.createKey(reportProject.getKey(), branch);
    UuidFactory uuidFactory = new UuidFactory(projectKey);

    // feed project information
    ComponentImpl root = (ComponentImpl) treeRootHolder.getRoot();
    root.setKey(projectKey);
    root.setUuid(uuidFactory.getOrCreateForKey(projectKey));

    processChildren(uuidFactory, root, root);
  }

  private void recursivelyProcessComponent(UuidFactory uuidFactory, ComponentImpl component, Component nearestModule) {
    switch (component.getType()) {
      case MODULE:
        processModule(uuidFactory, component);
        processChildren(uuidFactory, component, component);
        break;
      case DIRECTORY:
      case FILE:
        processDirectoryAndFile(uuidFactory, component, nearestModule);
        processChildren(uuidFactory, component, nearestModule);
        break;
      default:
        throw new IllegalStateException(String.format("Unsupported component type '%s'", component.getType()));
    }
  }

  private void processChildren(UuidFactory uuidFactory, Component component, Component nearestModule) {
    for (Component child : component.getChildren()) {
      recursivelyProcessComponent(uuidFactory, (ComponentImpl) child, nearestModule);
    }
  }

  private void processModule(UuidFactory uuidFactory, ComponentImpl component) {
    BatchReport.Metadata reportMetadata = reportReader.readMetadata();
    String branch = reportMetadata.hasBranch() ? reportMetadata.getBranch() : null;

    BatchReport.Component reportComponent = reportReader.readComponent(component.getReportAttributes().getRef());
    String key = ComponentKeys.createKey(reportComponent.getKey(), branch);
    component.setKey(key);
    component.setUuid(uuidFactory.getOrCreateForKey(key));
  }

  private void processDirectoryAndFile(UuidFactory uuidFactory, ComponentImpl component, Component nearestModule) {
    BatchReport.Component reportComponent = reportReader.readComponent(component.getReportAttributes().getRef());
    // TODO fail if path is null
    String key = ComponentKeys.createEffectiveKey(nearestModule.getKey(), reportComponent.getPath());
    component.setKey(key);
    component.setUuid(uuidFactory.getOrCreateForKey(key));
  }

  private class UuidFactory {
    private final Map<String, String> uuidsByKey = new HashMap<>();

    private UuidFactory(String projectKey) {
      DbSession session = dbClient.openSession(false);
      try {
        List<ComponentDto> components = dbClient.componentDao().selectAllComponentsFromProjectKey(session, projectKey);
        for (ComponentDto componentDto : components) {
          uuidsByKey.put(componentDto.getKey(), componentDto.uuid());
        }
      } finally {
        session.close();
      }
    }

    /**
     * Get UUID from database if it exists, else generate a new one
     */
    String getOrCreateForKey(String key) {
      String uuid = uuidsByKey.get(key);
      return (uuid == null) ? Uuids.create() : uuid;
    }
  }

  @Override
  public String getDescription() {
    return "Initialize components";
  }

}

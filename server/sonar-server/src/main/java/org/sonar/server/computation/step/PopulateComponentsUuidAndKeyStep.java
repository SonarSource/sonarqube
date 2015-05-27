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
import javax.annotation.Nullable;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.db.DbClient;

/**
 * Read all components from the batch report and set components uuid and key
 */
public class PopulateComponentsUuidAndKeyStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;

  public PopulateComponentsUuidAndKeyStep(DbClient dbClient, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      BatchReport.Metadata metadata = reportReader.readMetadata();
      String branch = metadata.hasBranch() ? metadata.getBranch() : null;
      BatchReport.Component project = reportReader.readComponent(metadata.getRootComponentRef());
      String projectKey = ComponentKeys.createKey(project.getKey(), branch);

      Map<String, String> componentUuidsByKey = new HashMap<>();
      List<ComponentDto> components = dbClient.componentDao().selectComponentsFromProjectKey(session, projectKey);
      for (ComponentDto componentDto : components) {
        componentUuidsByKey.put(componentDto.getKey(), componentDto.uuid());
      }

      ComponentContext componentContext = new ComponentContext(reportReader, componentUuidsByKey, branch);

      Component root = context.getRoot();
      processProject(componentContext, root, projectKey);
      processChildren(componentContext, root, root);
      session.commit();
    } finally {
      session.close();
    }
  }

  private void recursivelyProcessComponent(ComponentContext componentContext, Component component, Component module) {
    switch (component.getType()) {
      case MODULE:
        processModule(componentContext, component);
        processChildren(componentContext, component, component);
        break;
      case DIRECTORY:
      case FILE:
        processDirectoryAndFile(componentContext, component, module);
        processChildren(componentContext, component, module);
        break;
      default:
        throw new IllegalStateException(String.format("Unsupported component type '%s'", component.getType()));
    }
  }

  private void processChildren(ComponentContext componentContext, Component component, Component nearestModule) {
    for (Component child : component.getChildren()) {
      recursivelyProcessComponent(componentContext, child, nearestModule);
    }
  }

  private void processProject(ComponentContext componentContext, Component component, String projectKey) {
    feedComponent((ComponentImpl) component, projectKey, componentContext.componentUuidsByKey);
  }

  private void processModule(ComponentContext componentContext, Component component) {
    BatchReport.Component batchComponent = componentContext.reportReader.readComponent(component.getRef());
    String componentKey = ComponentKeys.createKey(batchComponent.getKey(), componentContext.branch);
    feedComponent((ComponentImpl) component, componentKey, componentContext.componentUuidsByKey);
  }

  private void processDirectoryAndFile(ComponentContext componentContext, Component component, Component module) {
    BatchReport.Component batchComponent = componentContext.reportReader.readComponent(component.getRef());
    // TODO fail if path is null
    String componentKey = ComponentKeys.createEffectiveKey(module.getKey(), batchComponent.getPath());
    feedComponent((ComponentImpl) component, componentKey, componentContext.componentUuidsByKey);
  }

  private void feedComponent(ComponentImpl component, String componentKey, Map<String, String> componentUuidByKey) {
    component.setKey(componentKey);

    String componentUuid = componentUuidByKey.get(componentKey);
    if (componentUuid == null) {
      component.setUuid(Uuids.create());
    } else {
      component.setUuid(componentUuid);
    }
  }

  private static class ComponentContext {
    private final BatchReportReader reportReader;
    private final Map<String, String> componentUuidsByKey;
    private final String branch;

    public ComponentContext(BatchReportReader reportReader, Map<String, String> componentUuidsByKey, @Nullable String branch) {
      this.reportReader = reportReader;
      this.componentUuidsByKey = componentUuidsByKey;
      this.branch = branch;
    }
  }

  @Override
  public String getDescription() {
    return "Feed components uuid";
  }

}

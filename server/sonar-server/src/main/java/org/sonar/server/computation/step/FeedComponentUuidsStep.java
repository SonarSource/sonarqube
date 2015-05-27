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

import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.db.DbClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read all components from the batch report and set components uuid and key
 */
public class FeedComponentUuidsStep implements ComputationStep {

  private final DbClient dbClient;

  public FeedComponentUuidsStep(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void execute(final ComputationContext context) {
    new ComponentDepthTraversalTypeAwareVisitor(context).visit(context.getRoot());
  }

  @Override
  public String getDescription() {
    return "Feed components uuid";
  }

  private class ComponentDepthTraversalTypeAwareVisitor extends DepthTraversalTypeAwareVisitor {

    private BatchReportReader reportReader;
    private Map<String, String> componentUuidByKey;
    private String branch;
    private Component nearestModule;

    public ComponentDepthTraversalTypeAwareVisitor(ComputationContext context) {
      super(Component.Type.FILE, Order.PRE_ORDER);
      this.componentUuidByKey = new HashMap<>();
      this.branch = context.getReportMetadata().hasBranch() ? context.getReportMetadata().getBranch() : null;
      this.reportReader = context.getReportReader();
      this.nearestModule = null;
    }

    @Override
    public void visitProject(Component project) {
      executeForProject(project);
      nearestModule = project;
    }

    @Override
    public void visitModule(Component module) {
      executeForModule(module);
      nearestModule = module;
    }

    @Override
    public void visitDirectory(Component directory) {
      executeForDirectoryAndFile(directory);
    }

    @Override
    public void visitFile(Component file) {
      executeForDirectoryAndFile(file);
    }

    private void executeForProject(Component component) {
      BatchReport.Component project = reportReader.readComponent(component.getRef());
      String projectKey = ComponentKeys.createKey(project.getKey(), branch);
      DbSession session = dbClient.openSession(false);
      try {
        List<ComponentDto> components = dbClient.componentDao().selectComponentsFromProjectKey(session, projectKey);
        for (ComponentDto componentDto : components) {
          componentUuidByKey.put(componentDto.getKey(), componentDto.uuid());
        }

        feedComponent((ComponentImpl) component, projectKey);
      } finally {
        session.close();
      }
    }

    private void executeForModule(Component component) {
      BatchReport.Component batchComponent = reportReader.readComponent(component.getRef());
      String componentKey = ComponentKeys.createKey(batchComponent.getKey(), branch);
      feedComponent((ComponentImpl) component, componentKey);
    }

    private void executeForDirectoryAndFile(Component component) {
      BatchReport.Component batchComponent = reportReader.readComponent(component.getRef());
      // TODO fail if path is null
      String componentKey = ComponentKeys.createEffectiveKey(nearestModule.getKey(), batchComponent.getPath());
      feedComponent((ComponentImpl) component, componentKey);
    }

    private void feedComponent(ComponentImpl projectImpl, String componentKey) {
      projectImpl.setKey(componentKey);

      String componentUuid = componentUuidByKey.get(componentKey);
      if (componentUuid == null) {
        projectImpl.setUuid(Uuids.create());
      } else {
        projectImpl.setUuid(componentUuid);
      }
    }
  }
}

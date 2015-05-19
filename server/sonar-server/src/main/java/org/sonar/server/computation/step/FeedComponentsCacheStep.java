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

import com.google.common.collect.Maps;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.db.DbClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To be finished
 */
public class FeedComponentsCacheStep implements ComputationStep {

  private final DbClient dbClient;

  public FeedComponentsCacheStep(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      List<ComponentDto> components = dbClient.componentDao().selectComponentsFromProjectUuid(session, context.getProject().uuid());
      Map<String, ComponentDto> componentDtosByKey = componentDtosByKey(components);
      Map<Integer, Component> componentsByRef = new HashMap<>();
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(context, rootComponentRef, context.getReportReader().readComponent(rootComponentRef), componentDtosByKey, componentsByRef);
    } finally {
      session.close();
    }
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef, BatchReport.Component nearestModule,
                                           Map<String, ComponentDto> componentDtosByKey, Map<Integer, Component> componentsByRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component reportComponent = reportReader.readComponent(componentRef);
    String componentKey = ComponentKeys.createKey(nearestModule.getKey(), reportComponent.getPath(), context.getReportMetadata().getBranch());

    Component component = new Component().setKey(componentKey);
    ComponentDto componentDto = componentDtosByKey.get(componentKey);
    if (componentDto == null) {
      component.setUuid(Uuids.create());
    } else {
      component.setId(component.getId());
      component.setKey(componentKey);
    }

    for (Integer childRef : reportComponent.getChildRefList()) {
      // If current component is not a module or a project, we need to keep the parent reference to the nearest module
      BatchReport.Component nextNearestModule = !reportComponent.getType().equals(Constants.ComponentType.PROJECT) && !reportComponent.getType().equals(Constants.ComponentType.MODULE) ?
        nearestModule : reportComponent;
      recursivelyProcessComponent(context, childRef, nextNearestModule, componentDtosByKey, componentsByRef);
    }
  }

  private Map<String, ComponentDto> componentDtosByKey(List<ComponentDto> components){
    return Maps.uniqueIndex(components, new NonNullInputFunction<ComponentDto, String>() {
      @Override
      public String doApply(ComponentDto input) {
        return input.key();
      }
    });
  }

  @Override
  public String getDescription() {
    return "Feed components cache";
  }
}

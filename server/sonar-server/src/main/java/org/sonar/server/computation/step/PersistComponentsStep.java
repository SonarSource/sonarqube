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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.db.DbClient;

public class PersistComponentsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbComponentsRefCache dbComponentsRefCache;

  public PersistComponentsStep(DbClient dbClient, DbComponentsRefCache dbComponentsRefCache) {
    this.dbClient = dbClient;
    this.dbComponentsRefCache = dbComponentsRefCache;
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      new ComponentDepthTraversalTypeAwareVisitor(session, context).visit(context.getRoot());
      session.commit();
    } finally {
      session.close();
    }
  }

  private class ComponentDepthTraversalTypeAwareVisitor extends DepthTraversalTypeAwareVisitor {

    private final DbSession session;
    private final BatchReportReader reportReader;
    private final Map<String, ComponentDto> componentDtosByKey;

    private Long projectId;
    private ComponentDto lastModule;

    public ComponentDepthTraversalTypeAwareVisitor(DbSession session, ComputationContext context) {
      super(Component.Type.FILE, Order.PRE_ORDER);
      this.session = session;
      this.reportReader = context.getReportReader();
      this.componentDtosByKey = new HashMap<>();
    }

    @Override
    public void visitProject(Component project) {
      List<ComponentDto> components = dbClient.componentDao().selectComponentsFromProjectKey(session, project.getKey());
      for (ComponentDto componentDto : components) {
        componentDtosByKey.put(componentDto.getKey(), componentDto);
      }

      BatchReport.Component reportComponent = reportReader.readComponent(project.getRef());
      ComponentDto componentDto = createComponentDto(reportComponent, project);

      componentDto.setScope(Scopes.PROJECT);
      componentDto.setQualifier(Qualifiers.PROJECT);
      componentDto.setName(reportComponent.getName());
      componentDto.setLongName(componentDto.name());
      if (reportComponent.hasDescription()) {
        componentDto.setDescription(reportComponent.getDescription());
      }
      componentDto.setProjectUuid(componentDto.uuid());
      componentDto.setModuleUuidPath(ComponentDto.MODULE_UUID_PATH_SEP + componentDto.uuid() + ComponentDto.MODULE_UUID_PATH_SEP);

      persistComponent(project.getRef(), componentDto);

      lastModule = componentDto;
      projectId = componentDto.getId();
    }

    @Override
    public void visitModule(Component module) {
      BatchReport.Component reportComponent = reportReader.readComponent(module.getRef());
      ComponentDto componentDto = createComponentDto(reportComponent, module);

      componentDto.setScope(Scopes.PROJECT);
      componentDto.setQualifier(Qualifiers.MODULE);
      componentDto.setName(reportComponent.getName());
      componentDto.setLongName(componentDto.name());
      if (reportComponent.hasDescription()) {
        componentDto.setDescription(reportComponent.getDescription());
      }
      componentDto.setParentProjectId(projectId);
      componentDto.setProjectUuid(lastModule.projectUuid());
      componentDto.setModuleUuid(lastModule.uuid());
      componentDto.setModuleUuidPath((lastModule.moduleUuidPath() + componentDto.uuid() + ComponentDto.MODULE_UUID_PATH_SEP));

      persistComponent(module.getRef(), componentDto);

      lastModule = componentDto;
    }

    @Override
    public void visitDirectory(Component directory) {
      BatchReport.Component reportComponent = reportReader.readComponent(directory.getRef());
      ComponentDto componentDto = createComponentDto(reportComponent, directory);

      componentDto.setScope(Scopes.DIRECTORY);
      componentDto.setQualifier(Qualifiers.DIRECTORY);
      componentDto.setName(reportComponent.getPath());
      componentDto.setLongName(reportComponent.getPath());
      if (reportComponent.hasPath()) {
        componentDto.setPath(reportComponent.getPath());
      }

      componentDto.setParentProjectId(lastModule.getId());
      componentDto.setProjectUuid(lastModule.projectUuid());
      componentDto.setModuleUuid(lastModule.uuid());
      componentDto.setModuleUuidPath(lastModule.moduleUuidPath());

      persistComponent(directory.getRef(), componentDto);
    }

    @Override
    public void visitFile(Component file) {
      BatchReport.Component reportComponent = reportReader.readComponent(file.getRef());
      ComponentDto componentDto = createComponentDto(reportComponent, file);

      componentDto.setScope(Scopes.FILE);
      componentDto.setQualifier(getFileQualifier(reportComponent));
      componentDto.setName(FilenameUtils.getName(reportComponent.getPath()));
      componentDto.setLongName(reportComponent.getPath());
      if (reportComponent.hasPath()) {
        componentDto.setPath(reportComponent.getPath());
      }
      if (reportComponent.hasLanguage()) {
        componentDto.setLanguage(reportComponent.getLanguage());
      }

      componentDto.setParentProjectId(lastModule.getId());
      componentDto.setProjectUuid(lastModule.projectUuid());
      componentDto.setModuleUuid(lastModule.uuid());
      componentDto.setModuleUuidPath(lastModule.moduleUuidPath());

      persistComponent(file.getRef(), componentDto);
    }

    private ComponentDto createComponentDto(BatchReport.Component reportComponent, Component component) {
      String componentKey = component.getKey();
      String componentUuid = component.getUuid();

      ComponentDto componentDto = new ComponentDto();
      componentDto.setUuid(componentUuid);
      componentDto.setKey(componentKey);
      componentDto.setDeprecatedKey(componentKey);
      componentDto.setEnabled(true);
      return componentDto;
    }

    private void persistComponent(int componentRef, ComponentDto componentDto) {
      ComponentDto existingComponent = componentDtosByKey.get(componentDto.getKey());
      if (existingComponent == null) {
        dbClient.componentDao().insert(session, componentDto);
      } else {
        componentDto.setId(existingComponent.getId());
        componentDto.setParentProjectId(existingComponent.parentProjectId());
        if (updateComponent(existingComponent, componentDto)) {
          dbClient.componentDao().update(session, componentDto);
        }
      }
      dbComponentsRefCache.addComponent(componentRef, new DbComponentsRefCache.DbComponent(componentDto.getId(), componentDto.getKey(), componentDto.uuid()));
    }

    private boolean updateComponent(ComponentDto existingComponent, ComponentDto newComponent) {
      boolean isUpdated = false;
      if (Scopes.PROJECT.equals(existingComponent.scope())) {
        if (!newComponent.name().equals(existingComponent.name())) {
          isUpdated = true;
        }
        if (!StringUtils.equals(existingComponent.description(), newComponent.description())) {
          isUpdated = true;
        }
      }

      if (!StringUtils.equals(existingComponent.moduleUuid(), newComponent.moduleUuid())) {
        isUpdated = true;
      }
      if (!existingComponent.moduleUuidPath().equals(newComponent.moduleUuidPath())) {
        isUpdated = true;
      }
      if (!ObjectUtils.equals(existingComponent.parentProjectId(), newComponent.parentProjectId())) {
        isUpdated = true;
      }
      return isUpdated;
    }
  }

  private static String getFileQualifier(BatchReport.Component reportComponent) {
    return reportComponent.getIsTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
  }

  @Override
  public String getDescription() {
    return "Feed components cache";
  }
}

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
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

public class PersistComponentsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbComponentsRefCache dbComponentsRefCache;
  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;

  public PersistComponentsStep(DbClient dbClient, DbComponentsRefCache dbComponentsRefCache, BatchReportReader reportReader, TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.dbComponentsRefCache = dbComponentsRefCache;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      Component root = treeRootHolder.getRoot();
      List<ComponentDto> components = dbClient.componentDao().selectComponentsFromProjectKey(session, root.getKey());
      Map<String, ComponentDto> componentDtosByKey = componentDtosByKey(components);
      ComponentContext componentContext = new ComponentContext(session, componentDtosByKey);

      ComponentDto projectDto = processProject(root, reportReader.readComponent(root.getRef()), componentContext);
      processChildren(componentContext, root, projectDto, projectDto);
      session.commit();
    } finally {
      session.close();
    }
  }

  private void recursivelyProcessComponent(ComponentContext componentContext, Component component, ComponentDto parentModule, ComponentDto project) {
    BatchReport.Component reportComponent = reportReader.readComponent(component.getRef());

    switch (component.getType()) {
      case MODULE:
        ComponentDto moduleDto = processModule(component, reportComponent, componentContext, parentModule, project.getId());
        processChildren(componentContext, component, moduleDto, project);
        break;
      case DIRECTORY:
        processDirectory(component, reportComponent, componentContext, parentModule, project.getId());
        processChildren(componentContext, component, parentModule, project);
        break;
      case FILE:
        processFile(component, reportComponent, componentContext, parentModule, project.getId());
        processChildren(componentContext, component, parentModule, project);
        break;
      default:
        throw new IllegalStateException(String.format("Unsupported component type '%s'", component.getType()));
    }
  }

  private void processChildren(ComponentContext componentContext, Component component, ComponentDto parentModule, ComponentDto project) {
    for (Component child : component.getChildren()) {
      recursivelyProcessComponent(componentContext, child, parentModule, project);
    }
  }

  public ComponentDto processProject(Component project, BatchReport.Component reportComponent, ComponentContext componentContext) {
    ComponentDto componentDto = createComponentDto(project);

    componentDto.setScope(Scopes.PROJECT);
    componentDto.setQualifier(Qualifiers.PROJECT);
    componentDto.setName(reportComponent.getName());
    componentDto.setLongName(componentDto.name());
    if (reportComponent.hasDescription()) {
      componentDto.setDescription(reportComponent.getDescription());
    }
    componentDto.setProjectUuid(componentDto.uuid());
    componentDto.setModuleUuidPath(ComponentDto.MODULE_UUID_PATH_SEP + componentDto.uuid() + ComponentDto.MODULE_UUID_PATH_SEP);

    return persistComponent(project.getRef(), componentDto, componentContext);
  }

  public ComponentDto processModule(Component module, BatchReport.Component reportComponent, ComponentContext componentContext, ComponentDto lastModule, long projectId) {
    ComponentDto componentDto = createComponentDto(module);

    componentDto.setScope(Scopes.PROJECT);
    componentDto.setQualifier(Qualifiers.MODULE);
    componentDto.setName(reportComponent.getName());
    componentDto.setLongName(componentDto.name());
    if (reportComponent.hasPath()) {
      componentDto.setPath(reportComponent.getPath());
    }
    if (reportComponent.hasDescription()) {
      componentDto.setDescription(reportComponent.getDescription());
    }
    componentDto.setParentProjectId(projectId);
    componentDto.setProjectUuid(lastModule.projectUuid());
    componentDto.setModuleUuid(lastModule.uuid());
    componentDto.setModuleUuidPath((lastModule.moduleUuidPath() + componentDto.uuid() + ComponentDto.MODULE_UUID_PATH_SEP));

    return persistComponent(module.getRef(), componentDto, componentContext);
  }

  public void processDirectory(Component directory, BatchReport.Component reportComponent, ComponentContext componentContext, ComponentDto lastModule, long projectId) {
    ComponentDto componentDto = createComponentDto(directory);

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

    persistComponent(directory.getRef(), componentDto, componentContext);
  }

  public void processFile(Component file, BatchReport.Component reportComponent, ComponentContext componentContext, ComponentDto lastModule, long projectId) {
    ComponentDto componentDto = createComponentDto(file);

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

    persistComponent(file.getRef(), componentDto, componentContext);
  }

  private ComponentDto createComponentDto(Component component) {
    String componentKey = component.getKey();
    String componentUuid = component.getUuid();

    ComponentDto componentDto = new ComponentDto();
    componentDto.setUuid(componentUuid);
    componentDto.setKey(componentKey);
    componentDto.setDeprecatedKey(componentKey);
    componentDto.setEnabled(true);
    return componentDto;
  }

  private ComponentDto persistComponent(int componentRef, ComponentDto componentDto, ComponentContext componentContext) {
    ComponentDto existingComponent = componentContext.componentDtosByKey.get(componentDto.getKey());
    if (existingComponent == null) {
      dbClient.componentDao().insert(componentContext.dbSession, componentDto);
      dbComponentsRefCache.addComponent(componentRef, new DbComponentsRefCache.DbComponent(componentDto.getId(), componentDto.getKey(), componentDto.uuid()));
      return componentDto;
    } else {
      if (updateComponent(existingComponent, componentDto)) {
        dbClient.componentDao().update(componentContext.dbSession, existingComponent);
      }
      dbComponentsRefCache.addComponent(componentRef, new DbComponentsRefCache.DbComponent(existingComponent.getId(), existingComponent.getKey(), existingComponent.uuid()));
      return existingComponent;
    }
  }

  private static boolean updateComponent(ComponentDto existingComponent, ComponentDto newComponent) {
    boolean isUpdated = false;
    if (!StringUtils.equals(existingComponent.name(), newComponent.name())) {
      existingComponent.setName(newComponent.name());
      isUpdated = true;
    }
    if (!StringUtils.equals(existingComponent.description(), newComponent.description())) {
      existingComponent.setDescription(newComponent.description());
      isUpdated = true;
    }
    if (!StringUtils.equals(existingComponent.path(), newComponent.path())) {
      existingComponent.setPath(newComponent.path());
      isUpdated = true;
    }
    if (!StringUtils.equals(existingComponent.moduleUuid(), newComponent.moduleUuid())) {
      existingComponent.setModuleUuid(newComponent.moduleUuid());
      isUpdated = true;
    }
    if (!existingComponent.moduleUuidPath().equals(newComponent.moduleUuidPath())) {
      existingComponent.setModuleUuidPath(newComponent.moduleUuidPath());
      isUpdated = true;
    }
    if (!ObjectUtils.equals(existingComponent.parentProjectId(), newComponent.parentProjectId())) {
      existingComponent.setParentProjectId(newComponent.parentProjectId());
      isUpdated = true;
    }
    return isUpdated;
  }

  private static String getFileQualifier(BatchReport.Component reportComponent) {
    return reportComponent.getIsTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
  }

  private Map<String, ComponentDto> componentDtosByKey(List<ComponentDto> components) {
    return Maps.uniqueIndex(components, new NonNullInputFunction<ComponentDto, String>() {
      @Override
      public String doApply(ComponentDto input) {
        return input.key();
      }
    });
  }

  private static class ComponentContext {
    private final Map<String, ComponentDto> componentDtosByKey;
    private final DbSession dbSession;

    public ComponentContext(DbSession dbSession, Map<String, ComponentDto> componentDtosByKey) {
      this.componentDtosByKey = componentDtosByKey;
      this.dbSession = dbSession;
    }
  }

  @Override
  public String getDescription() {
    return "Feed components cache";
  }
}

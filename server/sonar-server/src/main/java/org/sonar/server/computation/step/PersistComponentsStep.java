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
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

/**
 * Persist components
 * Also feed the components cache {@link DbIdsRepository} with component ids
 */
public class PersistComponentsStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final DbIdsRepository dbIdsRepository;
  private final System2 system2;

  public PersistComponentsStep(DbClient dbClient, TreeRootHolder treeRootHolder, BatchReportReader reportReader, DbIdsRepository dbIdsRepository, System2 system2) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.dbIdsRepository = dbIdsRepository;
    this.system2 = system2;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      org.sonar.server.computation.component.Component root = treeRootHolder.getRoot();
      List<ComponentDto> existingComponents = dbClient.componentDao().selectComponentsFromProjectKey(session, root.getKey());
      Map<String, ComponentDto> existingComponentDtosByKey = componentDtosByKey(existingComponents);
      PersisComponent persisComponent = new PersisComponent(session, existingComponentDtosByKey, reportReader);

      persisComponent.recursivelyProcessComponent(root, null);
      session.commit();
    } finally {
      session.close();
    }
  }

  private class PersisComponent {

    private final BatchReportReader reportReader;
    private final Map<String, ComponentDto> existingComponentDtosByKey;
    private final DbSession dbSession;

    private ComponentDto project;

    public PersisComponent(DbSession dbSession, Map<String, ComponentDto> existingComponentDtosByKey, BatchReportReader reportReader) {
      this.reportReader = reportReader;
      this.existingComponentDtosByKey = existingComponentDtosByKey;
      this.dbSession = dbSession;
    }

    private void recursivelyProcessComponent(Component component, @Nullable ComponentDto lastModule) {
      BatchReport.Component reportComponent = reportReader.readComponent(component.getRef());

      switch (component.getType()) {
        case PROJECT:
          this.project = processProject(component, reportComponent);
          processChildren(component, project);
          break;
        case MODULE:
          ComponentDto persistedModule = processModule(component, reportComponent, nonNullLastModule(lastModule));
          processChildren(component, persistedModule);
          break;
        case DIRECTORY:
          processDirectory(component, reportComponent, nonNullLastModule(lastModule));
          processChildren(component, nonNullLastModule(lastModule));
          break;
        case FILE:
          processFile(component, reportComponent, nonNullLastModule(lastModule));
          break;
        default:
          throw new IllegalStateException(String.format("Unsupported component type '%s'", component.getType()));
      }
    }

    private void processChildren(Component component, ComponentDto lastModule) {
      for (Component child : component.getChildren()) {
        recursivelyProcessComponent(child, lastModule);
      }
    }

    private ComponentDto nonNullLastModule(@Nullable ComponentDto lastModule) {
      return lastModule == null ? project : lastModule;
    }

    public ComponentDto processProject(Component project, BatchReport.Component reportComponent) {
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

      ComponentDto projectDto = persistComponent(project.getRef(), componentDto);
      addToCache(project, projectDto);
      return projectDto;
    }

    public ComponentDto processModule(Component module, BatchReport.Component reportComponent, ComponentDto lastModule) {
      ComponentDto componentDto = createComponentDto(reportComponent, module);

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
      componentDto.setParentProjectId(project.getId());
      componentDto.setProjectUuid(lastModule.projectUuid());
      componentDto.setModuleUuid(lastModule.uuid());
      componentDto.setModuleUuidPath(lastModule.moduleUuidPath() + componentDto.uuid() + ComponentDto.MODULE_UUID_PATH_SEP);

      ComponentDto moduleDto = persistComponent(module.getRef(), componentDto);
      addToCache(module, moduleDto);
      return moduleDto;
    }

    public ComponentDto processDirectory(org.sonar.server.computation.component.Component directory, BatchReport.Component reportComponent, ComponentDto lastModule) {
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

      ComponentDto directoryDto = persistComponent(directory.getRef(), componentDto);
      addToCache(directory, directoryDto);
      return directoryDto;
    }

    public void processFile(org.sonar.server.computation.component.Component file, BatchReport.Component reportComponent, ComponentDto lastModule) {
      ComponentDto componentDto = createComponentDto(reportComponent, file);

      componentDto.setScope(Scopes.FILE);
      componentDto.setQualifier(getFileQualifier(file));
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

      ComponentDto fileDto = persistComponent(file.getRef(), componentDto);
      addToCache(file, fileDto);
    }

    private ComponentDto createComponentDto(BatchReport.Component reportComponent, Component component) {
      String componentKey = component.getKey();
      String componentUuid = component.getUuid();

      ComponentDto componentDto = new ComponentDto();
      componentDto.setUuid(componentUuid);
      componentDto.setKey(componentKey);
      componentDto.setDeprecatedKey(componentKey);
      componentDto.setEnabled(true);
      componentDto.setCreatedAt(new Date(system2.now()));
      return componentDto;
    }

    private ComponentDto persistComponent(int componentRef, ComponentDto componentDto) {
      ComponentDto existingComponent = existingComponentDtosByKey.get(componentDto.getKey());
      if (existingComponent == null) {
        dbClient.componentDao().insert(dbSession, componentDto);
        return componentDto;
      } else {
        if (updateComponent(existingComponent, componentDto)) {
          dbClient.componentDao().update(dbSession, existingComponent);
        }
        return existingComponent;
      }
    }

    private void addToCache(Component component, ComponentDto componentDto) {
      dbIdsRepository.setComponentId(component, componentDto.getId());
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

  private static String getFileQualifier(Component component) {
    return component.isUnitTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
  }

  private static Map<String, ComponentDto> componentDtosByKey(List<ComponentDto> components) {
    return Maps.uniqueIndex(components, new NonNullInputFunction<ComponentDto, String>() {
      @Override
      public String doApply(ComponentDto input) {
        return input.key();
      }
    });
  }

  @Override
  public String getDescription() {
    return "Persist components";
  }
}

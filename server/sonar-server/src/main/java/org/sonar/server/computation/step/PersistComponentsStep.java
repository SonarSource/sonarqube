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
package org.sonar.server.computation.step;

import com.google.common.base.Predicate;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.MutableDbIdsRepository;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.PathAwareVisitor;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.component.TreeRootHolder;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.db.component.ComponentDto.MODULE_UUID_PATH_SEP;
import static org.sonar.db.component.ComponentDtoFunctions.toKey;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist report components
 * Also feed the components cache {@link DbIdsRepositoryImpl} with component ids
 */
public class PersistComponentsStep implements ComputationStep {
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final MutableDbIdsRepository dbIdsRepository;
  private final System2 system2;

  public PersistComponentsStep(DbClient dbClient, TreeRootHolder treeRootHolder, MutableDbIdsRepository dbIdsRepository, System2 system2) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.dbIdsRepository = dbIdsRepository;
    this.system2 = system2;
  }

  @Override
  public String getDescription() {
    return "Persist components";
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      Map<String, ComponentDto> existingComponentDtosByKey = indexExistingDtosByKey(session);
      new PathAwareCrawler<>(new PersistComponentStepsVisitor(existingComponentDtosByKey, session))
        .visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      dbClient.closeSession(session);
    }
  }

  private Map<String, ComponentDto> indexExistingDtosByKey(DbSession session) {
    return from(dbClient.componentDao().selectAllComponentsFromProjectKey(session, treeRootHolder.getRoot().getKey()))
      .uniqueIndex(toKey());
  }

  private class PersistComponentStepsVisitor extends PathAwareVisitorAdapter<ComponentDtoHolder> {

    private final Map<String, ComponentDto> existingComponentDtosByKey;
    private final DbSession dbSession;

    public PersistComponentStepsVisitor(Map<String, ComponentDto> existingComponentDtosByKey, DbSession dbSession) {
      super(
        CrawlerDepthLimit.LEAVES,
        PRE_ORDER,
        new SimpleStackElementFactory<ComponentDtoHolder>() {
          @Override
          public ComponentDtoHolder createForAny(Component component) {
            return new ComponentDtoHolder();
          }

          @Override
          public ComponentDtoHolder createForFile(Component file) {
            // no need to create holder for file since they are always leaves of the Component tree
            return null;
          }

          @Override
          public ComponentDtoHolder createForProjectView(Component projectView) {
            // no need to create holder for file since they are always leaves of the Component tree
            return null;
          }
        });
      this.existingComponentDtosByKey = existingComponentDtosByKey;
      this.dbSession = dbSession;
    }

    @Override
    public void visitProject(Component project, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForProject(project);
      path.current().setDto(persistAndPopulateCache(project, dto));
    }

    @Override
    public void visitModule(Component module, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForModule(module, path);
      path.current().setDto(persistAndPopulateCache(module, dto));
    }

    @Override
    public void visitDirectory(Component directory, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForDirectory(directory, path);
      path.current().setDto(persistAndPopulateCache(directory, dto));
    }

    @Override
    public void visitFile(Component file, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForFile(file, path);
      persistAndPopulateCache(file, dto);
    }

    @Override
    public void visitView(Component view, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForView(view);
      path.current().setDto(persistAndPopulateCache(view, dto));
    }

    @Override
    public void visitSubView(Component subView, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForSubView(subView, path);
      path.current().setDto(persistAndPopulateCache(subView, dto));
    }

    @Override
    public void visitProjectView(Component projectView, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForProjectView(projectView, path);
      persistAndPopulateCache(projectView, dto);
    }

    private ComponentDto persistAndPopulateCache(Component component, ComponentDto dto) {
      ComponentDto projectDto = persistComponent(dto);
      addToCache(component, projectDto);
      return projectDto;
    }

    private ComponentDto persistComponent(ComponentDto componentDto) {
      ComponentDto existingComponent = existingComponentDtosByKey.get(componentDto.getKey());
      if (existingComponent == null) {
        dbClient.componentDao().insert(dbSession, componentDto);
        return componentDto;
      } else {
        if (updateExisting(existingComponent, componentDto)) {
          dbClient.componentDao().update(dbSession, existingComponent);
        }
        return existingComponent;
      }
    }

    private void addToCache(Component component, ComponentDto componentDto) {
      dbIdsRepository.setComponentId(component, componentDto.getId());
    }
  }

  public ComponentDto createForProject(Component project) {
    ComponentDto res = createBase(project);

    res.setScope(Scopes.PROJECT);
    res.setQualifier(Qualifiers.PROJECT);
    res.setName(project.getName());
    res.setLongName(res.name());
    res.setDescription(project.getDescription());
    res.setProjectUuid(res.uuid());
    res.setModuleUuidPath(MODULE_UUID_PATH_SEP + res.uuid() + MODULE_UUID_PATH_SEP);

    return res;
  }

  public ComponentDto createForModule(Component module, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto res = createBase(module);

    res.setScope(Scopes.PROJECT);
    res.setQualifier(Qualifiers.MODULE);
    res.setName(module.getName());
    res.setLongName(res.name());
    res.setPath(module.getReportAttributes().getPath());
    res.setDescription(module.getDescription());

    setRootAndParentModule(res, path);

    return res;
  }

  public ComponentDto createForDirectory(Component directory, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto res = createBase(directory);

    res.setScope(Scopes.DIRECTORY);
    res.setQualifier(Qualifiers.DIRECTORY);
    res.setName(directory.getReportAttributes().getPath());
    res.setLongName(directory.getReportAttributes().getPath());
    res.setPath(directory.getReportAttributes().getPath());

    setParentModuleProperties(res, path);

    return res;
  }

  public ComponentDto createForFile(Component file, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto res = createBase(file);

    res.setScope(Scopes.FILE);
    res.setQualifier(getFileQualifier(file));
    res.setName(FilenameUtils.getName(file.getReportAttributes().getPath()));
    res.setLongName(file.getReportAttributes().getPath());
    res.setPath(file.getReportAttributes().getPath());
    res.setLanguage(file.getFileAttributes().getLanguageKey());

    setParentModuleProperties(res, path);

    return res;
  }

  private ComponentDto createForView(Component view) {
    ComponentDto res = createBase(view);

    res.setScope(Scopes.PROJECT);
    res.setQualifier(Qualifiers.VIEW);
    res.setName(view.getName());
    res.setDescription(view.getDescription());
    res.setLongName(res.name());
    res.setProjectUuid(res.uuid());
    res.setModuleUuidPath(MODULE_UUID_PATH_SEP + res.uuid() + MODULE_UUID_PATH_SEP);

    return res;
  }

  private ComponentDto createForSubView(Component subView, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto res = createBase(subView);

    res.setScope(Scopes.PROJECT);
    res.setQualifier(Qualifiers.SUBVIEW);
    res.setName(subView.getName());
    res.setDescription(subView.getDescription());
    res.setLongName(res.name());

    setRootAndParentModule(res, path);

    return res;
  }

  private ComponentDto createForProjectView(Component projectView, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto res = createBase(projectView);

    res.setScope(Scopes.FILE);
    res.setQualifier(Qualifiers.PROJECT);
    res.setName(projectView.getName());
    res.setLongName(res.name());
    res.setCopyResourceId(projectView.getProjectViewAttributes().getProjectId());

    setRootAndParentModule(res, path);

    return res;
  }

  private ComponentDto createBase(Component component) {
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

  /**
   * Applies to a node of type either MODULE, SUBVIEW, PROJECT_VIEW
   */
  private static void setRootAndParentModule(ComponentDto res, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto projectDto = from(path.getCurrentPath()).last().get().getElement().getDto();
    res.setParentProjectId(projectDto.getId());
    res.setProjectUuid(projectDto.uuid());

    ComponentDto parentModule = path.parent().getDto();
    res.setModuleUuid(parentModule.uuid());
    res.setModuleUuidPath(parentModule.moduleUuidPath() + res.uuid() + MODULE_UUID_PATH_SEP);
  }

  /**
   * Applies to a node of type either DIRECTORY or FILE
   */
  private static void setParentModuleProperties(ComponentDto componentDto, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    ComponentDto parentModule = from(path.getCurrentPath())
      .filter(ParentModulePathElement.INSTANCE)
      .first()
      .get()
      .getElement().getDto();
    componentDto.setParentProjectId(parentModule.getId());
    componentDto.setProjectUuid(parentModule.projectUuid());
    componentDto.setModuleUuid(parentModule.uuid());
    componentDto.setModuleUuidPath(parentModule.moduleUuidPath());
  }

  private static boolean updateExisting(ComponentDto existingComponent, ComponentDto newComponent) {
    boolean modified = false;
    if (!StringUtils.equals(existingComponent.name(), newComponent.name())) {
      existingComponent.setName(newComponent.name());
      modified = true;
    }
    if (!StringUtils.equals(existingComponent.longName(), newComponent.longName())) {
      existingComponent.setLongName(newComponent.longName());
      modified = true;
    }
    if (!StringUtils.equals(existingComponent.description(), newComponent.description())) {
      existingComponent.setDescription(newComponent.description());
      modified = true;
    }
    if (!StringUtils.equals(existingComponent.path(), newComponent.path())) {
      existingComponent.setPath(newComponent.path());
      modified = true;
    }
    if (!StringUtils.equals(existingComponent.moduleUuid(), newComponent.moduleUuid())) {
      existingComponent.setModuleUuid(newComponent.moduleUuid());
      modified = true;
    }
    if (!existingComponent.moduleUuidPath().equals(newComponent.moduleUuidPath())) {
      existingComponent.setModuleUuidPath(newComponent.moduleUuidPath());
      modified = true;
    }
    if (!ObjectUtils.equals(existingComponent.parentProjectId(), newComponent.parentProjectId())) {
      existingComponent.setParentProjectId(newComponent.parentProjectId());
      modified = true;
    }
    if (!ObjectUtils.equals(existingComponent.getCopyResourceId(), newComponent.getCopyResourceId())) {
      existingComponent.setCopyResourceId(newComponent.getCopyResourceId());
      modified = true;
    }
    if (!existingComponent.isEnabled()) {
      // If component was previously removed, re-enable it
      existingComponent.setEnabled(true);
      modified = true;
    }
    return modified;
  }

  private static String getFileQualifier(Component component) {
    return component.getFileAttributes().isUnitTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
  }

  private static class ComponentDtoHolder {
    private ComponentDto dto;

    public ComponentDto getDto() {
      return dto;
    }

    public void setDto(ComponentDto dto) {
      this.dto = dto;
    }
  }

  private enum ParentModulePathElement implements Predicate<PathAwareVisitor.PathElement<ComponentDtoHolder>> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull PathAwareVisitor.PathElement<ComponentDtoHolder> input) {
      return input.getComponent().getType() == Component.Type.MODULE
        || input.getComponent().getType() == Component.Type.PROJECT;
    }
  }

}

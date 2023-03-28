/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchPersister;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitor;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.component.ProjectPersister;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentUpdateDto;

import static java.util.Optional.ofNullable;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.ComponentDto.formatUuidPathFromParent;

/**
 * Persist report components
 */
public class PersistComponentsStep implements ComputationStep {
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final System2 system2;
  private final MutableDisabledComponentsHolder disabledComponentsHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final BranchPersister branchPersister;
  private final ProjectPersister projectPersister;

  public PersistComponentsStep(DbClient dbClient, TreeRootHolder treeRootHolder, System2 system2,
    MutableDisabledComponentsHolder disabledComponentsHolder, AnalysisMetadataHolder analysisMetadataHolder,
    BranchPersister branchPersister, ProjectPersister projectPersister) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.system2 = system2;
    this.disabledComponentsHolder = disabledComponentsHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.branchPersister = branchPersister;
    this.projectPersister = projectPersister;
  }

  @Override
  public String getDescription() {
    return "Persist components";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      branchPersister.persist(dbSession);
      projectPersister.persist(dbSession);

      String projectUuid = treeRootHolder.getRoot().getUuid();

      // safeguard, reset all rows to b-changed=false
      dbClient.componentDao().resetBChangedForRootComponentUuid(dbSession, projectUuid);

      Map<String, ComponentDto> existingDtosByUuids = indexExistingDtosByUuids(dbSession);
      boolean isRootPrivate = isRootPrivate(treeRootHolder.getRoot(), existingDtosByUuids);
      String mainBranchProjectUuid = loadProjectUuidOfMainBranch();

      // Insert or update the components in database. They are removed from existingDtosByUuids
      // at the same time.
      new PathAwareCrawler<>(new PersistComponentStepsVisitor(existingDtosByUuids, dbSession, mainBranchProjectUuid))
        .visit(treeRootHolder.getRoot());

      disableRemainingComponents(dbSession, existingDtosByUuids.values());
      dbClient.componentDao().setPrivateForRootComponentUuidWithoutAudit(dbSession, projectUuid, isRootPrivate);
      dbSession.commit();
    }
  }

  /**
   * See {@link ComponentDto#mainBranchProjectUuid} : value is null on main branches, otherwise it is
   * the uuid of the main branch.
   */
  @CheckForNull
  private String loadProjectUuidOfMainBranch() {
    if (!analysisMetadataHolder.getBranch().isMain()) {
      return analysisMetadataHolder.getProject().getUuid();
    }
    return null;
  }

  private void disableRemainingComponents(DbSession dbSession, Collection<ComponentDto> dtos) {
    Set<String> uuids = dtos.stream()
      .filter(ComponentDto::isEnabled)
      .map(ComponentDto::uuid)
      .collect(MoreCollectors.toSet(dtos.size()));
    dbClient.componentDao().updateBEnabledToFalse(dbSession, uuids);
    disabledComponentsHolder.setUuids(uuids);
  }

  private static boolean isRootPrivate(Component root, Map<String, ComponentDto> existingDtosByUuids) {
    ComponentDto rootDto = existingDtosByUuids.get(root.getUuid());
    if (rootDto == null) {
      if (Component.Type.VIEW == root.getType()) {
        return false;
      }
      throw new IllegalStateException(String.format("The project '%s' is not stored in the database, during a project analysis.", root.getKey()));
    }
    return rootDto.isPrivate();
  }

  /**
   * Returns a mutable map of the components currently persisted in database for the project, including
   * disabled components.
   */
  private Map<String, ComponentDto> indexExistingDtosByUuids(DbSession session) {
    return dbClient.componentDao().selectByBranchUuid(treeRootHolder.getRoot().getUuid(), session)
      .stream()
      .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
  }

  private class PersistComponentStepsVisitor extends PathAwareVisitorAdapter<ComponentDtoHolder> {

    private final Map<String, ComponentDto> existingComponentDtosByUuids;
    private final DbSession dbSession;
    @Nullable
    private final String mainBranchProjectUuid;

    PersistComponentStepsVisitor(Map<String, ComponentDto> existingComponentDtosByUuids, DbSession dbSession, @Nullable String mainBranchProjectUuid) {
      super(
        CrawlerDepthLimit.LEAVES,
        PRE_ORDER,
        new SimpleStackElementFactory<>() {
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
      this.existingComponentDtosByUuids = existingComponentDtosByUuids;
      this.dbSession = dbSession;
      this.mainBranchProjectUuid = mainBranchProjectUuid;
    }

    @Override
    public void visitProject(Component project, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForProject(project);
      path.current().setDto(persistComponent(dto));
    }

    @Override
    public void visitDirectory(Component directory, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForDirectory(directory, path);
      path.current().setDto(persistComponent(dto));
    }

    @Override
    public void visitFile(Component file, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForFile(file, path);
      persistComponent(dto);
    }

    @Override
    public void visitView(Component view, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForView(view);
      path.current().setDto(persistComponent(dto));
    }

    @Override
    public void visitSubView(Component subView, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForSubView(subView, path);
      path.current().setDto(persistComponent(dto));
    }

    @Override
    public void visitProjectView(Component projectView, Path<ComponentDtoHolder> path) {
      ComponentDto dto = createForProjectView(projectView, path);
      persistComponent(dto);
    }

    private ComponentDto persistComponent(ComponentDto componentDto) {
      ComponentDto existingComponent = existingComponentDtosByUuids.remove(componentDto.uuid());
      if (existingComponent == null) {
        dbClient.componentDao().insert(dbSession, componentDto);
        return componentDto;
      }
      Optional<ComponentUpdateDto> update = compareForUpdate(existingComponent, componentDto);
      if (update.isPresent()) {
        ComponentUpdateDto updateDto = update.get();
        dbClient.componentDao().update(dbSession, updateDto, componentDto.qualifier());

        // update the fields in memory in order the PathAwareVisitor.Path
        // to be up-to-date
        existingComponent.setKey(updateDto.getBKey());
        existingComponent.setCopyComponentUuid(updateDto.getBCopyComponentUuid());
        existingComponent.setDescription(updateDto.getBDescription());
        existingComponent.setEnabled(updateDto.isBEnabled());
        existingComponent.setUuidPath(updateDto.getBUuidPath());
        existingComponent.setLanguage(updateDto.getBLanguage());
        existingComponent.setLongName(updateDto.getBLongName());
        existingComponent.setModuleUuid(updateDto.getBModuleUuid());
        existingComponent.setModuleUuidPath(updateDto.getBModuleUuidPath());
        existingComponent.setName(updateDto.getBName());
        existingComponent.setPath(updateDto.getBPath());
        // We don't have a b_scope. The applyBChangesForRootComponentUuid query is using a case ... when to infer scope from the qualifier
        existingComponent.setScope(componentDto.scope());
        existingComponent.setQualifier(updateDto.getBQualifier());
      }
      return existingComponent;
    }

    public ComponentDto createForProject(Component project) {
      ComponentDto res = createBase(project);

      res.setScope(Scopes.PROJECT);
      res.setQualifier(PROJECT);
      res.setName(project.getName());
      res.setLongName(res.name());
      res.setDescription(project.getDescription());

      res.setBranchUuid(res.uuid());
      res.setRootUuid(res.uuid());
      res.setUuidPath(UUID_PATH_OF_ROOT);
      res.setModuleUuidPath(UUID_PATH_SEPARATOR + res.uuid() + UUID_PATH_SEPARATOR);

      return res;
    }

    public ComponentDto createForDirectory(Component directory, PathAwareVisitor.Path<ComponentDtoHolder> path) {
      ComponentDto res = createBase(directory);

      res.setScope(Scopes.DIRECTORY);
      res.setQualifier(Qualifiers.DIRECTORY);
      res.setName(directory.getShortName());
      res.setLongName(directory.getName());
      res.setPath(directory.getName());

      setParentModuleProperties(res, path);

      return res;
    }

    public ComponentDto createForFile(Component file, PathAwareVisitor.Path<ComponentDtoHolder> path) {
      ComponentDto res = createBase(file);

      res.setScope(Scopes.FILE);
      res.setQualifier(getFileQualifier(file));
      res.setName(file.getShortName());
      res.setLongName(file.getName());
      res.setPath(file.getName());
      res.setLanguage(file.getFileAttributes().getLanguageKey());

      setParentModuleProperties(res, path);

      return res;
    }

    private ComponentDto createForView(Component view) {
      ComponentDto res = createBase(view);

      res.setScope(Scopes.PROJECT);
      res.setQualifier(view.getViewAttributes().getType().getQualifier());
      res.setName(view.getName());
      res.setDescription(view.getDescription());
      res.setLongName(res.name());

      res.setBranchUuid(res.uuid());
      res.setRootUuid(res.uuid());
      res.setUuidPath(UUID_PATH_OF_ROOT);
      res.setModuleUuidPath(UUID_PATH_SEPARATOR + res.uuid() + UUID_PATH_SEPARATOR);

      return res;
    }

    private ComponentDto createForSubView(Component subView, PathAwareVisitor.Path<ComponentDtoHolder> path) {
      ComponentDto res = createBase(subView);

      res.setScope(Scopes.PROJECT);
      res.setQualifier(Qualifiers.SUBVIEW);
      res.setName(subView.getName());
      res.setDescription(subView.getDescription());
      res.setLongName(res.name());
      res.setCopyComponentUuid(subView.getSubViewAttributes().getOriginalViewUuid());

      setRootAndParentModule(res, path);

      return res;
    }

    private ComponentDto createForProjectView(Component projectView, PathAwareVisitor.Path<ComponentDtoHolder> path) {
      ComponentDto res = createBase(projectView);

      res.setScope(Scopes.FILE);
      res.setQualifier(PROJECT);
      res.setName(projectView.getName());
      res.setLongName(res.name());
      res.setCopyComponentUuid(projectView.getProjectViewAttributes().getUuid());

      setRootAndParentModule(res, path);

      return res;
    }

    private ComponentDto createBase(Component component) {
      String componentKey = component.getKey();
      String componentUuid = component.getUuid();

      ComponentDto componentDto = new ComponentDto();
      componentDto.setOrganizationUuid(analysisMetadataHolder.getOrganization().getUuid());
      componentDto.setUuid(componentUuid);
      componentDto.setKey(componentKey);
      componentDto.setMainBranchProjectUuid(mainBranchProjectUuid);
      componentDto.setEnabled(true);
      componentDto.setCreatedAt(new Date(system2.now()));

      return componentDto;
    }

    /**
     * Applies to a node of type either MODULE, SUBVIEW, PROJECT_VIEW
     */
    private void setRootAndParentModule(ComponentDto res, PathAwareVisitor.Path<ComponentDtoHolder> path) {
      ComponentDto rootDto = path.root().getDto();
      res.setRootUuid(rootDto.uuid());
      res.setBranchUuid(rootDto.uuid());

      ComponentDto parentModule = path.parent().getDto();
      res.setUuidPath(formatUuidPathFromParent(parentModule));
      res.setModuleUuid(parentModule.uuid());
      res.setModuleUuidPath(parentModule.moduleUuidPath() + res.uuid() + UUID_PATH_SEPARATOR);
    }
  }

  /**
   * Applies to a node of type either DIRECTORY or FILE
   */
  private static void setParentModuleProperties(ComponentDto componentDto, PathAwareVisitor.Path<ComponentDtoHolder> path) {
    componentDto.setBranchUuid(path.root().getDto().uuid());

    ComponentDto parentModule = StreamSupport.stream(path.getCurrentPath().spliterator(), false)
      .filter(p -> p.component().getType() == Component.Type.PROJECT)
      .findFirst()
      .get()
      .element().getDto();
    componentDto.setUuidPath(formatUuidPathFromParent(path.parent().getDto()));
    componentDto.setRootUuid(parentModule.uuid());
    componentDto.setModuleUuid(parentModule.uuid());
    componentDto.setModuleUuidPath(parentModule.moduleUuidPath());

  }

  private static Optional<ComponentUpdateDto> compareForUpdate(ComponentDto existing, ComponentDto target) {
    boolean hasDifferences = !StringUtils.equals(existing.getCopyComponentUuid(), target.getCopyComponentUuid()) ||
      !StringUtils.equals(existing.description(), target.description()) ||
      !StringUtils.equals(existing.getKey(), target.getKey()) ||
      !existing.isEnabled() ||
      !StringUtils.equals(existing.getUuidPath(), target.getUuidPath()) ||
      !StringUtils.equals(existing.language(), target.language()) ||
      !StringUtils.equals(existing.longName(), target.longName()) ||
      !StringUtils.equals(existing.moduleUuid(), target.moduleUuid()) ||
      !StringUtils.equals(existing.moduleUuidPath(), target.moduleUuidPath()) ||
      !StringUtils.equals(existing.name(), target.name()) ||
      !StringUtils.equals(existing.path(), target.path()) ||
      !StringUtils.equals(existing.scope(), target.scope()) ||
      !StringUtils.equals(existing.qualifier(), target.qualifier());

    ComponentUpdateDto update = null;
    if (hasDifferences) {
      update = ComponentUpdateDto
        .copyFrom(target)
        .setBChanged(true);
    }
    return ofNullable(update);
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
}

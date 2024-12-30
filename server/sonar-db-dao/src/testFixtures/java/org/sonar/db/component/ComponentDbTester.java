/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.portfolio.PortfolioProjectDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.NONE;

public class ComponentDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ComponentDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public SnapshotDto insertProjectAndSnapshot(ComponentDto component) {
    insertComponentAndBranchAndProject(component, null, defaults(), defaults(), defaults());
    return insertSnapshot(component);
  }

  public ProjectData insertProjectDataAndSnapshot(ComponentDto component) {
    ProjectData projectData = insertComponentAndBranchAndProject(component, null, defaults(), defaults(), defaults());
    insertSnapshot(component);
    return projectData;
  }

  public SnapshotDto insertPortfolioAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(db.getSession(), component, true);
    return insertSnapshot(component);
  }

  public ComponentDto insertComponent(ComponentDto component) {
    return insertComponentImpl(component, null, defaults());
  }

  public ProjectData insertPrivateProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true,
      defaults(), defaults(), defaults());
  }

  public ProjectData insertPrivateProjectWithCreationMethod(CreationMethod creationMethod) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true,
      defaults(), defaults(), projectDto -> projectDto.setCreationMethod(creationMethod));
  }

  public BranchDto getBranchDto(ComponentDto branch) {
    return db.getDbClient().branchDao().selectByUuid(db.getSession(), branch.uuid())
      .orElseThrow(() -> new IllegalStateException("Project has invalid configuration"));
  }

  public ProjectDto getProjectDtoByMainBranch(ComponentDto mainBranch) {
    return db.getDbClient().projectDao().selectByBranchUuid(db.getSession(), mainBranch.uuid())
      .orElseThrow(() -> new IllegalStateException("Project has invalid configuration"));
  }

  public ComponentDto getComponentDto(ProjectDto project) {
    BranchDto branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).get();
    return db.getDbClient().componentDao().selectByUuid(db.getSession(), branchDto.getUuid())
      .orElseThrow(() -> new IllegalStateException("Can't find project"));
  }

  public ComponentDto getComponentDto(BranchDto branch) {
    return db.getDbClient().componentDao().selectByUuid(db.getSession(), branch.getUuid())
      .orElseThrow(() -> new IllegalStateException("Can't find branch"));
  }

  public ProjectData insertPrivateProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, true);
  }

  public ProjectData insertPublicProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false);
  }

  public ProjectData insertPublicProject(String uuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), defaults(), p -> p.setUuid(uuid));
  }

  public ProjectData insertPublicProject(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), dtoPopulator, p -> p.setUuid(uuid));
  }

  public ProjectData insertPublicProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, false);
  }

  public ProjectData insertPrivateProject(String projectUuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, defaults(), defaults(), p -> p.setUuid(projectUuid));
  }

  public ProjectData insertPrivateProject(String projectUuid, ComponentDto mainBranch) {
    return insertComponentAndBranchAndProject(mainBranch, true, defaults(), defaults(), p -> p.setUuid(projectUuid));
  }

  public final ProjectData insertPrivateProject(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, defaults(), dtoPopulator);
  }

  public final ProjectData insertPrivateProject(Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(),
      true, defaults(), componentDtoPopulator, projectDtoPopulator);
  }

  public final ProjectData insertPublicProject(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), dtoPopulator);
  }

  public final ProjectData insertPublicProject(Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, defaults(), componentDtoPopulator, projectDtoPopulator);
  }

  public ProjectData insertPrivateProject(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    return insertPrivateProjectWithCustomBranch(branchPopulator, componentDtoPopulator, projectDtoPopulator);
  }

  public final ComponentDto insertFile(BranchDto branch) {
    ComponentDto projectComponent = getComponentDto(branch);
    return insertComponent(ComponentTesting.newFileDto(projectComponent));
  }

  public final ComponentDto insertFile(ComponentDto projectComponent) {
    return insertComponent(ComponentTesting.newFileDto(projectComponent));
  }

  public final ProjectData insertPrivateProject(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, defaults(), dtoPopulator, p -> p.setUuid(uuid));

  }

  public final ProjectData insertPrivateProjectWithCustomBranch(String branchKey) {
    return insertPrivateProjectWithCustomBranch(b -> b.setBranchType(BRANCH).setKey(branchKey), defaults());
  }

  public final ProjectData insertPrivateProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, branchPopulator, componentPopulator);
  }

  public final ProjectData insertPublicProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(), false, branchPopulator, componentPopulator);
  }

  public final ProjectData insertPrivateProjectWithCustomBranch(Consumer<BranchDto> branchPopulator, Consumer<ComponentDto> componentPopulator,
    Consumer<ProjectDto> projectPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, branchPopulator, componentPopulator, projectPopulator);
  }

  public ProjectData insertProjectWithAiCode() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(), true, defaults(), defaults(), p -> p.setContainsAiCode(true));
  }


  public final ComponentDto insertPublicPortfolio() {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, defaults(), defaults());
  }

  public final ComponentDto insertPublicPortfolio(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, defaults());
  }

  public final ComponentDto insertPublicPortfolio(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, defaults());
  }

  public final ComponentDto insertPublicPortfolio(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, portfolioPopulator);
  }

  public final PortfolioDto insertPublicPortfolioDto() {
    return insertPublicPortfolioDto(defaults());
  }

  public final PortfolioDto insertPublicPortfolioDto(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(String uuid) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, defaults(), defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(String uuid, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, defaults(), portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto() {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, defaults(), defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioData insertPrivatePortfolioData() {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, defaults(), defaults());
    PortfolioDto portfolioDto = getPortfolioDto(component);
    return new PortfolioData(portfolioDto, component);
  }

  public final PortfolioData insertPrivatePortfolioData(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, defaults());
    PortfolioDto portfolioDto = getPortfolioDto(component);
    return new PortfolioData(portfolioDto, component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPrivatePortfolioDto(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(String uuid, Consumer<ComponentDto> dtoPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, defaults());
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(String uuid, Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(false), false, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public final PortfolioDto insertPublicPortfolioDto(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    ComponentDto component = insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(false), false, dtoPopulator, portfolioPopulator);
    return getPortfolioDto(component);
  }

  public PortfolioDto getPortfolioDto(ComponentDto portfolio) {
    return db.getDbClient().portfolioDao().selectByUuid(db.getSession(), portfolio.uuid())
      .orElseThrow(() -> new IllegalStateException("Portfolio has invalid configuration"));
  }

  public PortfolioDto getPortfolioDto(PortfolioDto portfolio) {
    return db.getDbClient().portfolioDao().selectByUuid(db.getSession(), portfolio.getUuid())
      .orElseThrow(() -> new IllegalStateException("Portfolio has invalid configuration"));
  }

  public ComponentDto insertComponentAndPortfolio(ComponentDto componentDto, boolean isPrivate, Consumer<ComponentDto> componentPopulator,
    Consumer<PortfolioDto> portfolioPopulator) {
    insertComponentImpl(componentDto, isPrivate, componentPopulator);

    PortfolioDto portfolioDto = toPortfolioDto(componentDto, System2.INSTANCE.now());
    portfolioPopulator.accept(portfolioDto);
    dbClient.portfolioDao().insertWithAudit(db.getSession(), portfolioDto);
    db.commit();
    return componentDto;
  }

  public final ComponentDto insertPrivatePortfolio() {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, defaults(), defaults());
  }

  public final ComponentDto insertPrivatePortfolio(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio(uuid).setPrivate(true), true, dtoPopulator, defaults());
  }

  public final ComponentDto insertPrivatePortfolio(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, defaults());
  }

  public final ComponentDto insertPrivatePortfolio(Consumer<ComponentDto> dtoPopulator, Consumer<PortfolioDto> portfolioPopulator) {
    return insertComponentAndPortfolio(ComponentTesting.newPortfolio().setPrivate(true), true, dtoPopulator, portfolioPopulator);
  }

  public final ComponentDto insertSubportfolio(ComponentDto parentPortfolio) {
    return insertSubportfolio(parentPortfolio, defaults());
  }

  public final ComponentDto insertSubportfolio(ComponentDto parentPortfolio, Consumer<ComponentDto> consumer) {
    ComponentDto subPortfolioComponent = ComponentTesting.newSubPortfolio(parentPortfolio).setPrivate(true);
    return insertComponentAndPortfolio(subPortfolioComponent, true, consumer, sp -> sp.setParentUuid(sp.getRootUuid()));
  }

  public void addPortfolioReference(String portfolioUuid, String... referencerUuids) {
    for (String uuid : referencerUuids) {
      EntityDto entityDto = dbClient.entityDao().selectByUuid(db.getSession(), uuid)
        .orElseThrow();
      switch (entityDto.getQualifier()) {
        case APP -> {
          BranchDto appMainBranch = dbClient.branchDao().selectMainBranchByProjectUuid(db.getSession(), entityDto.getUuid())
            .orElseThrow();
          dbClient.portfolioDao().addReferenceBranch(db.getSession(), portfolioUuid, uuid, appMainBranch.getUuid());
        }
        case VIEW, SUBVIEW -> dbClient.portfolioDao().addReference(db.getSession(), portfolioUuid, uuid);
        default -> throw new IllegalStateException("Unexpected value: " + entityDto.getQualifier());
      }
    }
    db.commit();
  }

  public void addPortfolioReference(ComponentDto portfolio, String... referencerUuids) {
    addPortfolioReference(portfolio.uuid(), referencerUuids);
  }

  public void addPortfolioReference(PortfolioDto portfolio, String... referencerUuids) {
    addPortfolioReference(portfolio.getUuid(), referencerUuids);
  }

  public void addPortfolioReference(PortfolioDto portfolio, PortfolioDto reference) {
    addPortfolioReference(portfolio.getUuid(), reference.getUuid());
  }

  public void addPortfolioProject(String portfolioUuid, String... projectUuids) {
    for (String uuid : projectUuids) {
      dbClient.portfolioDao().addProject(db.getSession(), portfolioUuid, uuid);
    }
    db.commit();
  }

  public void addPortfolioProject(ComponentDto portfolio, String... projectUuids) {
    addPortfolioProject(portfolio.uuid(), projectUuids);
  }

  public void addPortfolioProject(ComponentDto portfolio, ProjectDto... projects) {
    for (ProjectDto project : projects) {
      dbClient.portfolioDao().addProject(db.getSession(), portfolio.uuid(), project.getUuid());
    }
    db.commit();
  }

  public void addPortfolioProject(ComponentDto portfolio, ComponentDto... mainBranches) {
    List<BranchDto> branchDtos = dbClient.branchDao().selectByUuids(db.getSession(), Arrays.stream(mainBranches).map(ComponentDto::uuid).toList());
    addPortfolioProject(portfolio, branchDtos.stream().map(BranchDto::getProjectUuid).toArray(String[]::new));
  }

  public void addPortfolioProject(PortfolioDto portfolioDto, ProjectDto... projects) {
    for (ProjectDto project : projects) {
      dbClient.portfolioDao().addProject(db.getSession(), portfolioDto.getUuid(), project.getUuid());
    }
    db.commit();
  }

  public void addPortfolioProjectBranch(PortfolioDto portfolio, ProjectDto project, String branchUuid) {
    addPortfolioProjectBranch(portfolio, project.getUuid(), branchUuid);
  }

  public void addPortfolioProjectBranch(PortfolioDto portfolio, String projectUuid, String branchUuid) {
    addPortfolioProjectBranch(portfolio.getUuid(), projectUuid, branchUuid);
  }

  public void addPortfolioProjectBranch(String portfolioUuid, String projectUuid, String branchUuid) {
    PortfolioProjectDto portfolioProject = dbClient.portfolioDao().selectPortfolioProjectOrFail(db.getSession(), portfolioUuid, projectUuid);
    dbClient.portfolioDao().addBranch(db.getSession(), portfolioProject.getUuid(), branchUuid);
    db.commit();
  }

  public final ProjectData insertPublicApplication() {
    return insertPublicApplication(defaults());
  }

  public final ProjectData insertPublicApplication(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication().setPrivate(false), false, defaults(), dtoPopulator);
  }

  public final ProjectData insertPrivateApplication(String uuid, Consumer<ComponentDto> dtoPopulator) {
    return insertPrivateApplication(dtoPopulator, p -> p.setUuid(uuid));
  }

  public final ProjectData insertPrivateApplication(String uuid) {
    return insertPrivateApplication(defaults(), p -> p.setUuid(uuid));
  }

  public final ProjectData insertPrivateApplication(Consumer<ComponentDto> dtoPopulator) {
    return insertPrivateApplication(dtoPopulator, defaults());
  }

  public final ProjectData insertPrivateApplication() {
    return insertPrivateApplication(defaults(), defaults());
  }

  public final ProjectData insertPrivateApplication(Consumer<ComponentDto> dtoPopulator, Consumer<ProjectDto> projectPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication().setPrivate(true), true, defaults(), dtoPopulator, projectPopulator);
  }

  public final ComponentDto insertSubView(ComponentDto view) {
    return insertSubView(view, defaults());
  }

  public final ComponentDto insertSubView(ComponentDto view, Consumer<ComponentDto> dtoPopulator) {
    ComponentDto subViewComponent = ComponentTesting.newSubPortfolio(view);
    return insertComponentAndPortfolio(subViewComponent, view.isPrivate(), dtoPopulator, p -> p.setParentUuid(view.uuid()));
  }

  public void addPortfolioApplicationBranch(String portfolioUuid, String applicationUuid, String branchUuid) {
    dbClient.portfolioDao().addReferenceBranch(db.getSession(), portfolioUuid, applicationUuid, branchUuid);
    db.commit();
  }

  private ProjectData insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto> componentDtoPopulator, Consumer<ProjectDto> projectDtoPopulator) {
    insertComponentImpl(component, isPrivate, componentDtoPopulator);

    ProjectDto projectDto = toProjectDto(component, System2.INSTANCE.now());
    projectDtoPopulator.accept(projectDto);
    dbClient.projectDao().insert(db.getSession(), projectDto);

    BranchDto branchDto = ComponentTesting.newMainBranchDto(component, projectDto.getUuid());
    branchDto.setExcludeFromPurge(true);
    branchPopulator.accept(branchDto);
    branchDto.setIsMain(true);
    dbClient.branchDao().insert(db.getSession(), branchDto);

    db.commit();
    return new ProjectData(getProjectDtoByMainBranch(component), branchDto, component);
  }

  public void addApplicationProject(ProjectDto application, ProjectDto... projects) {
    for (ProjectDto project : projects) {
      dbClient.applicationProjectsDao().addProject(db.getSession(), application.getUuid(), project.getUuid());
    }
    db.commit();
  }

  public void addApplicationProject(ProjectData application, ProjectData... projects) {
    for (ProjectData project : projects) {
      dbClient.applicationProjectsDao().addProject(db.getSession(), application.getProjectDto().getUuid(), project.getProjectDto().getUuid());
    }
    db.commit();
  }

  public void addProjectBranchToApplicationBranch(ComponentDto applicationBranchComponent, ComponentDto... projectBranchesComponent) {
    BranchDto applicationBranch = getBranchDto(applicationBranchComponent);
    BranchDto[] componentDtos = Arrays.stream(projectBranchesComponent).map(this::getBranchDto).toArray(BranchDto[]::new);

    addProjectBranchToApplicationBranch(applicationBranch, componentDtos);
  }

  public void addProjectBranchToApplicationBranch(BranchDto applicationBranch, BranchDto... projectBranches) {
    for (BranchDto projectBranch : projectBranches) {
      dbClient.applicationProjectsDao().addProjectBranchToAppBranch(db.getSession(), applicationBranch, projectBranch);
    }
    db.commit();
  }

  private ProjectData insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto> componentDtoPopulator) {
    return insertComponentAndBranchAndProject(component, isPrivate, branchPopulator, componentDtoPopulator, defaults());
  }

  private ProjectData insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, Consumer<BranchDto> branchPopulator) {
    return insertComponentAndBranchAndProject(component, isPrivate, branchPopulator, defaults());
  }

  private ProjectData insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate) {
    return insertComponentAndBranchAndProject(component, isPrivate, defaults());
  }

  private ComponentDto insertComponentImpl(ComponentDto component, @Nullable Boolean isPrivate, Consumer<ComponentDto> dtoPopulator) {
    dtoPopulator.accept(component);
    checkState(isPrivate == null || component.isPrivate() == isPrivate, "Illegal modification of private flag");
    dbClient.componentDao().insert(db.getSession(), component, true);
    db.commit();

    return component;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(db.getSession(), asList(components), true);
    db.commit();
  }

  public SnapshotDto insertSnapshot(SnapshotDto snapshotDto) {
    SnapshotDto snapshot = dbClient.snapshotDao().insert(db.getSession(), snapshotDto);
    db.commit();
    return snapshot;
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto) {
    return insertSnapshot(componentDto, defaults());
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(componentDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto) {
    return insertSnapshot(branchDto, defaults());
  }

  public SnapshotDto insertSnapshot(ProjectDto project) {
    return insertSnapshot(project, defaults());
  }

  public SnapshotDto insertSnapshot(ProjectData project, Consumer<SnapshotDto> consumer) {
    return insertSnapshot(project.getMainBranchDto(), consumer);
  }

  /**
   * Add a snapshot to the main branch of a project
   * Should use insertSnapshot(org.sonar.db.component.BranchDto, java.util.function.Consumer<org.sonar.db.component.SnapshotDto>) instead
   */
  @Deprecated
  public SnapshotDto insertSnapshot(ProjectDto project, Consumer<SnapshotDto> consumer) {
    BranchDto mainBranchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).orElseThrow();
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(mainBranchDto.getUuid());
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(PortfolioDto portfolio) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(portfolio.getUuid());
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(branchDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public void insertSnapshots(SnapshotDto... snapshotDtos) {
    dbClient.snapshotDao().insert(db.getSession(), asList(snapshotDtos));
    db.commit();
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(ComponentDto mainBranchComponent, Consumer<BranchDto>... dtoPopulators) {
    BranchDto mainBranch = dbClient.branchDao().selectByUuid(db.getSession(), mainBranchComponent.branchUuid()).orElseThrow(IllegalArgumentException::new);
    BranchDto branchDto = ComponentTesting.newBranchDto(mainBranch.getProjectUuid(), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    return insertProjectBranch(mainBranchComponent, branchDto);
  }

  @SafeVarargs
  public final BranchDto insertProjectBranch(ProjectDto project, Consumer<BranchDto>... dtoPopulators) {
    BranchDto branchDto = ComponentTesting.newBranchDto(project.getUuid(), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    insertProjectBranch(project, branchDto);
    return branchDto;
  }

  public final ComponentDto insertProjectBranch(ProjectDto project, BranchDto branchDto) {
    checkArgument(branchDto.getProjectUuid().equals(project.getUuid()));
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(db.getSession(), branchDto);
    db.commit();
    return branch;
  }

  public final ComponentDto insertProjectBranch(ComponentDto project, BranchDto branchDto) {
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(db.getSession(), branchDto);
    db.commit();
    return branch;
  }

  public static ProjectDto toProjectDto(ComponentDto componentDto, long createTime) {
    return new ProjectDto()
      .setUuid(Uuids.createFast())
      .setKey(componentDto.getKey())
      .setQualifier(componentDto.qualifier() != null ? componentDto.qualifier() : ComponentQualifiers.PROJECT)
      .setCreatedAt(createTime)
      .setUpdatedAt(createTime)
      .setPrivate(componentDto.isPrivate())
      .setDescription(componentDto.description())
      .setName(componentDto.name())
      .setCreationMethod(CreationMethod.LOCAL_API);
  }

  public static PortfolioDto toPortfolioDto(ComponentDto componentDto, long createTime) {
    return new PortfolioDto()
      .setUuid(componentDto.uuid())
      .setKey(componentDto.getKey())
      .setRootUuid(componentDto.branchUuid())
      .setSelectionMode(NONE.name())
      .setCreatedAt(createTime)
      .setUpdatedAt(createTime)
      .setPrivate(componentDto.isPrivate())
      .setDescription(componentDto.description())
      .setName(componentDto.name());
  }

  public static <T> Consumer<T> defaults() {
    return t -> {
    };
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.db.component.BranchType.BRANCH;

public class ComponentDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public ComponentDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public SnapshotDto insertProjectAndSnapshot(ComponentDto component) {
    insertComponentAndBranchAndProject(component, null, noExtraConfiguration());
    return insertSnapshot(component);
  }

  public SnapshotDto insertViewAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    return insertSnapshot(component);
  }

  public ComponentDto insertComponent(ComponentDto component) {
    return insertComponentImpl(component, null, noExtraConfiguration());
  }

  public ComponentDto insertPrivateProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()), true, noExtraConfiguration());
  }

  public ProjectDto getProjectDto(ComponentDto project) {
    return db.getDbClient().projectDao().selectByUuid(dbSession, project.uuid()).get();
  }

  public ComponentDto insertPrivateProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject() {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()), false, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject(ComponentDto componentDto) {
    return insertComponentAndBranchAndProject(componentDto, false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()), true, null, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicProject(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()), false, null, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(organizationDto), true, null, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicProject(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(organizationDto), false, null, dtoPopulators);
  }

  public ComponentDto insertPrivateProject(OrganizationDto organizationDto) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(organizationDto), true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject(OrganizationDto organizationDto) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(organizationDto), false, noExtraConfiguration());
  }

  public ProjectDto insertPublicProjectDto() {
    ComponentDto componentDto = insertPublicProject();
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPrivateProjectDto() {
    ComponentDto componentDto = insertPrivateProject();
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPublicProjectDto(OrganizationDto organization) {
    ComponentDto componentDto = insertPublicProject(organization);
    return getProjectDto(componentDto);
  }

  @SafeVarargs
  public final ProjectDto insertPublicProjectDto(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    ComponentDto componentDto = insertPublicProject(organization, dtoPopulators);
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPrivateProjectDto(OrganizationDto organization) {
    ComponentDto componentDto = insertPrivateProject(organization);
    return getProjectDto(componentDto);
  }

  @SafeVarargs
  public final ProjectDto insertPrivateProjectDto(Consumer<ComponentDto>... dtoPopulators) {
    ComponentDto componentDto = insertPrivateProject(dtoPopulators);
    return getProjectDto(componentDto);
  }

  public ProjectDto insertPrivateProjectDto(OrganizationDto organization, Consumer<BranchDto> branchConsumer) {
    ComponentDto componentDto = insertPrivateProjectWithCustomBranch(organization, branchConsumer);
    return getProjectDto(componentDto);
  }

  public ComponentDto insertPrivateProject(OrganizationDto organizationDto, String uuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(organizationDto, uuid), true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject(OrganizationDto organizationDto, String uuid) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPublicProjectDto(organizationDto, uuid), false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(OrganizationDto organizationDto, String uuid, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(organizationDto, uuid), true, null, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProjectWithCustomBranch(OrganizationDto organizationDto, Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto>... componentPopulator) {
    return insertComponentAndBranchAndProject(ComponentTesting.newPrivateProjectDto(organizationDto), true, branchPopulator, componentPopulator);
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  @Deprecated
  public ComponentDto insertView() {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization()), false, noExtraConfiguration());
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  public ComponentDto insertView(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization()), false, dtoPopulator);
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  public ComponentDto insertView(OrganizationDto organizationDto) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto), false, noExtraConfiguration());
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  public ComponentDto insertView(OrganizationDto organizationDto, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto), false, dtoPopulator);
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  public ComponentDto insertView(String uuid) {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization(), uuid), false, noExtraConfiguration());
  }

  /**
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  public ComponentDto insertView(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto, uuid), false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPublicPortfolio(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newView(organization).setPrivate(false), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivatePortfolio(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization()).setPrivate(true), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivatePortfolio(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newView(organization).setPrivate(true), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicApplication(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newApplication(db.getDefaultOrganization()).setPrivate(false), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicApplication(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication(organization).setPrivate(false), false, b -> {
    }, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateApplication(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newApplication(db.getDefaultOrganization()).setPrivate(true), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateApplication(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication(organization).setPrivate(true), true, b -> {
    }, dtoPopulators);
  }

  /**
   * @see #insertPublicApplication(OrganizationDto, Consumer[])
   * @deprecated since 6.6
   */
  @SafeVarargs
  public final ComponentDto insertApplication(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newApplication(organizationDto), false, b -> {
    }, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertSubView(ComponentDto view, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentAndBranchAndProject(ComponentTesting.newSubView(view), view.isPrivate(), null, dtoPopulators);
  }

  private static <T> Consumer<T> noExtraConfiguration() {
    return (t) -> {
    };
  }

  @SafeVarargs
  private final ComponentDto insertComponentAndBranchAndProject(ComponentDto component, @Nullable Boolean isPrivate, @Nullable Consumer<BranchDto> branchPopulator,
    Consumer<ComponentDto>... dtoPopulators) {
    insertComponentImpl(component, isPrivate, dtoPopulators);

    ProjectDto projectDto = toProjectDto(component, System2.INSTANCE.now());
    dbClient.projectDao().insert(dbSession, projectDto);

    BranchDto branchDto = ComponentTesting.newBranchDto(component, BRANCH);
    branchDto.setExcludeFromPurge(true);

    if (branchPopulator != null) {
      branchPopulator.accept(branchDto);
    }
    dbClient.branchDao().insert(dbSession, branchDto);

    db.commit();
    return component;
  }

  @SafeVarargs
  private final ComponentDto insertComponentImpl(ComponentDto component, @Nullable Boolean isPrivate, Consumer<ComponentDto>... dtoPopulators) {
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(component));
    checkState(isPrivate == null || component.isPrivate() == isPrivate, "Illegal modification of private flag");
    dbClient.componentDao().insert(dbSession, component);
    db.commit();

    return component;
  }

  public void insertComponents(ComponentDto... components) {
    dbClient.componentDao().insert(dbSession, asList(components));
    db.commit();
  }

  public SnapshotDto insertSnapshot(SnapshotDto snapshotDto) {
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, snapshotDto);
    db.commit();
    return snapshot;
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto) {
    return insertSnapshot(componentDto, noExtraConfiguration());
  }

  public SnapshotDto insertSnapshot(ComponentDto componentDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(componentDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto) {
    return insertSnapshot(branchDto, noExtraConfiguration());
  }

  public SnapshotDto insertSnapshot(BranchDto branchDto, Consumer<SnapshotDto> consumer) {
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(branchDto);
    consumer.accept(snapshotDto);
    return insertSnapshot(snapshotDto);
  }

  public void insertSnapshots(SnapshotDto... snapshotDtos) {
    dbClient.snapshotDao().insert(dbSession, asList(snapshotDtos));
    db.commit();
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(ComponentDto project, Consumer<BranchDto>... dtoPopulators) {
    // MainBranchProjectUuid will be null if it's a main branch
    BranchDto branchDto = ComponentTesting.newBranchDto(firstNonNull(project.getMainBranchProjectUuid(), project.projectUuid()), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    return insertProjectBranch(project, branchDto);
  }

  @SafeVarargs
  public final BranchDto insertProjectBranch(ProjectDto project, Consumer<BranchDto>... dtoPopulators) {
    BranchDto branchDto = ComponentTesting.newBranchDto(project.getUuid(), BRANCH);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    insertProjectBranch(project, branchDto);
    return branchDto;
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(OrganizationDto organization, Consumer<BranchDto>... dtoPopulators) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization);
    return insertProjectBranch(project, dtoPopulators);
  }

  public final ComponentDto insertProjectBranch(ProjectDto project, BranchDto branchDto) {
    checkArgument(branchDto.getProjectUuid().equals(project.getUuid()));
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  public final ComponentDto insertProjectBranch(ComponentDto project, BranchDto branchDto) {
    // MainBranchProjectUuid will be null if it's a main branch
    checkArgument(branchDto.getProjectUuid().equals(firstNonNull(project.getMainBranchProjectUuid(), project.projectUuid())));
    ComponentDto branch = ComponentTesting.newBranchComponent(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  private static <T> T firstNonNull(@Nullable T first, T second) {
    return (first != null) ? first : second;
  }

  // TODO temporary constructor to quickly create project from previous project component.
  private ProjectDto toProjectDto(ComponentDto componentDto, long createTime) {
    return new ProjectDto()
      .setUuid(componentDto.uuid())
      .setKey(componentDto.getDbKey())
      .setQualifier(componentDto.qualifier() != null ? componentDto.qualifier() : Qualifiers.PROJECT)
      .setCreatedAt(createTime)
      .setUpdatedAt(createTime)
      .setPrivate(componentDto.isPrivate())
      .setDescription(componentDto.description())
      .setName(componentDto.name())
      .setOrganizationUuid(componentDto.getOrganizationUuid())
      .setTags(componentDto.getTags())
      .setTagsString(componentDto.getTagsString());
  }
}

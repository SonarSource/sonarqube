/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonar.db.component.BranchType.LONG;

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
    return insertComponentAndSnapshot(component);
  }

  public SnapshotDto insertViewAndSnapshot(ComponentDto component) {
    return insertComponentAndSnapshot(component);
  }

  private SnapshotDto insertComponentAndSnapshot(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, SnapshotTesting.newAnalysis(component));
    db.commit();

    return snapshot;
  }

  public ComponentDto insertComponent(ComponentDto component) {
    return insertComponentImpl(component, null, noExtraConfiguration());
  }

  public ComponentDto insertPrivateProject() {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()), true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject() {
    return insertComponentImpl(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()), false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicProject(Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newPublicProjectDto(db.getDefaultOrganization()), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(organizationDto), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicProject(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newPublicProjectDto(organizationDto), false, dtoPopulators);
  }

  public ComponentDto insertPrivateProject(OrganizationDto organizationDto) {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(organizationDto), true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject(OrganizationDto organizationDto) {
    return insertComponentImpl(ComponentTesting.newPublicProjectDto(organizationDto), false, noExtraConfiguration());
  }

  public ComponentDto insertPrivateProject(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(organizationDto, uuid), true, noExtraConfiguration());
  }

  public ComponentDto insertPublicProject(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(ComponentTesting.newPublicProjectDto(organizationDto, uuid), false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPrivateProject(OrganizationDto organizationDto, String uuid, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newPrivateProjectDto(organizationDto, uuid), true, dtoPopulators);
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  @Deprecated
  public ComponentDto insertView() {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization()), false, noExtraConfiguration());
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  public ComponentDto insertView(Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization()), false, dtoPopulator);
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  public ComponentDto insertView(OrganizationDto organizationDto) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto), false, noExtraConfiguration());
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  public ComponentDto insertView(OrganizationDto organizationDto, Consumer<ComponentDto> dtoPopulator) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto), false, dtoPopulator);
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  public ComponentDto insertView(String uuid) {
    return insertComponentImpl(ComponentTesting.newView(db.getDefaultOrganization(), uuid), false, noExtraConfiguration());
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicPortfolio(OrganizationDto, Consumer[])
   */
  public ComponentDto insertView(OrganizationDto organizationDto, String uuid) {
    return insertComponentImpl(ComponentTesting.newView(organizationDto, uuid), false, noExtraConfiguration());
  }

  @SafeVarargs
  public final ComponentDto insertPublicPortfolio(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newView(organization).setPrivate(false), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivatePortfolio(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newView(organization).setPrivate(true), true, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPublicApplication(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newApplication(organization).setPrivate(false), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertPrivateApplication(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newApplication(organization).setPrivate(true), true, dtoPopulators);
  }

  /**
   * @deprecated since 6.6
   * @see #insertPublicApplication(OrganizationDto, Consumer[])
   */
  @SafeVarargs
  public final ComponentDto insertApplication(OrganizationDto organizationDto, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newApplication(organizationDto), false, dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertSubView(ComponentDto view, Consumer<ComponentDto>... dtoPopulators) {
    return insertComponentImpl(ComponentTesting.newSubView(view), view.isPrivate(), dtoPopulators);
  }

  private static <T> Consumer<T> noExtraConfiguration() {
    return (t) -> {
    };
  }

  @SafeVarargs
  private final ComponentDto insertComponentImpl(ComponentDto component, @Nullable Boolean isPrivate, Consumer<ComponentDto>... dtoPopulators) {
    Arrays.stream(dtoPopulators)
      .forEach(dtoPopulator -> dtoPopulator.accept(component));
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
    SnapshotDto snapshot = dbClient.snapshotDao().insert(dbSession, snapshotDto);
    db.commit();
    return snapshot;
  }

  public void insertSnapshots(SnapshotDto... snapshotDtos) {
    dbClient.snapshotDao().insert(dbSession, asList(snapshotDtos));
    db.commit();
  }

  @SafeVarargs
  public final ComponentDto insertMainBranch(Consumer<ComponentDto>... dtoPopulators) {
    return insertMainBranch(db.getDefaultOrganization(), dtoPopulators);
  }

  @SafeVarargs
  public final ComponentDto insertMainBranch(OrganizationDto organization, Consumer<ComponentDto>... dtoPopulators) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(project));
    return insertMainBranch(project);
  }

  public final ComponentDto insertMainBranch(ComponentDto project) {
    BranchDto branchDto = ComponentTesting.newBranchDto(project, LONG);
    insertComponent(project);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return project;
  }

  @SafeVarargs
  public final ComponentDto insertMainBranch(OrganizationDto organization, String mainBranchName, Consumer<ComponentDto>... dtoPopulators) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization);
    BranchDto branchDto = ComponentTesting.newBranchDto(project, LONG).setKey(mainBranchName);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(project));
    insertComponent(project);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return project;
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(ComponentDto project, Consumer<BranchDto>... dtoPopulators) {
    // MainBranchProjectUuid will be null if it's a main branch
    BranchDto branchDto = ComponentTesting.newBranchDto(firstNonNull(project.getMainBranchProjectUuid(), project.projectUuid()), LONG);
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(branchDto));
    ComponentDto branch = ComponentTesting.newProjectBranch(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  @SafeVarargs
  public final ComponentDto insertProjectBranch(OrganizationDto organization, Consumer<BranchDto>... dtoPopulators) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organization);
    return insertProjectBranch(project, dtoPopulators);
  }

  public final ComponentDto insertProjectBranch(ComponentDto project, BranchDto branchDto) {
    // MainBranchProjectUuid will be null if it's a main branch
    checkArgument(branchDto.getProjectUuid().equals(firstNonNull(project.getMainBranchProjectUuid(), project.projectUuid())));
    ComponentDto branch = ComponentTesting.newProjectBranch(project, branchDto);
    insertComponent(branch);
    dbClient.branchDao().insert(dbSession, branchDto);
    db.commit();
    return branch;
  }

  public final void setManualBaseline(ComponentDto longOrMainBranchOfProject, SnapshotDto analysis) {
    checkArgument(longOrMainBranchOfProject.isRoot());

    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(dbSession, longOrMainBranchOfProject.uuid())
      .orElseThrow(() -> new IllegalArgumentException("BranchDto not found for component " + longOrMainBranchOfProject));
    checkArgument(branchDto.getBranchType() == LONG, "must be a main or a Long Living branch");
    db.getDbClient().branchDao().updateManualBaseline(dbSession, longOrMainBranchOfProject.uuid(), analysis.getUuid());
    db.commit();
  }

  public final void unsetManualBaseline(ComponentDto longOrMainBranchOfProject) {
    checkArgument(longOrMainBranchOfProject.isRoot());

    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(dbSession, longOrMainBranchOfProject.uuid())
      .orElseThrow(() -> new IllegalArgumentException("BranchDto not found for component " + longOrMainBranchOfProject));
    checkArgument(branchDto.getBranchType() == LONG, "must be a main or a Long Living branch");
    db.getDbClient().branchDao().updateManualBaseline(dbSession, longOrMainBranchOfProject.uuid(), null);
    db.commit();
  }

  private static <T> T firstNonNull(@Nullable T first, T second) {
    return (first != null) ? first : second;
  }

}

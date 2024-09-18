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
package org.sonar.db.project;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualityprofile.QProfileDto;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.Metric.ValueType.STRING;

class ProjectDaoIT {

  private final Random random = new SecureRandom();

  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final AuditPersister auditPersister = mock(AuditPersister.class);

  private final ProjectDao projectDao = new ProjectDao(system2, new NoOpAuditPersister());
  private final ProjectDao projectDaoWithAuditPersister = new ProjectDao(system2, auditPersister);

  @Test
  void should_insert_and_select_by_uuid() {
    ProjectDto dto = createProject("o1", "p1");

    projectDao.insert(db.getSession(), dto);

    Optional<ProjectDto> projectByUuid = projectDao.selectByUuid(db.getSession(), "uuid_o1_p1");
    assertThat(projectByUuid).isPresent();
    assertProject(projectByUuid.get(), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertThat(projectByUuid.get().isPrivate()).isFalse();
  }

  @Test
  void selectProjectByKey_shouldReturnProject() {
    ProjectDto dto = createProject("o1", "p1");

    projectDao.insert(db.getSession(), dto);

    Optional<ProjectDto> projectByKee = projectDao.selectProjectByKey(db.getSession(), "projectKee_o1_p1");
    assertThat(projectByKee).isPresent();
    assertProject(projectByKee.get(), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
  }

  @Test
  void selectProjectsByKeys_whenEmptyInput_returnEmptyList() {
    assertThat(projectDao.selectProjectsByKeys(db.getSession(), emptySet())).isEmpty();
  }

  @Test
  void select_by_branch_uuid() {
    ProjectDto dto = createProject("o1", "p1");
    projectDao.insert(db.getSession(), dto);
    BranchDto branchDto = db.components().insertProjectBranch(dto);
    assertThat(projectDao.selectByBranchUuid(db.getSession(), branchDto.getUuid()).get().getUuid()).isEqualTo(dto.getUuid());
  }

  @Test
  void select_projects() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projects = projectDao.selectProjects(db.getSession());
    assertThat(projects).extracting(ProjectDto::getKey).containsExactlyInAnyOrder("projectKee_o1_p1", "projectKee_o1_p2");
  }

  @Test
  void selectProjects_returnsCreationMethod() {
    ProjectDto dto1 = createProject("o1", "p1").setCreationMethod(CreationMethod.SCANNER_API);
    ProjectDto dto2 = createProject("o1", "p2").setCreationMethod(CreationMethod.UNKNOWN);

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projects = projectDao.selectProjects(db.getSession());
    Map<String, CreationMethod> projectToCreationMethod = projects.stream().collect(Collectors.toMap(EntityDto::getName,
      ProjectDto::getCreationMethod));
    assertThat(projectToCreationMethod)
      .hasSize(2)
      .containsEntry("projectName_p1", CreationMethod.SCANNER_API)
      .containsEntry("projectName_p2", CreationMethod.UNKNOWN);
  }

  @Test
  void selectProjects_returnsAiCodeAssurance() {
    ProjectDto dto1 = createProject("o1", "p1").setAiCodeAssurance(true);
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projects = projectDao.selectProjects(db.getSession());
    Map<String, Boolean> projectToAiCodeAssurance = projects.stream().collect(Collectors.toMap(EntityDto::getName,
      ProjectDto::getAiCodeAssurance));
    assertThat(projectToAiCodeAssurance)
      .hasSize(2)
      .containsEntry("projectName_p1", true)
      .containsEntry("projectName_p2", false);
  }

  @Test
  void select_all() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o2", "p1");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByOrg = projectDao.selectAll(db.getSession());
    assertThat(projectsByOrg)
      .extracting(ProjectDto::getName, ProjectDto::getKey, ProjectDto::getUuid, ProjectDto::getDescription,
        ProjectDto::getTagsString, ProjectDto::isPrivate)
      .containsExactlyInAnyOrder(
        tuple("projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false),
        tuple("projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false),
        tuple("projectName_p1", "projectKee_o2_p1", "uuid_o2_p1", "desc_p1", "tag1,tag2", false));
  }

  @Test
  void selectApplicationsByKeys_shouldReturnAllApplications() {
    var applications = new ArrayList<ProjectDto>();

    for (int i = 0; i < 1500; i++) {
      ProjectDto project = db.components().insertPrivateProject().getProjectDto();
      ProjectDto app = db.components().insertPrivateApplication().getProjectDto();
      db.components().addApplicationProject(app, project);
      applications.add(i, app);
    }

    Set<String> applicationsId = new HashSet<>();

    List<Tuple> applicationsData = applications
      .stream()
      .map(x -> {
        applicationsId.add(x.getKey());
        return tuple(x.getKey(), x.getName(), x.getUuid(), x.getDescription(), x.isPrivate());
      })
      .toList();

    List<ProjectDto> selectedApplications = projectDao.selectApplicationsByKeys(db.getSession(), applicationsId);

    assertThat(selectedApplications)
      .extracting(ProjectDto::getKey, ProjectDto::getName, ProjectDto::getUuid, ProjectDto::getDescription, ProjectDto::isPrivate)
      .containsExactlyInAnyOrderElementsOf(applicationsData);
  }

  @Test
  void selectApplicationsByKeys_whenEmptyInput_shouldReturnEmptyList() {
    assertThat(projectDao.selectApplicationsByKeys(db.getSession(), emptySet())).isEmpty();
  }

  @Test
  void update_tags() {
    ProjectDto dto1 = createProject("o1", "p1").setTagsString("");
    ProjectDto dto2 = createProject("o1", "p2").setTagsString("tag1,tag2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", null, false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);

    dto1.setTags(Collections.singletonList("tag3"));
    dto2.setTagsString("");
    projectDao.updateTags(db.getSession(), dto1);
    projectDao.updateTags(db.getSession(), dto2);

    projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag3", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", null, false);

    assertThat(projectsByUuids.get(0).getTags()).containsOnly("tag3");
  }

  @Test
  void update_visibility() {
    ProjectDto dto1 = createProject("o1", "p1").setPrivate(true);
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", true);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);

    projectDao.updateVisibility(db.getSession(), dto1.getUuid(), false);
    projectDao.updateVisibility(db.getSession(), dto2.getUuid(), true);

    projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", true);
  }

  @Test
  void update_aiCodeAssurance() {
    ProjectDto dto1 = createProject("o1", "p1").setAiCodeAssurance(true);
    ProjectDto dto2 = createProject("o1", "p2");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false, true);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false, false);

    projectDao.updateAiCodeAssurance(db.getSession(), dto1.getUuid(), false);
    projectDao.updateAiCodeAssurance(db.getSession(), dto2.getUuid(), true);

    projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false, false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false, true);
  }

  @Test
  void select_by_uuids() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o1", "p3");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), new HashSet<>(Arrays.asList("uuid_o1_p1", "uuid_o1_p2")));
    assertThat(projectsByUuids).hasSize(2);
    assertProject(projectsByUuids.get(0), "projectName_p1", "projectKee_o1_p1", "uuid_o1_p1", "desc_p1", "tag1,tag2", false);
    assertProject(projectsByUuids.get(1), "projectName_p2", "projectKee_o1_p2", "uuid_o1_p2", "desc_p2", "tag1,tag2", false);
  }

  @Test
  void select_by_uuids_over_1000() {
    IntStream.range(0, 1005).mapToObj(value -> createProject("o1", "p" + value))
      .forEach(projectDto -> projectDao.insert(db.getSession(), projectDto));

    var projectUuids = projectDao.selectAll(db.getSession()).stream().map(ProjectDto::getUuid).collect(Collectors.toSet());
    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), projectUuids);
    assertThat(projectsByUuids).hasSize(1005);
  }

  @Test
  void select_empty_by_uuids() {
    ProjectDto dto1 = createProject("o1", "p1");
    ProjectDto dto2 = createProject("o1", "p2");
    ProjectDto dto3 = createProject("o1", "p3");

    projectDao.insert(db.getSession(), dto1);
    projectDao.insert(db.getSession(), dto2);
    projectDao.insert(db.getSession(), dto3);

    List<ProjectDto> projectsByUuids = projectDao.selectByUuids(db.getSession(), Collections.emptySet());
    assertThat(projectsByUuids).isEmpty();
  }

  @Test
  void insert_withoutTrack_shouldNotCallAuditPersister() {
    ProjectDto dto1 = createProject("o1", "p1");

    projectDaoWithAuditPersister.insert(db.getSession(), dto1, false);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void insert_withTrack_shouldCallAuditPersister() {
    ProjectDto dto1 = createProject("o1", "p1");

    projectDaoWithAuditPersister.insert(db.getSession(), dto1, true);

    verify(auditPersister, times(1)).addComponent(any(), any());
  }

  @Test
  void update_shouldCallAuditPersister() {
    ProjectDto dto1 = createProject("o1", "p1");

    projectDaoWithAuditPersister.update(db.getSession(), dto1);

    verify(auditPersister, times(1)).updateComponent(any(), any());
  }

  @Test
  void select_project_uuids_associated_to_default_quality_profile_for_specific_language() {
    String language = "xoo";
    Set<ProjectData> projects = insertProjects(random.nextInt(10));
    insertDefaultQualityProfile(language);
    insertProjectsLiveMeasures(language, projects);

    Set<String> projectUuids = projectDao.selectProjectUuidsAssociatedToDefaultQualityProfileByLanguage(db.getSession(), language);

    assertThat(projectUuids).containsExactlyInAnyOrderElementsOf(extractComponentUuids(projects));
  }

  @Test
  void update_ncloc_should_update_project() {
    String projectUuid = db.components().insertPublicProject().projectUuid();

    projectDao.updateNcloc(db.getSession(), projectUuid, 10L);

    Assertions.assertThat(projectDao.getNclocSum(db.getSession())).isEqualTo(10L);
  }

  @Test
  void getNcloc_sum_compute_correctly_sum_of_projects() {
    projectDao.updateNcloc(db.getSession(), db.components().insertPublicProject().projectUuid(), 1L);
    projectDao.updateNcloc(db.getSession(), db.components().insertPublicProject().projectUuid(), 20L);
    projectDao.updateNcloc(db.getSession(), db.components().insertPublicProject().projectUuid(), 100L);

    long nclocSum = projectDao.getNclocSum(db.getSession());

    Assertions.assertThat(nclocSum).isEqualTo(121L);
  }

  @Test
  void getNcloc_sum_compute_correctly_sum_of_projects_while_excluding_project() {
    projectDao.updateNcloc(db.getSession(), db.components().insertPublicProject().projectUuid(), 1L);
    projectDao.updateNcloc(db.getSession(), db.components().insertPublicProject().projectUuid(), 20L);
    ProjectDto project3 = db.components().insertPublicProject().getProjectDto();
    projectDao.updateNcloc(db.getSession(), project3.getUuid(), 100L);

    long nclocSum = projectDao.getNclocSum(db.getSession(), project3.getUuid());

    Assertions.assertThat(nclocSum).isEqualTo(21L);
  }

  @Test
  void selectByUuids_whenUuidsAreEmptyWithPagination_shouldReturnEmptyList() {
    db.components().insertPublicProject();

    List<ProjectDto> projectDtos = projectDao.selectByUuids(db.getSession(), emptySet(), Pagination.forPage(1).andSize(1));

    assertThat(projectDtos).isEmpty();
  }

  @Test
  void selectByUuids_whenPagination_shouldReturnSubSetOfPagination() {
    Set<String> projectUuids = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      final String name = "Project_" + i;
      String projectUuid = db.components().insertPublicProject(c -> c.setName(name)).projectUuid();
      projectUuids.add(projectUuid);
    }

    List<ProjectDto> projectDtos = projectDao.selectByUuids(db.getSession(), projectUuids, Pagination.forPage(2).andSize(2));

    assertThat(projectDtos).extracting(ProjectDto::getName).containsOnly("Project_2", "Project_3");
  }

  @Test
  void countIndexedProjects() {
    assertThat(projectDao.countIndexedProjects(db.getSession())).isZero();

    // master branch with flag set to false
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    // branches & PRs
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project2, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));
    db.components().insertProjectBranch(project2, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));

    assertThat(projectDao.countIndexedProjects(db.getSession())).isEqualTo(1);
  }

  @Test
  void countProjects() {
    assertThat(projectDao.countProjects(db.getSession())).isZero();

    IntStream.range(0, 10).forEach(x -> db.components().insertPrivateProject());

    assertThat(projectDao.countProjects(db.getSession())).isEqualTo(10);
  }

  @Test
  void selectProjectsByLanguage_whenTwoLanguagesArePassed_selectProjectsWithTheseLanguages() {
    Consumer<MetricDto> configureMetric = metric -> metric
      .setValueType(STRING.name())
      .setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    MetricDto metric = db.measures().insertMetric(configureMetric);

    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ProjectData project4 = db.components().insertPrivateProject();

    insertLiveMeasure("c", metric).accept(project1);
    insertLiveMeasure("cpp", metric).accept(project2);
    insertLiveMeasure("java", metric).accept(project3);
    insertLiveMeasure("cobol", metric).accept(project4);

    List<ProjectDto> projectDtos = projectDao.selectProjectsByLanguage(db.getSession(), Set.of("cpp", "c"));

    assertThat(projectDtos).extracting(ProjectDto::getUuid)
      .containsExactlyInAnyOrder(project1.getProjectDto().getUuid(), project2.getProjectDto().getUuid());
  }

  private void insertDefaultQualityProfile(String language) {
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true).setLanguage(language));
    db.qualityProfiles().setAsDefault(profile);
  }

  private static Set<String> extractComponentUuids(Collection<ProjectData> components) {
    return components
      .stream()
      .map(ProjectData::projectUuid)
      .collect(Collectors.toSet());
  }

  private Set<ProjectData> insertProjects(int number) {
    return IntStream
      .rangeClosed(0, number)
      .mapToObj(x -> db.components().insertPrivateProject())
      .collect(Collectors.toSet());
  }

  private Consumer<LiveMeasureDto> configureLiveMeasure(String language, MetricDto metric, ProjectDto project, ComponentDto componentDto) {
    return liveMeasure -> liveMeasure
      .setMetricUuid(metric.getUuid())
      .setComponentUuid(componentDto.uuid())
      .setProjectUuid(project.getUuid())
      .setData(language + "=" + random.nextInt(10));
  }

  private Consumer<ProjectData> insertLiveMeasure(String language, MetricDto metric) {
    return (projectData) -> db.measures().insertLiveMeasure(projectData.getMainBranchComponent(), metric,
      configureLiveMeasure(language, metric, projectData.getProjectDto(), projectData.getMainBranchComponent()));
  }

  private void insertProjectsLiveMeasures(String language, Set<ProjectData> projects) {
    Consumer<MetricDto> configureMetric = metric -> metric
      .setValueType(STRING.name())
      .setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY);

    MetricDto metric = db.measures().insertMetric(configureMetric);

    projects.forEach(insertLiveMeasure(language, metric));
  }

  private void assertProject(ProjectDto dto, String name, String kee, String uuid, String desc, @Nullable String tags, boolean isPrivate) {
    assertProject(dto, name, kee, uuid, desc, tags, isPrivate, false);
  }

  private void assertProject(ProjectDto dto, String name, String kee, String uuid, String desc, @Nullable String tags, boolean isPrivate, boolean isAiCodeAssurance) {
    assertThat(dto).extracting("name", "kee", "key", "uuid", "description", "tagsString", "private", "aiCodeAssurance")
      .containsExactly(name, kee, kee, uuid, desc, tags, isPrivate, isAiCodeAssurance);
  }

  private ProjectDto createProject(String org, String name) {
    return new ProjectDto()
      .setName("projectName_" + name)
      .setKey("projectKee_" + org + "_" + name)
      .setQualifier(Qualifiers.PROJECT)
      .setUuid("uuid_" + org + "_" + name)
      .setTags(Arrays.asList("tag1", "tag2"))
      .setDescription("desc_" + name)
      .setCreationMethod(CreationMethod.LOCAL_API)
      .setPrivate(false);
  }
}

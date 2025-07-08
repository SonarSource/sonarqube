/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.alm.setting;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.project.ProjectDto;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newAzureProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubProjectAlmSettingDto;

class ProjectAlmSettingDaoIT {

  private static final long A_DATE = 1_000_000_000_000L;
  private static final long A_DATE_LATER = 1_700_000_000_000L;

  private static final String A_UUID = "SOME_UUID";
  private static final String ANOTHER_UUID = "SOME_UUID2";
  private final TestSystem2 system2 = new TestSystem2().setNow(A_DATE);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final ProjectAlmSettingDao underTest = new ProjectAlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  @Test
  void select_by_project() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(githubAlmSettingDto, project);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto, githubAlmSettingDto.getKey(), anotherProject.getName(),
      anotherProject.getKey());

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt,
        ProjectAlmSettingDto::getSummaryCommentEnabled, ProjectAlmSettingDto::getInlineAnnotationsEnabled,
        ProjectAlmSettingDto::getMonorepo)
      .containsExactly(A_UUID, githubAlmSettingDto.getUuid(), project.getUuid(),
        githubProjectAlmSettingDto.getAlmRepo(), githubProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE, githubProjectAlmSettingDto.getSummaryCommentEnabled(), null, false);

    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotPresent();
  }

  @Test
  void select_by_alm_setting_and_slugs() {
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();
    ProjectAlmSettingDto matchingProject = createBitbucketProject(almSettingsDto, dto -> dto.setAlmSlug("slug1"));
    createBitbucketProject(almSettingsDto, dto -> dto.setAlmSlug("slug2"));

    Set<String> slugs = Set.of("slug1");
    List<ProjectAlmSettingDto> results = underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, slugs);
    
    assertThat(results)
      .extracting(ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getSummaryCommentEnabled, ProjectAlmSettingDto::getInlineAnnotationsEnabled)
      .containsExactly(tuple(matchingProject.getProjectUuid(), matchingProject.getSummaryCommentEnabled(), matchingProject.getInlineAnnotationsEnabled()));
  }

  @Test
  void selectByAlm_whenGivenGithub_onlyReturnsGithubProjects() {
    ProjectAlmSettingDto githubProject1 = createAlmProject(db.almSettings().insertGitHubAlmSetting());
    ProjectAlmSettingDto githubProject2 = createAlmProject(db.almSettings().insertGitHubAlmSetting());
    createAlmProject(db.almSettings().insertGitlabAlmSetting());

    List<ProjectAlmSettingDto> projectAlmSettingDtos = underTest.selectByAlm(dbSession, ALM.GITHUB);

    assertThat(projectAlmSettingDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(githubProject1, githubProject2);
  }

  @Test
  void selectByProjectUuidsAndAlm_whenGivenGithubAndProjectUuids_shouldOnlyReturnThose() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto githubProject = createAlmProject(githubSetting);
    createAlmProject(githubSetting);

    AlmSettingDto gitlabSetting = db.almSettings().insertGitlabAlmSetting();
    ProjectAlmSettingDto gitlabProject = createAlmProject(gitlabSetting);

    List<ProjectAlmSettingDto> projectAlmSettingDtos = underTest.selectByProjectUuidsAndAlm(dbSession, Set.of(githubProject.getProjectUuid(), gitlabProject.getProjectUuid()),
      ALM.GITHUB);

    assertThat(projectAlmSettingDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(githubProject);
  }

  private ProjectAlmSettingDto createAlmProject(AlmSettingDto almSettingsDto, Consumer<ProjectAlmSettingDto>... populators) {
    return createAlmProject(almSettingsDto, dto -> stream(populators).forEach(p -> p.accept(dto)), project -> newGithubProjectAlmSettingDto(almSettingsDto, project));
  }

  @Test
  void select_with_no_slugs_return_empty() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();

    assertThat(underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, new HashSet<>())).isEmpty();
  }

  @Test
  void select_by_alm_setting_and_repos() {
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto matchingProject = createAlmProject(almSettingsDto, dto -> dto.setAlmRepo("repo1"));
    createAlmProject(almSettingsDto, dto -> dto.setAlmRepo("repo2"));

    Set<String> repos = Set.of("repo1");
    List<ProjectAlmSettingDto> results = underTest.selectByAlmSettingAndRepos(dbSession, almSettingsDto, repos);
    
    assertThat(results)
      .extracting(ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(tuple(matchingProject.getProjectUuid(), matchingProject.getSummaryCommentEnabled()));
  }

  @Test
  void select_with_no_repos_return_empty() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();

    assertThat(underTest.selectByAlmSettingAndRepos(dbSession, almSettingsDto, new HashSet<>())).isEmpty();
  }

  @Test
  void selectAlmTypeAndUrlByProject_returnsCorrectValues() {
    when(uuidFactory.create())
      .thenReturn(A_UUID)
      .thenReturn(ANOTHER_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();

    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();

    ProjectAlmSettingDto githubProjectAlmSettingDto1 = newGithubProjectAlmSettingDto(almSettingsDto, project1, false);
    ProjectAlmSettingDto githubProjectAlmSettingDto2 = newGithubProjectAlmSettingDto(almSettingsDto, project2, true);

    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto1, almSettingsDto.getKey(), project1.getName(), project1.getKey());
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto2, almSettingsDto.getKey(), project2.getName(), project2.getKey());

    assertThat(underTest.selectAlmTypeAndUrlByProject(dbSession))
      .extracting(
        ProjectAlmKeyAndProject::getProjectUuid,
        ProjectAlmKeyAndProject::getAlmId,
        ProjectAlmKeyAndProject::getUrl,
        ProjectAlmKeyAndProject::getMonorepo)
      .containsExactlyInAnyOrder(
        tuple(project1.getUuid(), almSettingsDto.getAlm().getId(), almSettingsDto.getUrl(), false),
        tuple(project2.getUuid(), almSettingsDto.getAlm().getId(), almSettingsDto.getUrl(), true));
  }

  @Test
  void selectByUuid_whenNoResult_returnsEmptyOptional() {
    Optional<ProjectAlmSettingDto> dto = underTest.selectByUuid(dbSession, "inexistantUuid");
    assertThat(dto).isEmpty();
  }

  @Test
  void selectByUuid_whenResult_returnsIt() {
    ProjectAlmSettingDto expectedDto = createAlmProject(db.almSettings().insertGitHubAlmSetting());

    Optional<ProjectAlmSettingDto> actualDto = underTest.selectByUuid(dbSession, expectedDto.getUuid());

    assertThat(actualDto)
      .isPresent().get()
      .usingRecursiveComparison()
      .isEqualTo(expectedDto);
  }

  @Test
  void selectProjectAlmSettings_whenNoResult_returnsEmptyList() {
    List<ProjectAlmSettingDto> dtos = underTest.selectProjectAlmSettings(dbSession, new ProjectAlmSettingQuery("repository", "almSettingUuid"), 1, 100);
    assertThat(dtos).isEmpty();
  }

  @Test
  void selectProjectAlmSettings_whenResults_returnsThem() {
    AlmSettingDto matchingAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto notMatchingAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto matchingRepo = createAlmProject(matchingAlmSettingDto, dto -> dto.setAlmRepo("matchingRepo"));
    ProjectAlmSettingDto matchingAlmSetting = createAlmProject(matchingAlmSettingDto, dto -> dto.setAlmRepo("matchingRepo"));
    createAlmProject(matchingAlmSettingDto, dto -> dto.setAlmRepo("whatever")); // not matching repo
    createAlmProject(notMatchingAlmSettingDto, dto -> dto.setAlmRepo("matchingRepo")); // not matching alm setting

    ProjectAlmSettingQuery query = new ProjectAlmSettingQuery("matchingRepo", matchingAlmSettingDto.getUuid());
    List<ProjectAlmSettingDto> results = underTest.selectProjectAlmSettings(dbSession, query, 1, 100);
    
    assertThat(results)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(matchingRepo, matchingAlmSetting);
  }

  private static Object[][] paginationTestCases() {
    return new Object[][] {
      {100, 1, 5},
      {100, 3, 18},
      {2075, 41, 50},
      {0, 2, 5},
    };
  }

  @ParameterizedTest
  @MethodSource("paginationTestCases")
  void selectProjectAlmSettings_whenUsingPagination_findsTheRightResults(int numberToGenerate, int offset, int limit) {
    when(uuidFactory.create()).thenAnswer(answer -> UUID.randomUUID().toString());

    Map<String, ProjectAlmSettingDto> allProjectAlmSettingsDtos = generateProjectAlmSettingsDtos(numberToGenerate);

    ProjectAlmSettingQuery query = new ProjectAlmSettingQuery(null, null);
    List<ProjectAlmSettingDto> projectAlmSettingDtos = underTest.selectProjectAlmSettings(dbSession, query, offset, limit);

    Set<ProjectAlmSettingDto> expectedDtos = getExpectedProjectAlmSettingDtos(offset, limit, allProjectAlmSettingsDtos);

    assertThat(projectAlmSettingDtos).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expectedDtos);
    assertThat(underTest.countProjectAlmSettings(dbSession, query)).isEqualTo(numberToGenerate);
  }

  private Map<String, ProjectAlmSettingDto> generateProjectAlmSettingsDtos(int numberToGenerate) {
    if (numberToGenerate == 0) {
      return emptyMap();
    }
    Map<String, ProjectAlmSettingDto> result = IntStream.range(1000, 1000 + numberToGenerate)
      .mapToObj(i -> underTest.insertOrUpdate(dbSession, new ProjectAlmSettingDto()
        .setAlmRepo("repo_" + i)
        .setAlmSettingUuid("almSettingUuid_" + i)
        .setProjectUuid("projectUuid_" + i)
        .setMonorepo(false),
        "key_" + i, "projectName_" + i, "projectKey_" + i))
      .collect(toMap(ProjectAlmSettingDto::getAlmRepo, Function.identity()));
    db.commit();
    return result;
  }

  private Set<ProjectAlmSettingDto> getExpectedProjectAlmSettingDtos(int offset, int limit, Map<String, ProjectAlmSettingDto> allProjectAlmSettingsDtos) {
    if (allProjectAlmSettingsDtos.isEmpty()) {
      return emptySet();
    }
    return IntStream.range(1000 + (offset - 1) * limit, 1000 + offset * limit)
      .mapToObj(i -> allProjectAlmSettingsDtos.get("repo_" + i))
      .collect(toSet());
  }

  @Test
  void update_existing_github_binding() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    AlmSettingDto anotherGithubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    system2.setNow(A_DATE_LATER);
    ProjectAlmSettingDto newProjectAlmSettingDto = newGithubProjectAlmSettingDto(anotherGithubAlmSetting, project)
      .setSummaryCommentEnabled(false);
    underTest.insertOrUpdate(dbSession, newProjectAlmSettingDto, githubAlmSetting.getKey(), project.getName(), project.getKey());

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt,
        ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(projectAlmSettingDto.getUuid(), anotherGithubAlmSetting.getUuid(), project.getUuid(),
        newProjectAlmSettingDto.getAlmRepo(), newProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE_LATER, newProjectAlmSettingDto.getSummaryCommentEnabled());
  }

  @Test
  void update_existing_azure_binding() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto azureAlmSetting = db.almSettings().insertAzureAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertAzureProjectAlmSetting(azureAlmSetting, project);
    AlmSettingDto anotherAzureAlmSetting = db.almSettings().insertAzureAlmSetting();

    system2.setNow(A_DATE_LATER);
    ProjectAlmSettingDto newProjectAlmSettingDto = newAzureProjectAlmSettingDto(anotherAzureAlmSetting, project)
      .setInlineAnnotationsEnabled(false);
    underTest.insertOrUpdate(dbSession, newProjectAlmSettingDto, azureAlmSetting.getKey(), project.getName(), project.getKey());

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt,
        ProjectAlmSettingDto::getInlineAnnotationsEnabled, ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(projectAlmSettingDto.getUuid(), anotherAzureAlmSetting.getUuid(), project.getUuid(),
        newProjectAlmSettingDto.getAlmRepo(), newProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE_LATER, newProjectAlmSettingDto.getInlineAnnotationsEnabled(),
        newProjectAlmSettingDto.getSummaryCommentEnabled());
  }

  @Test
  void deleteByProject() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, anotherProject);

    underTest.deleteByProject(dbSession, project);

    assertThat(underTest.selectByProject(dbSession, project)).isEmpty();
    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotEmpty();
  }

  @Test
  void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project1);
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project2);

    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting1, anotherProject);

    underTest.deleteByAlmSetting(dbSession, githubAlmSetting);

    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting)).isZero();
    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting1)).isOne();
  }

  @Test
  void selectProjectAlmSettings_whenSearchingByAlmRepo_returnsMatchingResults() {
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto matchingProject = createAlmProject(almSetting, dto -> dto.setAlmRepo("target-repo"));
    createAlmProject(almSetting, dto -> dto.setAlmRepo("other-repo"));
    createAlmProject(almSetting, dto -> dto.setAlmSlug("target-repo")); // slug should not match

    ProjectAlmSettingQuery query = ProjectAlmSettingQuery.forAlmRepo("target-repo");
    List<ProjectAlmSettingDto> results = underTest.selectProjectAlmSettings(dbSession, query, 1, 100);

    assertThat(results)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactly(matchingProject);
  }

  @Test
  void selectProjectAlmSettings_whenSearchingByAlmRepoAndSlug_returnsMatchingResults() {
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    ProjectAlmSettingDto matchingProject = createAzureProject(almSetting, "target-repo", "target-project");
    createAzureProject(almSetting, "target-repo", "other-project"); // different slug
    createAzureProject(almSetting, "other-repo", "target-project"); // different repo
    createAlmProject(almSetting, dto -> dto.setAlmRepo("target-repo")); // missing slug

    ProjectAlmSettingQuery query = ProjectAlmSettingQuery.forAlmRepoAndSlug("target-repo", "target-project");
    List<ProjectAlmSettingDto> results = underTest.selectProjectAlmSettings(dbSession, query, 1, 100);

    assertThat(results)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactly(matchingProject);
  }

  @Test
  void selectProjectAlmSettings_whenSearchingByCaseInsensitiveAlmRepo_returnsMatchingResults() {
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto upperCaseProject = createAlmProject(almSetting, dto -> dto.setAlmRepo("TARGET-REPO"));
    ProjectAlmSettingDto lowerCaseProject = createAlmProject(almSetting, dto -> dto.setAlmRepo("target-repo"));
    createAlmProject(almSetting, dto -> dto.setAlmRepo("other-repo"));

    ProjectAlmSettingQuery query = ProjectAlmSettingQuery.forAlmRepo("Target-Repo");
    List<ProjectAlmSettingDto> results = underTest.selectProjectAlmSettings(dbSession, query, 1, 100);

    assertThat(results)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(upperCaseProject, lowerCaseProject);
  }

  @Test
  void countProjectAlmSettings_whenSearchingByAlmRepo_returnsCorrectCount() {
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    createAlmProject(almSetting, dto -> dto.setAlmRepo("target-repo"));
    createAlmProject(almSetting, dto -> dto.setAlmRepo("target-repo"));
    createAlmProject(almSetting, dto -> dto.setAlmRepo("other-repo"));

    ProjectAlmSettingQuery query = ProjectAlmSettingQuery.forAlmRepo("target-repo");
    int count = underTest.countProjectAlmSettings(dbSession, query);

    assertThat(count).isEqualTo(2);
  }

  private ProjectAlmSettingDto createAzureProject(AlmSettingDto almSettingsDto, String almRepo, String almSlug) {
    return createAlmProject(almSettingsDto, dto -> {
      dto.setAlmRepo(almRepo);
      dto.setAlmSlug(almSlug);
    }, project -> newAzureProjectAlmSettingDto(almSettingsDto, project));
  }

  private ProjectAlmSettingDto createBitbucketProject(AlmSettingDto almSettingsDto, Consumer<ProjectAlmSettingDto> customizer) {
    return createAlmProject(almSettingsDto, customizer, project -> newBitbucketProjectAlmSettingDto(almSettingsDto, project));
  }

  private ProjectAlmSettingDto createAlmProject(AlmSettingDto almSettingsDto, Consumer<ProjectAlmSettingDto> customizer, Function<ProjectDto, ProjectAlmSettingDto> dtoFactory) {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    when(uuidFactory.create()).thenReturn(project.getUuid() + "_set");
    ProjectAlmSettingDto projectAlmSettingDto = dtoFactory.apply(project);
    customizer.accept(projectAlmSettingDto);
    underTest.insertOrUpdate(dbSession, projectAlmSettingDto, almSettingsDto.getKey(), project.getName(), project.getKey());
    return projectAlmSettingDto;
  }

}

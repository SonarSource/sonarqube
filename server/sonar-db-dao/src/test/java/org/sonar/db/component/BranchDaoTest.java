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

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToProjectBranchesTable;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

@RunWith(DataProviderRunner.class)
public class BranchDaoTest {

  private static final long NOW = 1_000L;
  private static final String SELECT_FROM = "select project_uuid as \"projectUuid\", uuid as \"uuid\", branch_type as \"branchType\",  " +
    "kee as \"kee\", merge_branch_uuid as \"mergeBranchUuid\", pull_request_binary as \"pullRequestBinary\", created_at as \"createdAt\", updated_at as \"updatedAt\" " +
    "from project_branches ";
  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private BranchDao underTest = new BranchDao(system2);

  @Test
  public void insert_branch_with_only_nonnull_fields() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature/foo");

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat(map).contains(
      entry("projectUuid", "U1"),
      entry("uuid", "U2"),
      entry("branchType", "BRANCH"),
      entry("kee", "feature/foo"),
      entry("mergeBranchUuid", null),
      entry("pullRequestBinary", null),
      entry("createdAt", 1_000L),
      entry("updatedAt", 1_000L));
  }

  @Test
  public void update_main_branch_name() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    underTest.insert(dbSession, dto);

    BranchDto dto2 = new BranchDto();
    dto2.setProjectUuid("U2");
    dto2.setUuid("U2");
    dto2.setBranchType(BranchType.BRANCH);
    dto2.setKey("branch");
    underTest.insert(dbSession, dto2);

    underTest.updateBranchName(dbSession, "U1", "master");
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "master").get();
    assertThat(loaded.getMergeBranchUuid()).isNull();
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.BRANCH);
  }

  @Test
  public void updateExcludeFromPurge() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    dto.setExcludeFromPurge(false);
    underTest.insert(dbSession, dto);

    underTest.updateExcludeFromPurge(dbSession, "U1", true);

    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "feature").get();
    assertThat(loaded.isExcludeFromPurge()).isTrue();
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][]{
      {null},
      {""}
    };
  }

  @DataProvider
  public static Object[][] oldAndNewValuesCombinations() {
    String value1 = randomAlphabetic(10);
    String value2 = randomAlphabetic(20);
    return new Object[][]{
      {null, value1},
      {"", value1},
      {value1, null},
      {value1, ""},
      {value1, value2},
      {null, null},
      {"", null},
      {value1, value1}
    };
  }

  @Test
  public void insert_branch_with_all_fields_and_max_length_values() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid(repeat("a", 50));
    dto.setUuid(repeat("b", 50));
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey(repeat("c", 255));
    dto.setMergeBranchUuid(repeat("d", 50));

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat((String) map.get("projectUuid")).contains("a").isEqualTo(dto.getProjectUuid());
    assertThat((String) map.get("uuid")).contains("b").isEqualTo(dto.getUuid());
    assertThat((String) map.get("kee")).contains("c").isEqualTo(dto.getKey());
    assertThat((String) map.get("mergeBranchUuid")).contains("d").isEqualTo(dto.getMergeBranchUuid());
  }

  @Test
  public void insert_pull_request_branch_with_only_non_null_fields() {
    String projectUuid = "U1";
    String uuid = "U2";
    BranchType branchType = BranchType.PULL_REQUEST;
    String kee = "123";

    BranchDto dto = new BranchDto();
    dto.setProjectUuid(projectUuid);
    dto.setUuid(uuid);
    dto.setBranchType(branchType);
    dto.setKey(kee);

    underTest.insert(dbSession, dto);

    BranchDto loaded = underTest.selectByUuid(dbSession, dto.getUuid()).get();

    assertThat(loaded.getProjectUuid()).isEqualTo(projectUuid);
    assertThat(loaded.getUuid()).isEqualTo(uuid);
    assertThat(loaded.getBranchType()).isEqualTo(branchType);
    assertThat(loaded.getKey()).isEqualTo(kee);
    assertThat(loaded.getMergeBranchUuid()).isNull();
    assertThat(loaded.getPullRequestData()).isNull();
  }

  @Test
  public void insert_pull_request_branch_with_all_fields() {
    String projectUuid = "U1";
    String uuid = "U2";
    BranchType branchType = BranchType.PULL_REQUEST;
    String kee = "123";

    String branch = "feature/pr1";
    String title = "Dummy Feature Title";
    String url = "http://example.com/pullRequests/pr1";
    String tokenAttributeName = "token";
    String tokenAttributeValue = "dummy token";
    DbProjectBranches.PullRequestData pullRequestData = DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(branch)
      .setTitle(title)
      .setUrl(url)
      .putAttributes(tokenAttributeName, tokenAttributeValue)
      .build();

    BranchDto dto = new BranchDto();
    dto.setProjectUuid(projectUuid);
    dto.setUuid(uuid);
    dto.setBranchType(branchType);
    dto.setKey(kee);
    dto.setPullRequestData(pullRequestData);

    underTest.insert(dbSession, dto);

    BranchDto loaded = underTest.selectByUuid(dbSession, dto.getUuid()).get();

    assertThat(loaded.getProjectUuid()).isEqualTo(projectUuid);
    assertThat(loaded.getUuid()).isEqualTo(uuid);
    assertThat(loaded.getBranchType()).isEqualTo(branchType);
    assertThat(loaded.getKey()).isEqualTo(kee);
    assertThat(loaded.getMergeBranchUuid()).isNull();

    DbProjectBranches.PullRequestData loadedPullRequestData = loaded.getPullRequestData();
    assertThat(loadedPullRequestData).isNotNull();
    assertThat(loadedPullRequestData.getBranch()).isEqualTo(branch);
    assertThat(loadedPullRequestData.getTitle()).isEqualTo(title);
    assertThat(loadedPullRequestData.getUrl()).isEqualTo(url);
    assertThat(loadedPullRequestData.getAttributesMap()).containsEntry(tokenAttributeName, tokenAttributeValue);
  }

  @Test
  public void upsert_branch() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("foo");
    underTest.insert(dbSession, dto);

    // the fields that can be updated
    dto.setMergeBranchUuid("U3");

    // the fields that can't be updated. New values are ignored.
    dto.setProjectUuid("ignored");
    dto.setBranchType(BranchType.BRANCH);
    underTest.upsert(dbSession, dto);

    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "foo").get();
    assertThat(loaded.getMergeBranchUuid()).isEqualTo("U3");
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.BRANCH);
  }

  @Test
  public void upsert_pull_request() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.PULL_REQUEST);
    dto.setKey("foo");
    underTest.insert(dbSession, dto);

    // the fields that can be updated
    dto.setMergeBranchUuid("U3");

    String branch = "feature/pr1";
    String title = "Dummy Feature Title";
    String url = "http://example.com/pullRequests/pr1";
    String tokenAttributeName = "token";
    String tokenAttributeValue = "dummy token";
    DbProjectBranches.PullRequestData pullRequestData = DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(branch)
      .setTitle(title)
      .setUrl(url)
      .putAttributes(tokenAttributeName, tokenAttributeValue)
      .build();
    dto.setPullRequestData(pullRequestData);

    // the fields that can't be updated. New values are ignored.
    dto.setProjectUuid("ignored");
    dto.setBranchType(BranchType.BRANCH);
    underTest.upsert(dbSession, dto);

    BranchDto loaded = underTest.selectByPullRequestKey(dbSession, "U1", "foo").get();
    assertThat(loaded.getMergeBranchUuid()).isEqualTo("U3");
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.PULL_REQUEST);

    DbProjectBranches.PullRequestData loadedPullRequestData = loaded.getPullRequestData();
    assertThat(loadedPullRequestData).isNotNull();
    assertThat(loadedPullRequestData.getBranch()).isEqualTo(branch);
    assertThat(loadedPullRequestData.getTitle()).isEqualTo(title);
    assertThat(loadedPullRequestData.getUrl()).isEqualTo(url);
    assertThat(loadedPullRequestData.getAttributesMap()).containsEntry(tokenAttributeName, tokenAttributeValue);
  }

  @Test
  public void update_pull_request_data() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.PULL_REQUEST);
    dto.setKey("foo");

    // the fields that can be updated
    String mergeBranchUuid = "U3";
    dto.setMergeBranchUuid(mergeBranchUuid + "-dummy-suffix");

    String branch = "feature/pr1";
    String title = "Dummy Feature Title";
    String url = "http://example.com/pullRequests/pr1";
    String tokenAttributeName = "token";
    String tokenAttributeValue = "dummy token";
    DbProjectBranches.PullRequestData pullRequestData = DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(branch + "-dummy-suffix")
      .setTitle(title + "-dummy-suffix")
      .setUrl(url + "-dummy-suffix")
      .putAttributes(tokenAttributeName, tokenAttributeValue + "-dummy-suffix")
      .build();
    dto.setPullRequestData(pullRequestData);

    underTest.insert(dbSession, dto);

    // modify pull request data

    dto.setMergeBranchUuid(mergeBranchUuid);
    pullRequestData = DbProjectBranches.PullRequestData.newBuilder()
      .setBranch(branch)
      .setTitle(title)
      .setUrl(url)
      .putAttributes(tokenAttributeName, tokenAttributeValue)
      .build();
    dto.setPullRequestData(pullRequestData);

    underTest.upsert(dbSession, dto);

    BranchDto loaded = underTest.selectByPullRequestKey(dbSession, "U1", "foo").get();
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(mergeBranchUuid);
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.PULL_REQUEST);

    DbProjectBranches.PullRequestData loadedPullRequestData = loaded.getPullRequestData();
    assertThat(loadedPullRequestData).isNotNull();
    assertThat(loadedPullRequestData.getBranch()).isEqualTo(branch);
    assertThat(loadedPullRequestData.getTitle()).isEqualTo(title);
    assertThat(loadedPullRequestData.getUrl()).isEqualTo(url);
    assertThat(loadedPullRequestData.getAttributesMap()).containsEntry(tokenAttributeName, tokenAttributeValue);
  }

  @Test
  public void selectByBranchKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setBranchType(BranchType.BRANCH);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    underTest.insert(dbSession, featureBranch);

    // select the feature branch
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "feature/foo").get();
    assertThat(loaded.getUuid()).isEqualTo(featureBranch.getUuid());
    assertThat(loaded.getKey()).isEqualTo(featureBranch.getKey());
    assertThat(loaded.getProjectUuid()).isEqualTo(featureBranch.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(featureBranch.getBranchType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(featureBranch.getMergeBranchUuid());

    // select a branch on another project with same branch name
    assertThat(underTest.selectByBranchKey(dbSession, "U3", "feature/foo")).isEmpty();
  }

  @Test
  public void selectByBranchKeys() {
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    ProjectDto project3 = db.components().insertPrivateProjectDto();

    BranchDto branch1 = db.components().insertProjectBranch(project1, b -> b.setKey("branch1"));
    BranchDto branch2 = db.components().insertProjectBranch(project2, b -> b.setKey("branch2"));
    db.components().insertProjectBranch(project3, b -> b.setKey("branch3"));

    Map<String, String> branchKeysByProjectUuid = new HashMap<>();
    branchKeysByProjectUuid.put(project1.getUuid(), "branch1");
    branchKeysByProjectUuid.put(project2.getUuid(), "branch2");
    branchKeysByProjectUuid.put(project3.getUuid(), "nonexisting");

    List<BranchDto> branchDtos = underTest.selectByBranchKeys(dbSession, branchKeysByProjectUuid);
    assertThat(branchDtos).hasSize(2);
    assertThat(branchDtos).extracting(BranchDto::getUuid).containsExactlyInAnyOrder(branch1.getUuid(), branch2.getUuid());
  }

  @Test
  public void selectByComponent() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setBranchType(BranchType.BRANCH);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    underTest.insert(dbSession, featureBranch);

    ComponentDto component = new ComponentDto().setBranchUuid(mainBranch.getUuid());

    // select the component
    Collection<BranchDto> branches = underTest.selectByComponent(dbSession, component);

    assertThat(branches).hasSize(2);

    assertThat(branches).extracting(BranchDto::getUuid, BranchDto::getKey, BranchDto::getProjectUuid, BranchDto::getBranchType, BranchDto::getMergeBranchUuid)
      .containsOnly(tuple(mainBranch.getUuid(), mainBranch.getKey(), mainBranch.getProjectUuid(), mainBranch.getBranchType(), mainBranch.getMergeBranchUuid()),
        tuple(featureBranch.getUuid(), featureBranch.getKey(), featureBranch.getProjectUuid(), featureBranch.getBranchType(), featureBranch.getMergeBranchUuid()));
  }

  @Test
  public void selectByPullRequestKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    String pullRequestId = "123";
    BranchDto pullRequest = new BranchDto();
    pullRequest.setProjectUuid("U1");
    pullRequest.setUuid("U2");
    pullRequest.setBranchType(BranchType.PULL_REQUEST);
    pullRequest.setKey(pullRequestId);
    pullRequest.setMergeBranchUuid("U3");
    underTest.insert(dbSession, pullRequest);

    // select the feature branch
    BranchDto loaded = underTest.selectByPullRequestKey(dbSession, "U1", pullRequestId).get();
    assertThat(loaded.getUuid()).isEqualTo(pullRequest.getUuid());
    assertThat(loaded.getKey()).isEqualTo(pullRequest.getKey());
    assertThat(loaded.getProjectUuid()).isEqualTo(pullRequest.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(pullRequest.getBranchType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(pullRequest.getMergeBranchUuid());

    // select a branch on another project with same branch name
    assertThat(underTest.selectByPullRequestKey(dbSession, "U3", pullRequestId)).isEmpty();
  }

  @Test
  public void selectByKeys() {
    BranchDto mainBranch = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U1")
      .setBranchType(BranchType.BRANCH)
      .setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U2")
      .setBranchType(BranchType.BRANCH)
      .setKey("feature1");
    underTest.insert(dbSession, featureBranch);

    String pullRequestId = "123";
    BranchDto pullRequest = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U3")
      .setBranchType(BranchType.PULL_REQUEST)
      .setKey(pullRequestId)
      .setMergeBranchUuid("U4");
    underTest.insert(dbSession, pullRequest);

    assertThat(underTest.selectByKeys(dbSession, "U1", Collections.emptySet()))
      .isEmpty();

    List<BranchDto> loaded = underTest.selectByKeys(dbSession, "U1", Set.of(mainBranch.getKey(), featureBranch.getKey()));
    assertThat(loaded)
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(mainBranch.getUuid(), featureBranch.getUuid());
  }

  @Test
  public void selectByUuids() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    ComponentDto branch2 = db.components().insertProjectBranch(project);
    ComponentDto branch3 = db.components().insertProjectBranch(project);

    assertThat(underTest.selectByUuids(db.getSession(), asList(branch1.uuid(), branch2.uuid(), branch3.uuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(branch1.uuid(), branch2.uuid(), branch3.uuid());
    assertThat(underTest.selectByUuids(db.getSession(), singletonList(branch1.uuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(branch1.uuid());
    assertThat(underTest.selectByUuids(db.getSession(), singletonList("unknown"))).isEmpty();
  }

  @Test
  public void selectByProjectUuid() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    ComponentDto branch1 = db.components().insertProjectBranch(project1);
    ComponentDto branch2 = db.components().insertProjectBranch(project1);
    ComponentDto branch3 = db.components().insertProjectBranch(project2);
    ComponentDto branch4 = db.components().insertProjectBranch(project2);

    assertThat(underTest.selectByProject(dbSession, new ProjectDto().setUuid(project1.uuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(project1.uuid(), branch1.uuid(), branch2.uuid());
    assertThat(underTest.selectByProject(dbSession, new ProjectDto().setUuid(project2.uuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(project2.uuid(), branch3.uuid(), branch4.uuid());
  }

  @Test
  public void selectByUuid() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    db.components().insertProjectBranch(project);

    assertThat(underTest.selectByUuid(db.getSession(), branch1.uuid()).get())
      .extracting(BranchDto::getUuid)
      .isEqualTo(branch1.uuid());
    assertThat(underTest.selectByUuid(db.getSession(), project.uuid())).isPresent();
    assertThat(underTest.selectByUuid(db.getSession(), "unknown")).isNotPresent();
  }

  @Test
  public void countPrAndBranchByProjectUuid() {
    ComponentDto project1 = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BRANCH).setKey("p1-branch-1"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BRANCH).setKey("p1-branch-2"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-1"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-2"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-3"));

    ComponentDto project2 = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project2, b -> b.setBranchType(PULL_REQUEST).setKey("p2-pr-1"));

    ComponentDto project3 = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project3, b -> b.setBranchType(BRANCH).setKey("p3-branch-1"));

    MetricDto unanalyzedC = db.measures().insertMetric(m -> m.setKey("unanalyzed_c"));
    MetricDto unanalyzedCpp = db.measures().insertMetric(m -> m.setKey("unanalyzed_cpp"));
    db.measures().insertLiveMeasure(project1, unanalyzedC);
    db.measures().insertLiveMeasure(project1, unanalyzedCpp);
    db.measures().insertLiveMeasure(project2, unanalyzedCpp);
    db.measures().insertLiveMeasure(project3, unanalyzedC);

    assertThat(underTest.countPrBranchAnalyzedLanguageByProjectUuid(db.getSession()))
      .extracting(PrBranchAnalyzedLanguageCountByProjectDto::getProjectUuid, PrBranchAnalyzedLanguageCountByProjectDto::getBranch,
        PrBranchAnalyzedLanguageCountByProjectDto::getPullRequest)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), 3L, 3L),
        tuple(project2.uuid(), 1L, 1L),
        tuple(project3.uuid(), 2L, 0L)
      );
  }

  @Test
  public void selectProjectUuidsWithIssuesNeedSync() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto project4 = db.components().insertPrivateProject();
    ProjectDto project1Dto = db.components().getProjectDto(project1);
    ProjectDto project2Dto = db.components().getProjectDto(project2);
    ProjectDto project3Dto = db.components().getProjectDto(project3);
    ProjectDto project4Dto = db.components().getProjectDto(project4);

    db.components().insertProjectBranch(project1Dto, branchDto -> branchDto.setNeedIssueSync(true));
    db.components().insertProjectBranch(project1Dto, branchDto -> branchDto.setNeedIssueSync(false));
    db.components().insertProjectBranch(project2Dto);

    assertThat(underTest.selectProjectUuidsWithIssuesNeedSync(db.getSession(),
      Sets.newHashSet(project1Dto.getUuid(), project2Dto.getUuid(), project3Dto.getUuid(), project4Dto.getUuid())))
      .containsOnly(project1.uuid());
  }

  @Test
  public void hasAnyBranchWhereNeedIssueSync() {
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isFalse();
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, false)).isFalse();

    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project, b -> b.setNeedIssueSync(false));

    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isFalse();
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, false)).isTrue();

    project = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project, b -> b.setNeedIssueSync(true));
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isTrue();
  }

  @Test
  public void countByTypeAndCreationDate() {
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, 0L)).isZero();

    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, 0L)).isEqualTo(3);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, NOW)).isEqualTo(3);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, NOW + 100)).isZero();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, 0L)).isOne();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW)).isOne();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW + 100)).isZero();
  }

  @Test
  public void countByNeedIssueSync() {
    assertThat(underTest.countByNeedIssueSync(dbSession, true)).isZero();
    assertThat(underTest.countByNeedIssueSync(dbSession, false)).isZero();

    // master branch with flag set to false
    ComponentDto project = db.components().insertPrivateProject();
    // branches & PRs
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));

    assertThat(underTest.countByNeedIssueSync(dbSession, true)).isEqualTo(4);
    assertThat(underTest.countByNeedIssueSync(dbSession, false)).isEqualTo(4);
  }

  @Test
  public void selectUuidsWithMeasuresMigratedFalse() throws SQLException {
    createMeasuresMigratedColumn();

    // master branch with flag set to false
    ComponentDto notMigratedProject = db.components().insertPrivateProject();
    // branches & PRs
    ComponentDto migratedBranch = db.components().insertProjectBranch(notMigratedProject, b -> b.setBranchType(BRANCH));
    ComponentDto notMigratedBranch = db.components().insertProjectBranch(notMigratedProject, b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto migratedPR = db.components().insertProjectBranch(notMigratedProject, b -> b.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto notMigratedPR = db.components().insertProjectBranch(notMigratedProject, b -> b.setBranchType(PULL_REQUEST));

    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, migratedBranch.branchUuid(), true);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, notMigratedBranch.branchUuid(), false);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, migratedPR.branchUuid(), true);

    assertThat(underTest.selectUuidsWithMeasuresMigratedFalse(dbSession, 10))
      .hasSize(3)
      .containsOnly(notMigratedProject.branchUuid(), notMigratedBranch.branchUuid(), notMigratedPR.branchUuid());

    assertThat(underTest.selectUuidsWithMeasuresMigratedFalse(dbSession, 1))
      .hasSize(1)
      .containsOnly(notMigratedProject.branchUuid());
  }

  @Test
  public void countByMeasuresMigratedFalse() throws SQLException {
    createMeasuresMigratedColumn();

    // master branch with flag set to false
    ComponentDto project = db.components().insertPrivateProject();
    // branches & PRs
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto branch3 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));

    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch1.branchUuid(), true);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch2.branchUuid(), false);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch3.branchUuid(), true);

    assertThat(underTest.countByMeasuresMigratedFalse(dbSession)).isEqualTo(3);
  }

  @Test
  public void updateMeasuresMigrated() throws SQLException {
    createMeasuresMigratedColumn();

    ComponentDto project = db.components().insertPrivateProject();
    String uuid1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH)).uuid();
    String uuid2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH)).uuid();

    underTest.updateMeasuresMigrated(dbSession, uuid1, true);
    underTest.updateMeasuresMigrated(dbSession, uuid2, false);

    assertThat(underTest.isMeasuresMigrated(dbSession, uuid1)).isTrue();
    assertThat(underTest.isMeasuresMigrated(dbSession, uuid2)).isFalse();
  }

  @Test
  public void isMeasuresMigrated() throws SQLException {
    createMeasuresMigratedColumn();

    // master branch with flag set to false
    ComponentDto project = db.components().insertPrivateProject();
    // branches & PRs
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH));
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto branch3 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto branch4 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));

    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch1.branchUuid(), true);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch2.branchUuid(), false);
    db.getDbClient().branchDao().updateMeasuresMigrated(dbSession, branch3.branchUuid(), true);

    assertThat(underTest.isMeasuresMigrated(dbSession, project.uuid())).isFalse();
    assertThat(underTest.isMeasuresMigrated(dbSession, branch1.uuid())).isTrue();
    assertThat(underTest.isMeasuresMigrated(dbSession, branch2.uuid())).isFalse();
    assertThat(underTest.isMeasuresMigrated(dbSession, branch3.uuid())).isTrue();
    assertThat(underTest.isMeasuresMigrated(dbSession, branch4.uuid())).isFalse();
  }

  private void createMeasuresMigratedColumn() throws SQLException {
    AddMeasuresMigratedColumnToProjectBranchesTable migration = new AddMeasuresMigratedColumnToProjectBranchesTable(db.getDbClient().getDatabase());
    migration.execute();
  }

  @Test
  public void countAll() {
    assertThat(underTest.countAll(dbSession)).isZero();

    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));

    assertThat(underTest.countAll(dbSession)).isEqualTo(8);
  }

  @Test
  public void selectBranchNeedingIssueSync() {
    ComponentDto project = db.components().insertPrivateProject();
    String uuid = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));

    assertThat(underTest.selectBranchNeedingIssueSync(dbSession))
      .extracting(BranchDto::getUuid)
      .containsExactly(uuid);
  }

  @Test
  public void selectBranchNeedingIssueSyncForProject() {
    ComponentDto project = db.components().insertPrivateProject();
    String uuid = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));

    assertThat(underTest.selectBranchNeedingIssueSyncForProject(dbSession, project.uuid()))
      .extracting(BranchDto::getUuid)
      .containsExactly(uuid);
  }

  @Test
  public void updateAllNeedIssueSync() {
    ComponentDto project = db.components().insertPrivateProject();
    String uuid1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    String uuid2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false)).uuid();

    underTest.updateAllNeedIssueSync(dbSession);

    Optional<BranchDto> project1 = underTest.selectByUuid(dbSession, uuid1);
    assertThat(project1).isPresent();
    assertThat(project1.get().isNeedIssueSync()).isTrue();

    Optional<BranchDto> project2 = underTest.selectByUuid(dbSession, uuid2);
    assertThat(project2).isPresent();
    assertThat(project2.get().isNeedIssueSync()).isTrue();
  }

  @Test
  public void updateAllNeedIssueSyncForProject() {
    ComponentDto project = db.components().insertPrivateProject();
    String uuid1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    String uuid2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false)).uuid();

    underTest.updateAllNeedIssueSyncForProject(dbSession, project.uuid());

    Optional<BranchDto> project1 = underTest.selectByUuid(dbSession, uuid1);
    assertThat(project1).isPresent();
    assertThat(project1.get().isNeedIssueSync()).isTrue();

    Optional<BranchDto> project2 = underTest.selectByUuid(dbSession, uuid2);
    assertThat(project2).isPresent();
    assertThat(project2.get().isNeedIssueSync()).isTrue();
  }

  @Test
  public void updateNeedIssueSync() {
    ComponentDto project = db.components().insertPrivateProject();
    String uuid1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false)).uuid();
    String uuid2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();

    underTest.updateNeedIssueSync(dbSession, uuid1, true);
    underTest.updateNeedIssueSync(dbSession, uuid2, false);

    Optional<BranchDto> project1 = underTest.selectByUuid(dbSession, uuid1);
    assertThat(project1).isPresent();
    assertThat(project1.get().isNeedIssueSync()).isTrue();

    Optional<BranchDto> project2 = underTest.selectByUuid(dbSession, uuid2);
    assertThat(project2).isPresent();
    assertThat(project2.get().isNeedIssueSync()).isFalse();
  }

  @Test
  public void doAnyOfComponentsNeedIssueSync() {
    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, emptyList())).isFalse();

    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ProjectDto projectDto = db.components().getProjectDto(project1);
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, singletonList(project1.getKey()))).isTrue();
    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, singletonList(project2.getKey()))).isFalse();
  }

  @Test
  public void doAnyOfComponentsNeedIssueSync_test_more_than_1000() {
    List<String> componentKeys = IntStream.range(0, 1100).mapToObj(value -> db.components().insertPrivateProject())
      .map(ComponentDto::getKey)
      .collect(Collectors.toList());

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, componentKeys)).isFalse();

    ComponentDto project = db.components().insertPrivateProject();
    ProjectDto projectDto = db.components().getProjectDto(project);
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));

    componentKeys.add(project.getKey());

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, componentKeys)).isTrue();
  }

  @Test
  public void updateMeasuresMigratedToFalse() throws SQLException {
    createMeasuresMigratedColumn();

    ComponentDto project = db.components().insertPrivateProject();
    String uuid1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH)).uuid();
    String uuid2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH)).uuid();

    underTest.updateMeasuresMigrated(dbSession, uuid1, true);
    underTest.updateMeasuresMigrated(dbSession, uuid2, true);

    underTest.updateMeasuresMigratedToFalse(dbSession);

    assertThat(underTest.isMeasuresMigrated(dbSession, uuid1)).isFalse();
    assertThat(underTest.isMeasuresMigrated(dbSession, uuid2)).isFalse();
  }
}

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

class BranchDaoIT {

  private static final long NOW = 1_000L;
  private static final String SELECT_FROM = """
    select project_uuid as "projectUuid", uuid as "uuid", branch_type as "branchType",
    kee as "kee", merge_branch_uuid as "mergeBranchUuid", pull_request_binary as "pullRequestBinary", created_at as "createdAt", updated_at as "updatedAt", is_main as "isMain"
    from project_branches""";
  private final System2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final BranchDao underTest = new BranchDao(system2);

  @Test
  void insert_branch_with_only_nonnull_fields() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.BRANCH);
    dto.setIsMain(true);
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
    assertThat(map.get("isMain")).isIn(true, 1L); // Oracle returns 1L instead of true
  }

  @Test
  void update_main_branch_name() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setIsMain(true);
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    underTest.insert(dbSession, dto);

    BranchDto dto2 = new BranchDto();
    dto2.setProjectUuid("U2");
    dto2.setUuid("U2");
    dto2.setIsMain(true);
    dto2.setBranchType(BranchType.BRANCH);
    dto2.setKey("branch");
    underTest.insert(dbSession, dto2);

    underTest.updateBranchName(dbSession, "U1", "master");
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "master").get();
    assertThat(loaded.getMergeBranchUuid()).isNull();
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.BRANCH);
    assertThat(loaded.isMain()).isTrue();
  }

  @Test
  void selectBranchMeasuresForTelemetry() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    dto.setIsMain(true);
    dto.setExcludeFromPurge(false);
    underTest.insert(dbSession, dto);

    MetricDto qg = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
    SnapshotDto analysis = db.components().insertSnapshot(dto);
    db.measures().insertMeasure(dto, analysis, qg, pm -> pm.setData("OK"));

    var branchMeasures = underTest.selectBranchMeasuresWithCaycMetric(dbSession);

    assertThat(branchMeasures)
      .hasSize(1)
      .extracting(BranchMeasuresDto::getBranchUuid, BranchMeasuresDto::getBranchKey, BranchMeasuresDto::getProjectUuid,
        BranchMeasuresDto::getAnalysisCount, BranchMeasuresDto::getGreenQualityGateCount, BranchMeasuresDto::getExcludeFromPurge)
      .containsExactly(tuple("U1", "feature", "U1", 1, 1, false));
  }

  @Test
  void updateExcludeFromPurge() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    dto.setIsMain(true);
    dto.setExcludeFromPurge(false);
    underTest.insert(dbSession, dto);

    underTest.updateExcludeFromPurge(dbSession, "U1", true);

    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "feature").get();
    assertThat(loaded.isExcludeFromPurge()).isTrue();
  }

  @Test
  void insert_branch_with_all_fields_and_max_length_values() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid(repeat("a", 50));
    dto.setUuid(repeat("b", 50));
    dto.setIsMain(false);
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
  void insert_pull_request_branch_with_only_non_null_fields() {
    String projectUuid = "U1";
    String uuid = "U2";
    BranchType branchType = BranchType.PULL_REQUEST;
    String kee = "123";

    BranchDto dto = new BranchDto();
    dto.setProjectUuid(projectUuid);
    dto.setUuid(uuid);
    dto.setIsMain(false);
    dto.setBranchType(branchType);
    dto.setKey(kee);

    underTest.insert(dbSession, dto);

    BranchDto loaded = underTest.selectByUuid(dbSession, dto.getUuid()).get();

    assertThat(loaded.getProjectUuid()).isEqualTo(projectUuid);
    assertThat(loaded.getUuid()).isEqualTo(uuid);
    assertThat(loaded.isMain()).isFalse();
    assertThat(loaded.getBranchType()).isEqualTo(branchType);
    assertThat(loaded.getKey()).isEqualTo(kee);
    assertThat(loaded.getMergeBranchUuid()).isNull();
    assertThat(loaded.getPullRequestData()).isNull();
  }

  @Test
  void insert_pull_request_branch_with_all_fields() {
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
    dto.setIsMain(false);
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
  void upsert_branch() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setIsMain(false);
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
  void upsert_pull_request() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.PULL_REQUEST);
    dto.setKey("foo");
    dto.setIsMain(false);
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
  void update_pull_request_data() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setIsMain(false);
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
  void selectByBranchKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setIsMain(true);
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setIsMain(false);
    featureBranch.setBranchType(BranchType.BRANCH);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    underTest.insert(dbSession, featureBranch);

    // select the feature branch
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "feature/foo").get();
    assertThat(loaded.getUuid()).isEqualTo(featureBranch.getUuid());
    assertThat(loaded.getKey()).isEqualTo(featureBranch.getKey());
    assertThat(loaded.isMain()).isFalse();
    assertThat(loaded.getProjectUuid()).isEqualTo(featureBranch.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(featureBranch.getBranchType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(featureBranch.getMergeBranchUuid());

    // select a branch on another project with same branch name
    assertThat(underTest.selectByBranchKey(dbSession, "U3", "feature/foo")).isEmpty();
  }

  @Test
  void selectByBranchKeys() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();

    BranchDto branch1 = db.components().insertProjectBranch(project1, b -> b.setKey("branch1"));
    BranchDto branch2 = db.components().insertProjectBranch(project2, b -> b.setKey("branch2"));
    BranchDto branch3 = db.components().insertProjectBranch(project3, b -> b.setKey("branch3"));

    Map<String, String> branchKeysByProjectUuid = new HashMap<>();
    branchKeysByProjectUuid.put(project1.getUuid(), "branch1");
    branchKeysByProjectUuid.put(project2.getUuid(), "branch2");
    branchKeysByProjectUuid.put(project3.getUuid(), "nonexisting");

    List<BranchDto> branchDtos = underTest.selectByBranchKeys(dbSession, branchKeysByProjectUuid);
    assertThat(branchDtos).hasSize(2);
    assertThat(branchDtos).extracting(BranchDto::getUuid).containsExactlyInAnyOrder(branch1.getUuid(), branch2.getUuid());
  }

  @Test
  void selectByComponent() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setIsMain(true);
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setIsMain(false);
    featureBranch.setBranchType(BranchType.BRANCH);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    underTest.insert(dbSession, featureBranch);

    ComponentDto component = new ComponentDto().setBranchUuid(mainBranch.getUuid());

    // select the component
    Collection<BranchDto> branches = underTest.selectByComponent(dbSession, component);

    assertThat(branches).hasSize(2);

    assertThat(branches).extracting(BranchDto::getUuid, BranchDto::getKey, BranchDto::isMain, BranchDto::getProjectUuid,
        BranchDto::getBranchType, BranchDto::getMergeBranchUuid)
      .containsOnly(tuple(mainBranch.getUuid(), mainBranch.getKey(), mainBranch.isMain(), mainBranch.getProjectUuid(),
          mainBranch.getBranchType(), mainBranch.getMergeBranchUuid()),
        tuple(featureBranch.getUuid(), featureBranch.getKey(), featureBranch.isMain(), featureBranch.getProjectUuid(),
          featureBranch.getBranchType(),
          featureBranch.getMergeBranchUuid()));
  }

  @Test
  void selectByPullRequestKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.BRANCH);
    mainBranch.setKey("master");
    mainBranch.setIsMain(true);
    underTest.insert(dbSession, mainBranch);

    String pullRequestId = "123";
    BranchDto pullRequest = new BranchDto();
    pullRequest.setIsMain(false);
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
  void selectByKeys() {
    BranchDto mainBranch = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U1")
      .setIsMain(true)
      .setBranchType(BranchType.BRANCH)
      .setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U2")
      .setIsMain(false)
      .setBranchType(BranchType.BRANCH)
      .setKey("feature1");
    underTest.insert(dbSession, featureBranch);

    String pullRequestId = "123";
    BranchDto pullRequest = new BranchDto()
      .setProjectUuid("U1")
      .setUuid("U3")
      .setIsMain(false)
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
  void selectByUuids() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
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
  void selectByProjectUuid() {
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();

    ComponentDto branch1 = db.components().insertProjectBranch(mainBranch1);
    ComponentDto branch2 = db.components().insertProjectBranch(mainBranch1);
    ComponentDto branch3 = db.components().insertProjectBranch(mainBranch2);
    ComponentDto branch4 = db.components().insertProjectBranch(mainBranch2);

    assertThat(underTest.selectByProject(dbSession, new ProjectDto().setUuid(projectData1.projectUuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(mainBranch1.uuid(), branch1.uuid(), branch2.uuid());
    assertThat(underTest.selectByProject(dbSession, new ProjectDto().setUuid(projectData2.projectUuid())))
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(mainBranch2.uuid(), branch3.uuid(), branch4.uuid());
  }

  @Test
  void selectByUuid() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    ComponentDto branch2 = db.components().insertProjectBranch(project);

    assertThat(underTest.selectByUuid(db.getSession(), branch1.uuid()).get())
      .extracting(BranchDto::getUuid)
      .isEqualTo(branch1.uuid());
    assertThat(underTest.selectByUuid(db.getSession(), project.uuid())).isPresent();
    assertThat(underTest.selectByUuid(db.getSession(), "unknown")).isNotPresent();
  }

  @Test
  void countPrAndBranchByProjectUuid() {
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BRANCH).setKey("p1-branch-1"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(BRANCH).setKey("p1-branch-2"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-1"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-2"));
    db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey("p1-pr-3"));

    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertProjectBranch(project2, b -> b.setBranchType(PULL_REQUEST).setKey("p2-pr-1"));

    ProjectData projectData3 = db.components().insertPrivateProject();
    ComponentDto project3 = projectData3.getMainBranchComponent();
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
        tuple(projectData1.projectUuid(), 3L, 3L),
        tuple(projectData2.projectUuid(), 1L, 1L),
        tuple(projectData3.projectUuid(), 2L, 0L));
  }

  @Test
  void selectProjectUuidsWithIssuesNeedSync() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project4 = db.components().insertPrivateProject().getMainBranchComponent();
    ProjectDto project1Dto = db.components().getProjectDtoByMainBranch(project1);
    ProjectDto project2Dto = db.components().getProjectDtoByMainBranch(project2);
    ProjectDto project3Dto = db.components().getProjectDtoByMainBranch(project3);
    ProjectDto project4Dto = db.components().getProjectDtoByMainBranch(project4);

    BranchDto branch1 = db.components().insertProjectBranch(project1Dto, branchDto -> branchDto.setNeedIssueSync(true));
    BranchDto branch2 = db.components().insertProjectBranch(project1Dto, branchDto -> branchDto.setNeedIssueSync(false));
    BranchDto branch3 = db.components().insertProjectBranch(project2Dto);

    assertThat(underTest.selectProjectUuidsWithIssuesNeedSync(db.getSession(),
      Sets.newHashSet(project1Dto.getUuid(), project2Dto.getUuid(), project3Dto.getUuid(), project4Dto.getUuid())))
      .containsOnly(project1Dto.getUuid());
  }

  @Test
  void hasAnyBranchWhereNeedIssueSync() {
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isFalse();
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, false)).isFalse();

    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setNeedIssueSync(false));

    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isFalse();
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, false)).isTrue();

    project = db.components().insertPrivateProject().getMainBranchComponent();
    branch = db.components().insertProjectBranch(project, b -> b.setNeedIssueSync(true));
    assertThat(underTest.hasAnyBranchWhereNeedIssueSync(dbSession, true)).isTrue();
  }

  @Test
  void countByTypeAndCreationDate() {
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, 0L)).isZero();

    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, 0L)).isEqualTo(3);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, NOW)).isEqualTo(3);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.BRANCH, NOW + 100)).isZero();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, 0L)).isOne();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW)).isOne();
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW + 100)).isZero();
  }

  @Test
  void selectBranchNeedingIssueSync() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    String uuid = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));

    assertThat(underTest.selectBranchNeedingIssueSync(dbSession))
      .extracting(BranchDto::getUuid)
      .containsExactly(uuid);
  }

  @Test
  void selectBranchNeedingIssueSyncForProject() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String uuid = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));

    assertThat(underTest.selectBranchNeedingIssueSyncForProject(dbSession, projectData.projectUuid()))
      .extracting(BranchDto::getUuid)
      .containsExactly(uuid);
  }

  @Test
  void updateAllNeedIssueSync() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String uuid1 = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    String uuid2 = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false)).uuid();

    underTest.updateAllNeedIssueSync(dbSession);

    Optional<BranchDto> project1 = underTest.selectByUuid(dbSession, uuid1);
    assertThat(project1).isPresent();
    assertThat(project1.get().isNeedIssueSync()).isTrue();

    Optional<BranchDto> project2 = underTest.selectByUuid(dbSession, uuid2);
    assertThat(project2).isPresent();
    assertThat(project2.get().isNeedIssueSync()).isTrue();
  }

  @Test
  void updateAllNeedIssueSyncForProject() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String uuid1 = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true)).uuid();
    String uuid2 = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false)).uuid();

    underTest.updateAllNeedIssueSyncForProject(dbSession, projectData.projectUuid());

    Optional<BranchDto> project1 = underTest.selectByUuid(dbSession, uuid1);
    assertThat(project1).isPresent();
    assertThat(project1.get().isNeedIssueSync()).isTrue();

    Optional<BranchDto> project2 = underTest.selectByUuid(dbSession, uuid2);
    assertThat(project2).isPresent();
    assertThat(project2.get().isNeedIssueSync()).isTrue();
  }

  @Test
  void updateNeedIssueSync() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
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
  void updateIsMain() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto projectDto = projectData.getProjectDto();
    BranchDto mainBranch = projectData.getMainBranchDto();
    BranchDto nonMainBranch = db.components().insertProjectBranch(projectDto).setBranchType(BRANCH).setIsMain(false);

    underTest.updateIsMain(dbSession, mainBranch.getUuid(), false);
    underTest.updateIsMain(dbSession, nonMainBranch.getUuid(), true);

    Optional<BranchDto> oldMainBranch = underTest.selectByUuid(dbSession, mainBranch.getUuid());
    assertThat(oldMainBranch).isPresent().get().extracting(BranchDto::isMain).isEqualTo(false);
    Optional<BranchDto> newMainBranch = underTest.selectByUuid(dbSession, nonMainBranch.getUuid());
    assertThat(newMainBranch).isPresent().get().extracting(BranchDto::isMain).isEqualTo(true);

  }

  @Test
  void doAnyOfComponentsNeedIssueSync() {
    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, emptyList())).isFalse();

    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertProjectBranch(projectData1.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    BranchDto projectBranch1 = db.components().insertProjectBranch(projectData1.getProjectDto(),
      b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));
    BranchDto projectBranch2 = db.components().insertProjectBranch(projectData1.getProjectDto(),
      b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    db.components().insertProjectBranch(projectData1.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(false));
    BranchDto pullRequest1 = db.components().insertProjectBranch(projectData1.getProjectDto(),
      b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));
    BranchDto pullRequest2 = db.components().insertProjectBranch(projectData1.getProjectDto(),
      b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(false));
    db.components().insertProjectBranch(projectData1.getProjectDto(), b -> b.setBranchType(BranchType.PULL_REQUEST).setNeedIssueSync(true));

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, singletonList(projectData1.projectKey()))).isTrue();
    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, singletonList(projectData2.projectKey()))).isFalse();
  }

  @Test
  void doAnyOfComponentsNeedIssueSync_test_more_than_1000() {
    List<String> componentKeys = IntStream.range(0, 1100).mapToObj(value -> db.components().insertPrivateProject().getMainBranchComponent())
      .map(ComponentDto::getKey)
      .collect(Collectors.toCollection(ArrayList::new));

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, componentKeys)).isFalse();

    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ProjectDto projectDto = db.components().getProjectDtoByMainBranch(project);
    db.components().insertProjectBranch(projectDto, b -> b.setBranchType(BranchType.BRANCH).setNeedIssueSync(true));

    componentKeys.add(project.getKey());

    assertThat(underTest.doAnyOfComponentsNeedIssueSync(dbSession, componentKeys)).isTrue();
  }

  private static Object[][] booleanValues() {
    return new Object[][]{
      {true},
      {false}
    };
  }

  @ParameterizedTest
  @MethodSource("booleanValues")
  void isBranchNeedIssueSync_shouldReturnCorrectValue(boolean needIssueSync) {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    String branchUuid = db.components().insertProjectBranch(project,
      branch -> branch.setBranchType(BranchType.BRANCH).setNeedIssueSync(needIssueSync)).uuid();

    assertThat(underTest.isBranchNeedIssueSync(dbSession, branchUuid)).isEqualTo(needIssueSync);
  }

  @Test
  void isBranchNeedIssueSync_whenNoBranch_shouldReturnFalse() {
    assertThat(underTest.isBranchNeedIssueSync(dbSession, "unknown")).isFalse();
  }

  @Test
  void selectMainBranchByProjectUuid_whenMainBranch_shouldReturnMainBranch() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setIsMain(true);
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    underTest.insert(dbSession, dto);

    assertThat(underTest.selectMainBranchByProjectUuid(dbSession, "U1")).get()
      .extracting(BranchDto::getUuid).isEqualTo("U1");
  }

  @Test
  void selectMainBranchByProjectUuid_whenNonMainBranch_shouldReturnEmpty() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setIsMain(false);
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("feature");
    underTest.insert(dbSession, dto);

    assertThat(underTest.selectMainBranchByProjectUuid(dbSession, "U1")).isEmpty();
  }

  @Test
  void selectMainBranchesByProjectUuids_whenNoUuidsPassed_shouldReturnEmpty() {
    insertBranchesForProjectUuids(true, "1");

    List<BranchDto> branchDtos = underTest.selectMainBranchesByProjectUuids(dbSession, Set.of());

    assertThat(branchDtos).isEmpty();
  }

  @Test
  void selectMainBranchesByProjectUuids_whenOneUuidPassedAndTwoBranchesInDatabase_shouldReturnOneBranch() {
    insertBranchesForProjectUuids(true, "1", "2");

    List<BranchDto> branchDtos = underTest.selectMainBranchesByProjectUuids(dbSession, Set.of("1"));

    assertThat(branchDtos).hasSize(1);
    assertThat(branchDtos).extracting(BranchDto::getProjectUuid).allMatch(s -> s.equals("1"));
  }

  @Test
  void selectMainBranchesByProjectUuids_whenTenUuidsPassedAndTenBranchesInDatabase_shouldReturnAllBranches() {
    String[] projectUuids = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    insertBranchesForProjectUuids(true, projectUuids);
    insertBranchesForProjectUuids(false, projectUuids);

    List<BranchDto> branchDtos = underTest.selectMainBranchesByProjectUuids(dbSession, Set.of(projectUuids));

    assertThat(branchDtos).hasSize(10);
    assertThat(branchDtos).extracting(BranchDto::isMain).allMatch(b -> true);
  }

  private void insertBranchesForProjectUuids(boolean mainBranch, String... uuids) {
    for (String uuid : uuids) {
      BranchDto dto = new BranchDto();
      dto.setProjectUuid(uuid);
      dto.setUuid(uuid + "-uuid" + mainBranch);
      dto.setIsMain(mainBranch);
      dto.setBranchType(BranchType.BRANCH);
      dto.setKey("feature-" + uuid + mainBranch);
      underTest.insert(dbSession, dto);
    }
  }
}

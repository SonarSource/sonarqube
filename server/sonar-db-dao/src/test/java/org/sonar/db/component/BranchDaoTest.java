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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbProjectBranches;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
    dto.setBranchType(BranchType.SHORT);
    dto.setKey("feature/foo");

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat(map).contains(
      entry("projectUuid", "U1"),
      entry("uuid", "U2"),
      entry("branchType", "SHORT"),
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
    dto.setBranchType(BranchType.LONG);
    dto.setKey("feature");
    underTest.insert(dbSession, dto);

    BranchDto dto2 = new BranchDto();
    dto2.setProjectUuid("U2");
    dto2.setUuid("U2");
    dto2.setBranchType(BranchType.LONG);
    dto2.setKey("branch");
    underTest.insert(dbSession, dto2);

    underTest.updateMainBranchName(dbSession, "U1", "master");
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "master").get();
    assertThat(loaded.getMergeBranchUuid()).isNull();
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.LONG);
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void insert_null_or_empty_manual_baseline_analysis_uuid_is_null(@Nullable String nullOrEmpty) {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.LONG);
    dto.setKey("foo");
    dto.setManualBaseline(nullOrEmpty);

    underTest.insert(dbSession, dto);

    assertThat(underTest.selectByUuid(dbSession, dto.getUuid()).get().getManualBaseline()).isNull();
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""}
    };
  }

  @Test
  public void insert_manual_baseline_analysis_uuid() {
    String manualBaselineAnalysisUuid = randomAlphabetic(12);
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.LONG);
    dto.setKey("foo");
    dto.setManualBaseline(manualBaselineAnalysisUuid);

    underTest.insert(dbSession, dto);

    assertThat(underTest.selectByUuid(dbSession, dto.getUuid()).get().getManualBaseline()).isEqualTo(manualBaselineAnalysisUuid);
  }

  @Test
  @UseDataProvider("oldAndNewValuesCombinations")
  public void update_manualBaselineAnalysisUuid_if_new_value_is_different(@Nullable String oldValue, @Nullable String newValue) {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U1");
    dto.setBranchType(BranchType.LONG);
    dto.setKey("foo");
    dto.setManualBaseline(oldValue);
    underTest.insert(dbSession, dto);

    BranchDto otherDtoThatShouldNotChange = new BranchDto();
    otherDtoThatShouldNotChange.setProjectUuid("U2");
    otherDtoThatShouldNotChange.setUuid("U2");
    otherDtoThatShouldNotChange.setBranchType(BranchType.LONG);
    otherDtoThatShouldNotChange.setKey("branch");
    otherDtoThatShouldNotChange.setManualBaseline(oldValue);
    underTest.insert(dbSession, otherDtoThatShouldNotChange);

    assertThat(underTest.updateManualBaseline(dbSession, dto.getUuid(), newValue)).isEqualTo(1);

    assertThat(underTest.selectByUuid(dbSession, dto.getUuid()).get().getManualBaseline())
      .isEqualTo(emptyToNull(newValue));
    assertThat(underTest.selectByUuid(dbSession, otherDtoThatShouldNotChange.getUuid()).get().getManualBaseline())
      .isEqualTo(emptyToNull(oldValue));
  }

  @DataProvider
  public static Object[][] oldAndNewValuesCombinations() {
    String value1 = randomAlphabetic(10);
    String value2 = randomAlphabetic(20);
    return new Object[][] {
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
  @UseDataProvider("nonLongBranchType")
  public void do_not_update_manual_baseline_of_non_long_branches(BranchType branchType) {
    String analysisUuid = randomAlphabetic(12);
    String otherAnalysisUuid = randomAlphabetic(12);
    BranchDto noManualBaselineDto = ComponentTesting.newBranchDto(db.components().insertPrivateProject(), branchType);
    BranchDto withManualBaselineDto = ComponentTesting.newBranchDto(db.components().insertPrivateProject(), branchType).setManualBaseline(analysisUuid);
    underTest.insert(dbSession, noManualBaselineDto);
    underTest.insert(dbSession, withManualBaselineDto);
    dbSession.commit();

    assertThat(underTest.updateManualBaseline(dbSession, noManualBaselineDto.getUuid(), null)).isZero();
    assertThat(underTest.updateManualBaseline(dbSession, noManualBaselineDto.getUuid(), otherAnalysisUuid)).isZero();
    assertThat(underTest.updateManualBaseline(dbSession, withManualBaselineDto.getUuid(), null)).isZero();
    assertThat(underTest.updateManualBaseline(dbSession, withManualBaselineDto.getUuid(), otherAnalysisUuid)).isZero();
  }

  @DataProvider
  public static Object[][] nonLongBranchType() {
    return new Object[][] {
      {BranchType.SHORT},
      {BranchType.PULL_REQUEST}
    };
  }

  @Test
  public void insert_branch_with_all_fields_and_max_length_values() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid(repeat("a", 50));
    dto.setUuid(repeat("b", 50));
    dto.setBranchType(BranchType.SHORT);
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
    assertThat(loadedPullRequestData.getAttributesMap().get(tokenAttributeName)).isEqualTo(tokenAttributeValue);
  }

  @Test
  public void upsert_branch() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.LONG);
    dto.setKey("foo");
    underTest.insert(dbSession, dto);

    // the fields that can be updated
    dto.setMergeBranchUuid("U3");

    // the fields that can't be updated. New values are ignored.
    dto.setProjectUuid("ignored");
    dto.setBranchType(BranchType.SHORT);
    underTest.upsert(dbSession, dto);

    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "foo").get();
    assertThat(loaded.getMergeBranchUuid()).isEqualTo("U3");
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.LONG);
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
    dto.setBranchType(BranchType.SHORT);
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
    assertThat(loadedPullRequestData.getAttributesMap().get(tokenAttributeName)).isEqualTo(tokenAttributeValue);
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
    assertThat(loadedPullRequestData.getAttributesMap().get(tokenAttributeName)).isEqualTo(tokenAttributeValue);
  }

  @Test
  public void selectByBranchKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.LONG);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setBranchType(BranchType.SHORT);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    featureBranch.setManualBaseline("analysisUUID");
    underTest.insert(dbSession, featureBranch);

    // select the feature branch
    BranchDto loaded = underTest.selectByBranchKey(dbSession, "U1", "feature/foo").get();
    assertThat(loaded.getUuid()).isEqualTo(featureBranch.getUuid());
    assertThat(loaded.getKey()).isEqualTo(featureBranch.getKey());
    assertThat(loaded.getProjectUuid()).isEqualTo(featureBranch.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(featureBranch.getBranchType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(featureBranch.getMergeBranchUuid());
    assertThat(loaded.getManualBaseline()).isEqualTo(featureBranch.getManualBaseline());

    // select a branch on another project with same branch name
    assertThat(underTest.selectByBranchKey(dbSession, "U3", "feature/foo")).isEmpty();
  }

  @Test
  public void selectByPullRequestKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.LONG);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    String pullRequestId = "123";
    BranchDto pullRequest = new BranchDto();
    pullRequest.setProjectUuid("U1");
    pullRequest.setUuid("U2");
    pullRequest.setBranchType(BranchType.PULL_REQUEST);
    pullRequest.setKey(pullRequestId);
    pullRequest.setMergeBranchUuid("U3");
    pullRequest.setManualBaseline("analysisUUID");
    underTest.insert(dbSession, pullRequest);

    // select the feature branch
    BranchDto loaded = underTest.selectByPullRequestKey(dbSession, "U1", pullRequestId).get();
    assertThat(loaded.getUuid()).isEqualTo(pullRequest.getUuid());
    assertThat(loaded.getKey()).isEqualTo(pullRequest.getKey());
    assertThat(loaded.getProjectUuid()).isEqualTo(pullRequest.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(pullRequest.getBranchType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(pullRequest.getMergeBranchUuid());
    assertThat(loaded.getManualBaseline()).isEqualTo(pullRequest.getManualBaseline());

    // select a branch on another project with same branch name
    assertThat(underTest.selectByPullRequestKey(dbSession, "U3", pullRequestId)).isEmpty();
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
  public void selectByUuid() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    ComponentDto branch2 = db.components().insertProjectBranch(project);

    assertThat(underTest.selectByUuid(db.getSession(), branch1.uuid()).get())
      .extracting(BranchDto::getUuid)
      .containsExactlyInAnyOrder(branch1.uuid());
    assertThat(underTest.selectByUuid(db.getSession(), project.uuid())).isNotPresent();
    assertThat(underTest.selectByUuid(db.getSession(), "unknown")).isNotPresent();
  }

  @Test
  public void existsNonMainBranch() {
    assertThat(underTest.hasNonMainBranches(dbSession)).isFalse();
    ComponentDto project = db.components().insertPrivateProject();
    assertThat(underTest.hasNonMainBranches(dbSession)).isFalse();

    ComponentDto branch1 = db.components().insertProjectBranch(project);
    assertThat(underTest.hasNonMainBranches(dbSession)).isTrue();

    ComponentDto branch2 = db.components().insertProjectBranch(project);
    assertThat(underTest.hasNonMainBranches(dbSession)).isTrue();
  }

  @Test
  public void countByTypeAndCreationDate() {
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.LONG, 0L)).isEqualTo(0);

    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto longBranch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto longBranch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.LONG));
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.LONG, 0L)).isEqualTo(2);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.LONG, NOW)).isEqualTo(2);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.LONG, NOW + 100)).isEqualTo(0);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, 0L)).isEqualTo(1);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW)).isEqualTo(1);
    assertThat(underTest.countByTypeAndCreationDate(dbSession, BranchType.PULL_REQUEST, NOW + 100)).isEqualTo(0);
  }

  private static String emptyToNull(@Nullable String newValue) {
    return newValue == null || newValue.isEmpty() ? null : newValue;
  }
}

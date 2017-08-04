/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class BranchDaoTest {

  private static final long NOW = 1_000L;
  private static final String SELECT_FROM = "select project_uuid as \"projectUuid\", uuid as \"uuid\", branch_type as \"branchType\", kee_type as \"keeType\", " +
    "kee as \"kee\", merge_branch_uuid as \"mergeBranchUuid\", pull_request_title as \"pullRequestTitle\", created_at as \"createdAt\", updated_at as \"updatedAt\" " +
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
    dto.setKeeType(BranchKeyType.BRANCH);
    dto.setKey("feature/foo");

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat(map).contains(
      entry("projectUuid", "U1"),
      entry("uuid", "U2"),
      entry("branchType", "SHORT"),
      entry("keeType", "BRANCH"),
      entry("kee", "feature/foo"),
      entry("mergeBranchUuid", null),
      entry("pullRequestTitle", null),
      entry("createdAt", 1_000L),
      entry("updatedAt", 1_000L)
    );
  }

  @Test
  public void insert_branch_with_all_fields_and_max_length_values() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid(repeat("a", 50));
    dto.setUuid(repeat("b", 50));
    dto.setBranchType(BranchType.SHORT);
    dto.setKeeType(BranchKeyType.BRANCH);
    dto.setKey(repeat("c", 255));
    dto.setMergeBranchUuid(repeat("d", 50));
    dto.setPullRequestTitle(repeat("e", 4_000));

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat((String)map.get("projectUuid")).contains("a").isEqualTo(dto.getProjectUuid());
    assertThat((String)map.get("uuid")).contains("b").isEqualTo(dto.getUuid());
    assertThat((String)map.get("kee")).contains("c").isEqualTo(dto.getKey());
    assertThat((String)map.get("mergeBranchUuid")).contains("d").isEqualTo(dto.getMergeBranchUuid());
    assertThat((String)map.get("pullRequestTitle")).contains("e").isEqualTo(dto.getPullRequestTitle());
  }

  @Test
  public void null_value_of_kee_column_is_converted_in_db() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("u1");
    dto.setUuid("u2");
    dto.setKeeType(BranchKeyType.BRANCH);
    dto.setKey(null);

    underTest.insert(dbSession, dto);

    Map<String, Object> map = db.selectFirst(dbSession, SELECT_FROM + " where uuid='" + dto.getUuid() + "'");
    assertThat(map).contains(entry("kee", BranchDto.NULL_KEY));
    BranchDto loaded = underTest.selectByKey(dbSession, dto.getProjectUuid(), dto.getKeeType(), null).get();
    assertThat(loaded.getKey()).isNull();
  }

  @Test
  public void upsert() {
    BranchDto dto = new BranchDto();
    dto.setProjectUuid("U1");
    dto.setUuid("U2");
    dto.setBranchType(BranchType.LONG);
    dto.setKeeType(BranchKeyType.BRANCH);
    dto.setKey("foo");
    underTest.insert(dbSession, dto);

    // the fields that can be updated
    dto.setMergeBranchUuid("U3");
    dto.setPullRequestTitle("theTitle");

    // the fields that can't be updated. New values are ignored.
    dto.setProjectUuid("ignored");
    dto.setBranchType(BranchType.SHORT);
    dto.setKeeType(BranchKeyType.PR);
    underTest.upsert(dbSession, dto);

    BranchDto loaded = underTest.selectByKey(dbSession, "U1", BranchKeyType.BRANCH, "foo").get();
    assertThat(loaded.getMergeBranchUuid()).isEqualTo("U3");
    assertThat(loaded.getPullRequestTitle()).isEqualTo("theTitle");
    assertThat(loaded.getProjectUuid()).isEqualTo("U1");
    assertThat(loaded.getBranchType()).isEqualTo(BranchType.LONG);
    assertThat(loaded.getKeeType()).isEqualTo(BranchKeyType.BRANCH);
  }

  @Test
  public void selectByKey() {
    BranchDto mainBranch = new BranchDto();
    mainBranch.setProjectUuid("U1");
    mainBranch.setUuid("U1");
    mainBranch.setBranchType(BranchType.LONG);
    mainBranch.setKeeType(BranchKeyType.BRANCH);
    mainBranch.setKey("master");
    underTest.insert(dbSession, mainBranch);

    BranchDto featureBranch = new BranchDto();
    featureBranch.setProjectUuid("U1");
    featureBranch.setUuid("U2");
    featureBranch.setBranchType(BranchType.SHORT);
    featureBranch.setKeeType(BranchKeyType.BRANCH);
    featureBranch.setKey("feature/foo");
    featureBranch.setMergeBranchUuid("U3");
    underTest.insert(dbSession, featureBranch);

    // select the feature branch
    BranchDto loaded = underTest.selectByKey(dbSession, "U1", BranchKeyType.BRANCH, "feature/foo").get();
    assertThat(loaded.getUuid()).isEqualTo(featureBranch.getUuid());
    assertThat(loaded.getKey()).isEqualTo(featureBranch.getKey());
    assertThat(loaded.getProjectUuid()).isEqualTo(featureBranch.getProjectUuid());
    assertThat(loaded.getBranchType()).isEqualTo(featureBranch.getBranchType());
    assertThat(loaded.getKeeType()).isEqualTo(featureBranch.getKeeType());
    assertThat(loaded.getMergeBranchUuid()).isEqualTo(featureBranch.getMergeBranchUuid());
    assertThat(loaded.getPullRequestTitle()).isEqualTo(featureBranch.getPullRequestTitle());

    // select a pull request with same key than the feature branch
    assertThat(underTest.selectByKey(dbSession, "U1", BranchKeyType.PR, "feature/foo")).isEmpty();

    // select a branch on another project with same branch name
    assertThat(underTest.selectByKey(dbSession, "U3", BranchKeyType.BRANCH, "feature/foo")).isEmpty();
  }

}

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
package org.sonar.db.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleRepositoryDaoTest {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private RuleRepositoryDao underTest = new RuleRepositoryDao(system2);

  @Test
  public void insertOrUpdate_insert_rows_that_do_not_exist() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.insertOrUpdate(dbTester.getSession(), asList(repo1, repo2, repo3));

    List<RuleRepositoryDto> rows = underTest.selectAll(dbTester.getSession());
    assertThat(rows).hasSize(3);
    // ordered by keys
    assertThat(rows.get(0)).isEqualToComparingFieldByField(repo1);
    assertThat(rows.get(1)).isEqualToComparingFieldByField(repo3);
    assertThat(rows.get(2)).isEqualToComparingFieldByField(repo2);

    assertThat(selectCreatedAtByKey(dbTester.getSession(), repo1.getKey()))
      .isEqualTo(selectCreatedAtByKey(dbTester.getSession(), repo2.getKey()))
      .isEqualTo(selectCreatedAtByKey(dbTester.getSession(), repo3.getKey()));
  }

  @Test
  public void insertOrUpdate_update_rows_that_exist() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    underTest.insertOrUpdate(dbTester.getSession(), asList(repo1, repo2));

    // update sonarjava, insert sonarcobol
    RuleRepositoryDto repo2bis = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.insertOrUpdate(dbTester.getSession(), asList(repo2bis, repo3));

    List<RuleRepositoryDto> rows = underTest.selectAll(dbTester.getSession());
    assertThat(rows).hasSize(3);
    // ordered by keys
    assertThat(rows.get(0)).isEqualToComparingFieldByField(repo1);
    assertThat(rows.get(1)).isEqualToComparingFieldByField(repo3);
    assertThat(rows.get(2)).isEqualToComparingFieldByField(repo2bis);

    assertThat(selectCreatedAtByKey(dbTester.getSession(), repo1.getKey()))
      .isEqualTo(selectCreatedAtByKey(dbTester.getSession(), repo2.getKey()))
      .isLessThan(selectCreatedAtByKey(dbTester.getSession(), repo3.getKey()));
  }

  @Test
  public void deleteIfKeyNotIn() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.insertOrUpdate(dbTester.getSession(), asList(repo1, repo2, repo3));

    underTest.deleteIfKeyNotIn(dbTester.getSession(), Arrays.asList(repo2.getKey(), "unknown"));
    assertThat(underTest.selectAll(dbTester.getSession()))
      .extracting(RuleRepositoryDto::getKey)
      .containsExactly(repo2.getKey());
  }

  @Test
  public void deleteIfKeyNotIn_truncates_table_if_keys_are_empty() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    underTest.insertOrUpdate(dbTester.getSession(), asList(repo1, repo2));

    underTest.deleteIfKeyNotIn(dbTester.getSession(), emptyList());

    assertThat(underTest.selectAll(dbTester.getSession())).isEmpty();
  }

  @Test
  public void deleteIfKeyNotIn_fails_if_more_than_1000_keys() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("too many rule repositories: 1100");

    Collection<String> keys = IntStream.range(0, 1_100).mapToObj(index -> "repo" + index).collect(Collectors.toSet());
    underTest.deleteIfKeyNotIn(dbTester.getSession(), keys);
  }

  @Test
  public void selectByLanguage() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("squid", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    underTest.insertOrUpdate(dbSession, asList(dto1, dto2, dto3));

    assertThat(underTest.selectByLanguage(dbSession, "java")).extracting(RuleRepositoryDto::getKey)
      // ordered by key
      .containsExactly("findbugs", "squid");
  }

  @Test
  public void selectByLanguage_returns_empty_list_if_no_results() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    underTest.insertOrUpdate(dbSession, asList(dto1));

    assertThat(underTest.selectByLanguage(dbSession, "missing")).hasSize(0);
  }

  private long selectCreatedAtByKey(DbSession dbSession, String key) {
    return (long) dbTester.selectFirst(dbSession, "select created_at as \"created_at\" from rule_repositories where kee='" + key + "'")
      .get("created_at");
  }
}

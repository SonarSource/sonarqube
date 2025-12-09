/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleRepositoryDaoIT {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(system2);

  private final RuleRepositoryDao underTest = new RuleRepositoryDao(system2);

  @Test
  void insert_insert_rows_that_do_not_exist() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.insert(dbTester.getSession(), asList(repo1, repo2, repo3));

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
  void update_update_rows_that_exist() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    underTest.insert(dbTester.getSession(), asList(repo1, repo2));

    // update sonarjava, insert sonarcobol
    RuleRepositoryDto repo2bis = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.update(dbTester.getSession(), asList(repo2bis));
    underTest.insert(dbTester.getSession(), asList(repo3));

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
  void deleteIfKeyNotIn() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("sonarcobol", "cobol", "SonarCobol");
    underTest.insert(dbTester.getSession(), asList(repo1, repo2, repo3));

    underTest.deleteIfKeyNotIn(dbTester.getSession(), Arrays.asList(repo2.getKey(), "unknown"));
    assertThat(underTest.selectAll(dbTester.getSession()))
      .extracting(RuleRepositoryDto::getKey)
      .containsExactly(repo2.getKey());
  }

  @Test
  void deleteIfKeyNotIn_truncates_table_if_keys_are_empty() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("sonarjava", "java", "SonarJava");
    underTest.insert(dbTester.getSession(), asList(repo1, repo2));

    underTest.deleteIfKeyNotIn(dbTester.getSession(), emptyList());

    assertThat(underTest.selectAll(dbTester.getSession())).isEmpty();
  }

  @Test
  void deleteIfKeyNotIn_fails_if_more_than_1000_keys() {
    assertThatThrownBy(() -> {
      Collection<String> keys = IntStream.range(0, 1_100).mapToObj(index -> "repo" + index).collect(Collectors.toSet());
      underTest.deleteIfKeyNotIn(dbTester.getSession(), keys);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("too many rule repositories: 1100");
  }

  @Test
  void selectByQueryAndLanguage_shouldMatchOnlyOnKeeOrName() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("a_findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("jdk", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    List<RuleRepositoryDto> ruleRepositoryDtos = underTest.selectByQueryAndLanguage(dbSession, "%a%", null);

    assertThat(ruleRepositoryDtos).extracting(RuleRepositoryDto::getName)
      .containsExactlyInAnyOrder("Java", "Findbugs");
  }

  @Test
  void selectByQueryAndLanguage_whenSeveralRepoMatchingForDifferentLanguages_matchOnlyTheRepoOfTheChosenLanguage() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugsa");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("java", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-bug", "cobol", "Cobol Lint");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    List<RuleRepositoryDto> ruleRepositoryDtos = underTest.selectByQueryAndLanguage(dbSession, "%bug%", "java");

    assertThat(ruleRepositoryDtos).extracting(RuleRepositoryDto::getKey)
      .containsExactly("findbugs");
  }


  @Test
  void selectAllKeys() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("java", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    assertThat(underTest.selectAllKeys(dbSession)).containsOnly("findbugs", "java", "cobol-lint");
  }

  @Test
  void selectByQueryAndLanguage_returnsEmptyList_when_thereIsNoResults() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    underTest.insert(dbSession, List.of(dto1));

    assertThat(underTest.selectByQueryAndLanguage(dbSession, "missing", null)).isEmpty();
  }

  @Test
  void selectByQueryAndLanguage_shouldBeCaseInsensitive() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("FINDBUGS", "java", "repoFB");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("openjdk", "java", "JaVa");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    assertThat(underTest.selectByQueryAndLanguage(dbSession, "bug", null))
      .extracting(RuleRepositoryDto::getKey).contains("FINDBUGS");
    assertThat(underTest.selectByQueryAndLanguage(dbSession, "COBOL", null))
      .extracting(RuleRepositoryDto::getKey).contains("cobol-lint");
    assertThat(underTest.selectByQueryAndLanguage(dbSession, "jAvA", null))
      .extracting(RuleRepositoryDto::getKey).contains("openjdk");

  }


  private long selectCreatedAtByKey(DbSession dbSession, String key) {
    return (long) dbTester.selectFirst(dbSession, "select created_at as \"created_at\" from rule_repositories where kee='" + key + "'")
      .get("created_at");
  }
}

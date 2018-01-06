/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleRepositoryDaoTest {

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private RuleRepositoryDao underTest = new RuleRepositoryDao(system2);

  @Test
  public void test_insert_and_selectAll() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    underTest.insert(dbSession, asList(dto));

    List<RuleRepositoryDto> rows = underTest.selectAll(dbSession);
    assertThat(rows).hasSize(1);
    RuleRepositoryDto row = rows.get(0);
    assertThat(row.getKey()).isEqualTo("findbugs");
    assertThat(row.getName()).isEqualTo("Findbugs");
    assertThat(row.getLanguage()).isEqualTo("java");
  }

  @Test
  public void insert_multiple_rows() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("squid", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    assertThat(underTest.selectAll(dbSession)).extracting(RuleRepositoryDto::getKey)
      // ordered by key
      .containsExactly("cobol-lint", "findbugs", "squid");
  }

  @Test
  public void selectByLanguage() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("squid", "java", "Java");
    RuleRepositoryDto dto3 = new RuleRepositoryDto("cobol-lint", "cobol", "Cobol Lint");
    underTest.insert(dbSession, asList(dto1, dto2, dto3));

    assertThat(underTest.selectByLanguage(dbSession, "java")).extracting(RuleRepositoryDto::getKey)
      // ordered by key
      .containsExactly("findbugs", "squid");
  }

  @Test
  public void selectByLanguage_returns_empty_list_if_no_results() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    underTest.insert(dbSession, asList(dto1));

    assertThat(underTest.selectByLanguage(dbSession, "missing")).hasSize(0);
  }

  @Test
  public void selectByKey() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    underTest.insert(dbSession, asList(dto1));

    assertThat(underTest.selectByKey(dbSession, "findbugs").get().getKey()).isEqualTo("findbugs");
    assertThat(underTest.selectByKey(dbSession, "missing")).isNotPresent();
  }

  @Test
  public void truncate() {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto dto1 = new RuleRepositoryDto("findbugs", "java", "Findbugs");
    RuleRepositoryDto dto2 = new RuleRepositoryDto("squid", "java", "Java");
    underTest.insert(dbSession, asList(dto1, dto2));

    underTest.truncate(dbSession);

    assertThat(underTest.selectAll(dbSession)).isEmpty();
  }

}

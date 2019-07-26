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
package org.sonar.db.issue;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


public class IssueChangeMapperTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private IssueChangeMapper underTest = dbTester.getSession().getMapper(IssueChangeMapper.class);

  @Test
  public void insert_diff() {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setKey(null /* no key on field changes */);
    dto.setUserUuid("user_uuid");
    dto.setIssueKey("ABCDE");
    dto.setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE);
    dto.setChangeData("severity=INFO|BLOCKER");
    dto.setCreatedAt(1_500_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);
    dto.setIssueChangeCreationDate(1_500_000_000_000L);
    underTest.insert(dto);
    dbTester.getSession().commit();

    List<IssueChangeDto> dtos = underTest.selectByIssues(singletonList("ABCDE"));
    assertThat(dtos).hasSize(1);
    IssueChangeDto issueChangeDto = dtos.get(0);
    assertEquals(dto, issueChangeDto);
  }

  @Test
  public void insert_comment() {
    IssueChangeDto dto = new IssueChangeDto();
    dto.setKey("COMMENT-1234");
    dto.setUserUuid("user_uuid");
    dto.setIssueKey("ABCDE");
    dto.setChangeType(IssueChangeDto.TYPE_COMMENT);
    dto.setChangeData("the comment");
    dto.setCreatedAt(1_500_000_000_000L);
    dto.setUpdatedAt(1_500_000_000_000L);
    dto.setIssueChangeCreationDate(1_500_000_000_000L);

    underTest.insert(dto);
    dbTester.getSession().commit();

    List<IssueChangeDto> dtos = underTest.selectByIssues(singletonList("ABCDE"));
    assertThat(dtos).hasSize(1);
    IssueChangeDto issueChangeDto = dtos.get(0);
    assertEquals(dto, issueChangeDto);
  }

  private void assertEquals(IssueChangeDto expected, IssueChangeDto actual) {
    assertThat(actual.getKey()).isEqualTo(expected.getKey());
    assertThat(actual.getUserUuid()).isEqualTo(expected.getUserUuid());
    assertThat(actual.getIssueKey()).isEqualTo(expected.getIssueKey());
    assertThat(actual.getChangeType()).isEqualTo(expected.getChangeType());
    assertThat(actual.getChangeData()).isEqualTo(expected.getChangeData());
    assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(actual.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
    assertThat(actual.getIssueChangeCreationDate()).isEqualTo(expected.getIssueChangeCreationDate());
  }
}

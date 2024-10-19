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
package org.sonar.db.issue;

import org.junit.jupiter.api.Test;
import org.sonar.core.util.UuidFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewCodeReferenceIssueDtoTest {

  private static final IssueDto ISSUE_DTO = mock(IssueDto.class);
  private static final String KEY = "issue-key";
  private static final String UUID = "uuid";
  private static final UuidFactory UUID_FACTORY = mock(UuidFactory.class);

  @Test
  void create_from_issue_dto() {
    when(ISSUE_DTO.getKey()).thenReturn(KEY);
    when(UUID_FACTORY.create()).thenReturn(UUID);
    long now = System.currentTimeMillis();

    NewCodeReferenceIssueDto dto = NewCodeReferenceIssueDto.fromIssueDto(ISSUE_DTO, now, UUID_FACTORY);

    assertThat(dto.getUuid()).isEqualTo(UUID);
    assertThat(dto.getIssueKey()).isEqualTo(KEY);
    assertThat(dto.getCreatedAt()).isNotNull();
  }

  @Test
  void create_from_issue_key() {
    when(UUID_FACTORY.create()).thenReturn(UUID);
    long now = System.currentTimeMillis();

    NewCodeReferenceIssueDto dto = NewCodeReferenceIssueDto.fromIssueKey(KEY, now, UUID_FACTORY);

    assertThat(dto.getUuid()).isEqualTo(UUID);
    assertThat(dto.getIssueKey()).isEqualTo(KEY);
    assertThat(dto.getCreatedAt()).isNotNull();
  }
}

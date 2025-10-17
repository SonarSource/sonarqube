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
package org.sonar.db.jira;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.jira.dao.AtlassianAuthenticationDetailsDao;
import org.sonar.db.jira.dto.AtlassianAuthenticationDetailsDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtlassianAuthenticationDetailsDaoTest {

  private final System2 system2 = mock(System2.class);

  private final AtlassianAuthenticationDetailsDto dto = new AtlassianAuthenticationDetailsDto()
    .setClientId("foo")
    .setSecret("bar");

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final AtlassianAuthenticationDetailsDao underTest = new AtlassianAuthenticationDetailsDao(system2);

  @Test
  void insertOrUpdate_shouldInsertNewBinding() {
    var now = 1000L;
    when(system2.now()).thenReturn(now);

    var result = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(result.getClientId()).isEqualTo("foo");
    assertThat(result.getSecret()).isEqualTo("bar");
    assertThat(result.getCreatedAt()).isEqualTo(now);
    assertThat(result.getUpdatedAt()).isEqualTo(now);

    var found = underTest.selectFirst(db.getSession());
    assertThat(found).isPresent();
    assertThat(found.get().getClientId()).isEqualTo("foo");
    assertThat(found.get().getSecret()).isEqualTo("bar");
    assertThat(found.get().getCreatedAt()).isEqualTo(now);
    assertThat(found.get().getUpdatedAt()).isEqualTo(now);
  }

  @Test
  void insertOrUpdate_shouldUpdateExistingBinding() {
    when(system2.now()).thenReturn(1000L, 2000L);
    underTest.insertOrUpdate(db.getSession(), dto);
    // Update the dto
    dto.setSecret("very secret don't look please");

    var updated = underTest.insertOrUpdate(db.getSession(), dto);

    assertThat(updated.getClientId()).isEqualTo("foo");
    assertThat(updated.getSecret()).isEqualTo("very secret don't look please");
    assertThat(updated.getCreatedAt()).isEqualTo(1000L);
    assertThat(updated.getUpdatedAt()).isEqualTo(2000L);

    var found = underTest.selectFirst(db.getSession());
    assertThat(found).isPresent();
    assertThat(found.get().getSecret()).isEqualTo("very secret don't look please");
  }

  @Test
  void selectById_shouldReturnEmpty_whenNotFound() {
    assertThat(underTest.selectFirst(db.getSession())).isEmpty();
  }

  @Test
  void selectById_shouldReturnBinding_whenExists() {
    when(system2.now()).thenReturn(1000L);
    underTest.insertOrUpdate(db.getSession(), dto);

    var result = underTest.selectFirst(db.getSession());

    assertThat(result).isPresent();
    assertThat(result.get().getClientId()).isEqualTo(dto.getClientId());
  }
}

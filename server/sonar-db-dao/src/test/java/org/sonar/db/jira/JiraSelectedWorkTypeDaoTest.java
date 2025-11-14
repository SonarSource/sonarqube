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
package org.sonar.db.jira;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.jira.dao.JiraSelectedWorkTypeDao;
import org.sonar.db.jira.dto.JiraSelectedWorkTypeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraSelectedWorkTypeDaoTest {

  private final System2 system2 = mock(System2.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final JiraSelectedWorkTypeDao underTest = new JiraSelectedWorkTypeDao(system2, uuidFactory);

  @Test
  void findByJiraProjectBindingId_shouldReturnEmpty_whenNoWorkTypes() {
    var result = underTest.findByJiraProjectBindingId(db.getSession(), "non-existent-binding");

    assertThat(result).isEmpty();
  }

  @Test
  void findByJiraProjectBindingId_shouldReturnOnlyMatchingBinding() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-1", "uuid-2", "uuid-3");

    var dto1 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-1");
    var dto2 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-2");
    var dto3 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-2")
      .setWorkTypeId("work-type-3");

    underTest.saveAll(db.getSession(), Arrays.asList(dto1, dto2, dto3));

    var result = underTest.findByJiraProjectBindingId(db.getSession(), "binding-1");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(JiraSelectedWorkTypeDto::getId).containsExactlyInAnyOrder("uuid-1", "uuid-2");
    assertThat(result).allMatch(dto -> dto.getJiraProjectBindingId().equals("binding-1"));
    assertThat(result).extracting(JiraSelectedWorkTypeDto::getWorkTypeId).containsExactlyInAnyOrder("work-type-1", "work-type-2");
    assertThat(result).allMatch(dto -> dto.getCreatedAt() == 1000L);
    assertThat(result).allMatch(dto -> dto.getUpdatedAt() == 1000L);
  }

  @Test
  void deleteByJiraProjectBindingId_shouldReturnZero_whenNoWorkTypes() {
    var deletedCount = underTest.deleteByJiraProjectBindingId(
      db.getSession(),
      "non-existent-binding"
    );

    assertThat(deletedCount).isZero();
  }

  @Test
  void deleteByJiraProjectBindingId_shouldDeleteAllWorkTypes_forBinding() {
    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-10", "uuid-20", "uuid-30");

    var dto1 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-1");
    var dto2 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-2");
    var dto3 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-2")
      .setWorkTypeId("work-type-3");

    underTest.saveAll(db.getSession(), Arrays.asList(dto1, dto2, dto3));

    int deletedCount = underTest.deleteByJiraProjectBindingId(db.getSession(), "binding-1");

    assertThat(deletedCount).isEqualTo(2);
    assertThat(underTest.findByJiraProjectBindingId(db.getSession(), "binding-1")).isEmpty();
    assertThat(underTest.findByJiraProjectBindingId(db.getSession(), "binding-2")).hasSize(1);
  }

  @Test
  void saveAll_shouldSaveAllWorkTypes() {
    assertThat(underTest.findByJiraProjectBindingId(db.getSession(), "binding-1")).isEmpty();

    when(system2.now()).thenReturn(1000L);
    when(uuidFactory.create()).thenReturn("uuid-100", "uuid-200");

    var dto1 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-1");
    var dto2 = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-2");

    underTest.saveAll(db.getSession(), Arrays.asList(dto1, dto2));

    var result = underTest.findByJiraProjectBindingId(db.getSession(), "binding-1");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(JiraSelectedWorkTypeDto::getId).containsExactlyInAnyOrder("uuid-100", "uuid-200");
    assertThat(result).allMatch(dto -> dto.getJiraProjectBindingId().equals("binding-1"));
    assertThat(result).extracting(JiraSelectedWorkTypeDto::getWorkTypeId).containsExactlyInAnyOrder("work-type-1", "work-type-2");
    assertThat(result).allMatch(dto -> dto.getCreatedAt() == 1000L);
    assertThat(result).allMatch(dto -> dto.getUpdatedAt() == 1000L);
  }

  @Test
  void saveAll_shouldSetAllColumnValues() {
    when(system2.now()).thenReturn(2000L);
    when(uuidFactory.create()).thenReturn("custom-uuid-1");

    var dto = new JiraSelectedWorkTypeDto()
      .setJiraProjectBindingId("binding-1")
      .setWorkTypeId("work-type-1");

    underTest.saveAll(db.getSession(), List.of(dto));

    var result = underTest.findByJiraProjectBindingId(db.getSession(), "binding-1");

    assertThat(result).hasSize(1);
    var saved = result.get(0);
    assertThat(saved.getId()).isEqualTo("custom-uuid-1");
    assertThat(saved.getCreatedAt()).isEqualTo(2000L);
    assertThat(saved.getUpdatedAt()).isEqualTo(2000L);
    assertThat(saved.getJiraProjectBindingId()).isEqualTo("binding-1");
    assertThat(saved.getWorkTypeId()).isEqualTo("work-type-1");
  }

  @Test
  void saveAll_shouldHandleEmptyList() {
    assertThatNoException()
      .isThrownBy(() -> underTest.saveAll(db.getSession(), List.of()));
  }
}

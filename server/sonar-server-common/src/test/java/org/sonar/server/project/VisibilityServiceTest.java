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
package org.sonar.server.project;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.entity.EntityDto;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.GITHUB_PROJECT_PERMISSIONS_PROVISIONING;

@RunWith(MockitoJUnitRunner.class)
public class VisibilityServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;

  @Mock
  private DbSession dbSession;

  @InjectMocks
  private VisibilityService visibilityService;

  @Test
  public void checkNoPendingTasks_whenEntityNotFound_throwsIae() {
    EntityDto entityDto = mockEntityDto();
    when(dbClient.entityDao().selectByKey(dbSession, entityDto.getKey())).thenReturn(Optional.empty());

    assertThatIllegalStateException()
      .isThrownBy(() -> visibilityService.checkNoPendingTasks(dbSession, entityDto))
      .withMessage("Can't find entity entityKey");
  }

  @Test
  public void checkNoPendingTasks_whenEntityFoundAndNoTaskInQueue_doesNotThrow() {
    EntityDto entityDto = mockEntityDto();
    when(dbClient.entityDao().selectByKey(dbSession, entityDto.getKey())).thenReturn(Optional.of(entityDto));

    visibilityService.checkNoPendingTasks(dbSession, entityDto);
  }

  @Test
  public void checkNoPendingTasks_whenOneGithubSyncTaskInQueue_doesNotThrow() {
    EntityDto entityDto = mockEntityDto();
    when(dbClient.entityDao().selectByKey(dbSession, entityDto.getKey())).thenReturn(Optional.of(entityDto));

    mockCeQueueDto(GITHUB_PROJECT_PERMISSIONS_PROVISIONING, entityDto.getKey());

    visibilityService.checkNoPendingTasks(dbSession, entityDto);
  }

  @Test
  public void checkNoPendingTasks_whenAnyOtherTaskInQueue_throws() {
    EntityDto entityDto = mockEntityDto();
    when(dbClient.entityDao().selectByKey(dbSession, entityDto.getKey())).thenReturn(Optional.of(entityDto));

    mockCeQueueDto("ANYTHING", entityDto.getUuid());

    assertThatIllegalStateException()
      .isThrownBy(() -> visibilityService.checkNoPendingTasks(dbSession, entityDto))
      .withMessage("Component visibility can't be changed as long as it has background task(s) pending or in progress");
  }

  private void mockCeQueueDto(String taskType, String entityDto) {
    CeQueueDto ceQueueDto = mock(CeQueueDto.class);
    when(ceQueueDto.getTaskType()).thenReturn(taskType);
    when(dbClient.ceQueueDao().selectByEntityUuid(dbSession, entityDto)).thenReturn(List.of(ceQueueDto));
  }

  private static EntityDto mockEntityDto() {
    EntityDto entityDto = mock(EntityDto.class);
    when(entityDto.getKey()).thenReturn("entityKey");
    when(entityDto.getUuid()).thenReturn("entityUuid");
    return entityDto;
  }
}

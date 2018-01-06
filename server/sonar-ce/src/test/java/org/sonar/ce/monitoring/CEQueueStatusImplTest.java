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
package org.sonar.ce.monitoring;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CEQueueStatusImplTest {
  private static final int SOME_RANDOM_MAX = 96535;
  private static final int SOME_PROCESSING_TIME = 8723;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private CEQueueStatusImpl underTest = new CEQueueStatusImpl(dbClient);

  @Test
  public void verify_just_created_instance_metrics() {
    assertThat(underTest.getInProgressCount()).isEqualTo(0);
    assertThat(underTest.getErrorCount()).isEqualTo(0);
    assertThat(underTest.getSuccessCount()).isEqualTo(0);
    assertThat(underTest.getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addInProgress_increases_InProgress() {
    underTest.addInProgress();

    assertThat(underTest.getInProgressCount()).isEqualTo(1);
    assertThat(underTest.getErrorCount()).isEqualTo(0);
    assertThat(underTest.getSuccessCount()).isEqualTo(0);
    assertThat(underTest.getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addInProgress_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      underTest.addInProgress();
    }

    assertThat(underTest.getInProgressCount()).isEqualTo(calls);
    assertThat(underTest.getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addError_throws_IAE_if_time_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Processing time can not be < 0");

    underTest.addError(-1);
  }

  @Test
  public void addError_increases_Error_and_decreases_InProgress_by_1_without_check_on_InProgress() {
    underTest.addError(SOME_PROCESSING_TIME);

    assertThat(underTest.getInProgressCount()).isEqualTo(-1);
    assertThat(underTest.getErrorCount()).isEqualTo(1);
    assertThat(underTest.getSuccessCount()).isEqualTo(0);
    assertThat(underTest.getProcessingTime()).isEqualTo(SOME_PROCESSING_TIME);
  }

  @Test
  public void addError_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      underTest.addError(1);
    }

    assertThat(underTest.getErrorCount()).isEqualTo(calls);
    assertThat(underTest.getInProgressCount()).isEqualTo(-calls);
    assertThat(underTest.getProcessingTime()).isEqualTo(calls);
  }

  @Test
  public void addSuccess_throws_IAE_if_time_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Processing time can not be < 0");

    underTest.addSuccess(-1);
  }

  @Test
  public void addSuccess_increases_Error_and_decreases_InProgress_by_1_without_check_on_InProgress() {
    underTest.addSuccess(SOME_PROCESSING_TIME);

    assertThat(underTest.getInProgressCount()).isEqualTo(-1);
    assertThat(underTest.getErrorCount()).isEqualTo(0);
    assertThat(underTest.getSuccessCount()).isEqualTo(1);
    assertThat(underTest.getProcessingTime()).isEqualTo(SOME_PROCESSING_TIME);
  }

  @Test
  public void addSuccess_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      underTest.addSuccess(1);
    }

    assertThat(underTest.getSuccessCount()).isEqualTo(calls);
    assertThat(underTest.getInProgressCount()).isEqualTo(-calls);
    assertThat(underTest.getProcessingTime()).isEqualTo(calls);
  }

  @Test
  public void count_Pending_from_database() {
    when(dbClient.ceQueueDao().countByStatus(any(DbSession.class), eq(CeQueueDto.Status.PENDING))).thenReturn(42);

    assertThat(underTest.getPendingCount()).isEqualTo(42);
  }
}

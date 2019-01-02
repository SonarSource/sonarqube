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
package org.sonar.ce.monitoring;

import java.util.Optional;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.server.property.InternalProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public abstract class CommonCEQueueStatusImplTest {
  private static final int SOME_RANDOM_MAX = 96535;
  private static final int SOME_PROCESSING_TIME = 8723;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient;

  protected CommonCEQueueStatusImplTest(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public DbClient getDbClient() {
    return dbClient;
  }

  protected abstract CEQueueStatusImpl getUnderTest();

  @Test
  public void verify_just_created_instance_metrics() {
    assertThat(getUnderTest().getInProgressCount()).isEqualTo(0);
    assertThat(getUnderTest().getErrorCount()).isEqualTo(0);
    assertThat(getUnderTest().getSuccessCount()).isEqualTo(0);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addInProgress_increases_InProgress() {
    getUnderTest().addInProgress();

    assertThat(getUnderTest().getInProgressCount()).isEqualTo(1);
    assertThat(getUnderTest().getErrorCount()).isEqualTo(0);
    assertThat(getUnderTest().getSuccessCount()).isEqualTo(0);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addInProgress_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      getUnderTest().addInProgress();
    }

    assertThat(getUnderTest().getInProgressCount()).isEqualTo(calls);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void addError_throws_IAE_if_time_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Processing time can not be < 0");

    getUnderTest().addError(-1);
  }

  @Test
  public void addError_increases_Error_and_decreases_InProgress_by_1_without_check_on_InProgress() {
    getUnderTest().addError(SOME_PROCESSING_TIME);

    assertThat(getUnderTest().getInProgressCount()).isEqualTo(-1);
    assertThat(getUnderTest().getErrorCount()).isEqualTo(1);
    assertThat(getUnderTest().getSuccessCount()).isEqualTo(0);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(SOME_PROCESSING_TIME);
  }

  @Test
  public void addError_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      getUnderTest().addError(1);
    }

    assertThat(getUnderTest().getErrorCount()).isEqualTo(calls);
    assertThat(getUnderTest().getInProgressCount()).isEqualTo(-calls);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(calls);
  }

  @Test
  public void addSuccess_throws_IAE_if_time_is_less_than_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Processing time can not be < 0");

    getUnderTest().addSuccess(-1);
  }

  @Test
  public void addSuccess_increases_Error_and_decreases_InProgress_by_1_without_check_on_InProgress() {
    getUnderTest().addSuccess(SOME_PROCESSING_TIME);

    assertThat(getUnderTest().getInProgressCount()).isEqualTo(-1);
    assertThat(getUnderTest().getErrorCount()).isEqualTo(0);
    assertThat(getUnderTest().getSuccessCount()).isEqualTo(1);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(SOME_PROCESSING_TIME);
  }

  @Test
  public void addSuccess_any_number_of_call_change_by_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      getUnderTest().addSuccess(1);
    }

    assertThat(getUnderTest().getSuccessCount()).isEqualTo(calls);
    assertThat(getUnderTest().getInProgressCount()).isEqualTo(-calls);
    assertThat(getUnderTest().getProcessingTime()).isEqualTo(calls);
  }

  @Test
  public void workers_pause_is_loaded_from_db() {
    when(dbClient.internalPropertiesDao().selectByKey(any(), eq(InternalProperties.COMPUTE_ENGINE_PAUSE))).thenReturn(Optional.of("true"));

    assertThat(getUnderTest().areWorkersPaused()).isTrue();
  }

  @Test
  public void workers_pause_is_false_by_default() {
    assertThat(getUnderTest().areWorkersPaused()).isFalse();
  }
}

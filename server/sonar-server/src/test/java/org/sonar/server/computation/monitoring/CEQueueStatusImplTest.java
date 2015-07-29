/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.monitoring;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class CEQueueStatusImplTest {
  private static final String ISE_initPendingCount_CALL_MSG = "Method initPendingCount must be used before any other method and can not be called twice";
  private static final int SOME_RANDOM_MAX = 96535;
  private static final int SOME_PROCESSING_TIME = 8723;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CEQueueStatusImpl underTest = new CEQueueStatusImpl();

  @Test
  public void verify_just_created_instance_metrics() {
    assertThat(underTest.getReceivedCount()).isEqualTo(0);
    assertThat(underTest.getPendingCount()).isEqualTo(0);
    assertThat(underTest.getInProgressCount()).isEqualTo(0);
    assertThat(underTest.getErrorCount()).isEqualTo(0);
    assertThat(underTest.getSuccessCount()).isEqualTo(0);
    assertThat(underTest.getProcessingTime()).isEqualTo(0);
  }

  @Test
  public void initPendingCount_sets_value_of_pendingCount() {
    underTest.initPendingCount(10);

    assertThat(underTest.getPendingCount()).isEqualTo(10);
  }

  @Test
  public void initPendingCount_throws_ISE_if_called_twice() {
    expectISEForIllegalCallToInitPendingCount();

    underTest.initPendingCount(10);
    underTest.initPendingCount(10);
  }

  @Test
  public void initPendingCount_throws_ISE_if_called_after_getPendingCount() {
    expectISEForIllegalCallToInitPendingCount();

    underTest.getPendingCount();
    underTest.initPendingCount(10);
  }

  @Test
  public void initPendingCount_throws_ISE_if_called_after_addReceived() {
    expectISEForIllegalCallToInitPendingCount();

    underTest.addReceived();
    underTest.initPendingCount(10);
  }

  @Test
  public void initPendingCount_throws_ISE_if_called_after_addInProgress() {
    expectISEForIllegalCallToInitPendingCount();

    underTest.addInProgress();
    underTest.initPendingCount(10);
  }

  private void expectISEForIllegalCallToInitPendingCount() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(ISE_initPendingCount_CALL_MSG);
  }

  @Test
  public void addReceived_sets_received_and_pending_counts_to_1_when_initPendingCount_has_not_been_called() {
    underTest.addReceived();

    assertThat(underTest.getReceivedCount()).isEqualTo(1);
    assertThat(underTest.getPendingCount()).isEqualTo(1);
  }

  @Test
  public void addReceived_any_number_of_call_adds_1_per_call() {
    int calls = new Random().nextInt(SOME_RANDOM_MAX);
    for (int i = 0; i < calls; i++) {
      underTest.addReceived();
    }

    assertThat(underTest.getReceivedCount()).isEqualTo(calls);
    assertThat(underTest.getPendingCount()).isEqualTo(calls);
  }

  @Test
  public void addInProgress_increases_InProgress_and_decreases_Pending_by_1_without_check_on_Pending() {
    underTest.addInProgress();

    assertThat(underTest.getReceivedCount()).isEqualTo(0);
    assertThat(underTest.getPendingCount()).isEqualTo(-1);
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
    assertThat(underTest.getPendingCount()).isEqualTo(-calls);
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

    assertThat(underTest.getReceivedCount()).isEqualTo(0);
    assertThat(underTest.getPendingCount()).isEqualTo(0);
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

    assertThat(underTest.getReceivedCount()).isEqualTo(0);
    assertThat(underTest.getPendingCount()).isEqualTo(0);
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
}

/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate.changeevent;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class QGChangeEventListenersImplTest {
  @Rule
  public LogTester logTester = new LogTester();

  private QGChangeEventListener listener1 = mock(QGChangeEventListener.class);
  private QGChangeEventListener listener2 = mock(QGChangeEventListener.class);
  private QGChangeEventListener listener3 = mock(QGChangeEventListener.class);
  private InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
  private List<QGChangeEvent> threeChangeEvents = Arrays.asList(mock(QGChangeEvent.class), mock(QGChangeEvent.class));

  private QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl(new QGChangeEventListener[] {listener1, listener2, listener3});

  @Test
  public void isEmpty_returns_true_for_constructor_without_argument() {
    QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl();

    assertThat(underTest.isEmpty()).isTrue();
  }

  @Test
  public void isEmpty_returns_false_for_constructor_with_one_argument() {
    QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl(new QGChangeEventListener[] {listener2});

    assertThat(underTest.isEmpty()).isFalse();
  }

  @Test
  public void isEmpty_returns_false_for_constructor_with_multiple_arguments() {
    QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl(new QGChangeEventListener[] {listener2, listener3});

    assertThat(underTest.isEmpty()).isFalse();
  }

  @Test
  public void no_effect_when_no_changeEvent() {
    underTest.broadcast(Trigger.ISSUE_CHANGE, Collections.emptySet());

    verifyZeroInteractions(listener1, listener2, listener3);
  }

  @Test
  public void broadcast_passes_Trigger_and_collection_to_all_listeners_in_order_of_addition_to_constructor() {
    underTest.broadcast(Trigger.ISSUE_CHANGE, threeChangeEvents);

    inOrder.verify(listener1).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verify(listener2).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verify(listener3).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void broadcast_calls_all_listeners_even_if_one_throws_an_exception() {
    QGChangeEventListener failingListener = new QGChangeEventListener[] {listener1, listener2, listener3}[new Random().nextInt(3)];
    doThrow(new RuntimeException("Faking an exception thrown by onChanges"))
      .when(failingListener)
      .onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);

    underTest.broadcast(Trigger.ISSUE_CHANGE, threeChangeEvents);

    inOrder.verify(listener1).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verify(listener2).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verify(listener3).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verifyNoMoreInteractions();
    assertThat(logTester.logs()).hasSize(4);
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
  }

  @Test
  public void broadcast_stops_calling_listeners_when_one_throws_an_ERROR() {
    doThrow(new Error("Faking an error thrown by a listener"))
      .when(listener2)
      .onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);

    underTest.broadcast(Trigger.ISSUE_CHANGE, threeChangeEvents);

    inOrder.verify(listener1).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verify(listener2).onChanges(Trigger.ISSUE_CHANGE, threeChangeEvents);
    inOrder.verifyNoMoreInteractions();
    assertThat(logTester.logs()).hasSize(3);
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
  }

  @Test
  public void broadcast_logs_each_listener_call_at_TRACE_level() {
    underTest.broadcast(Trigger.ISSUE_CHANGE, threeChangeEvents);

    assertThat(logTester.logs()).hasSize(3);
    List<String> traceLogs = logTester.logs(LoggerLevel.TRACE);
    assertThat(traceLogs).hasSize(3)
      .containsOnly(
        "calling onChange() on listener " + listener1.getClass().getName() + " for events " + threeChangeEvents.toString() + "...",
        "calling onChange() on listener " + listener2.getClass().getName() + " for events " + threeChangeEvents.toString() + "...",
        "calling onChange() on listener " + listener3.getClass().getName() + " for events " + threeChangeEvents.toString() + "...");
  }

  @Test
  public void broadcast_passes_immutable_list_of_events() {
    QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl(new QGChangeEventListener[] {listener1});

    underTest.broadcast(Trigger.ISSUE_CHANGE, threeChangeEvents);

    ArgumentCaptor<Collection> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(listener1).onChanges(eq(Trigger.ISSUE_CHANGE), collectionCaptor.capture());
    assertThat(collectionCaptor.getValue()).isInstanceOf(ImmutableList.class);
  }

  @Test
  public void no_effect_when_no_listener() {
    QGChangeEventListenersImpl underTest = new QGChangeEventListenersImpl();

    underTest.broadcast(Trigger.ISSUE_CHANGE, Collections.emptySet());

    verifyZeroInteractions(listener1, listener2, listener3);
  }

}

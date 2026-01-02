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
package org.sonar.ce.task.projectanalysis.event;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EventRepositoryImplTest {
  private static final Event EVENT_1 = Event.createProfile("event_1", null, null);
  private static final Event EVENT_2 = Event.createProfile("event_2", null, null);

  private final EventRepositoryImpl underTest = new EventRepositoryImpl();

  @Test
  public void getEvents_returns_empty_iterable_when_repository_is_empty() {
    assertThat(underTest.getEvents()).isEmpty();
  }

  @Test
  public void add_throws_NPE_if_even_arg_is_null() {
    assertThatThrownBy(() -> underTest.add(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void can_add_and_retrieve_many_events() {
    underTest.add(EVENT_1);
    underTest.add(EVENT_2);

    assertThat(underTest.getEvents()).extracting("name").containsOnly(EVENT_1.getName(), EVENT_2.getName());
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.pushevent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PushEventRepositoryImplTest {

  private final PushEventRepository underTest = new PushEventRepositoryImpl();

  @Test
  public void should_add_event_to_repository() {
    assertThat(underTest.getEvents()).isEmpty();

    underTest.add(new PushEvent<>().setName("event1").setData("event_data1"));
    underTest.add(new PushEvent<>().setName("event2").setData("event_data2"));

    assertThat(underTest.getEvents())
      .extracting(PushEvent::getData, PushEvent::getName)
      .containsExactlyInAnyOrder(tuple("event_data1", "event1"),
        tuple("event_data2", "event2"));

  }

}

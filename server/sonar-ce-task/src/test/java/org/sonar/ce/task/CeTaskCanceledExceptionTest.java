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
package org.sonar.ce.task;

import org.junit.Test;
import org.sonar.db.ce.CeActivityDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskCanceledExceptionTest {
  @Test
  public void message_is_based_on_specified_thread_name() {
    Thread t = new Thread();
    t.setName(randomAlphabetic(29));

    CeTaskCanceledException underTest = new CeTaskCanceledException(t);

    assertThat(underTest.getMessage()).isEqualTo("CeWorker executing in Thread '" + t.getName() + "' has been interrupted");
  }

  @Test
  public void getStatus_returns_CANCELED() {
    CeTaskCanceledException underTest = new CeTaskCanceledException(new Thread());

    assertThat(underTest.getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }
}

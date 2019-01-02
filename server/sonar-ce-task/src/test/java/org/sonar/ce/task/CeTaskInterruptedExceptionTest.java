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

import java.util.Random;
import org.junit.Test;
import org.sonar.db.ce.CeActivityDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.CeTaskInterruptedException.isTaskInterruptedException;

public class CeTaskInterruptedExceptionTest {

  @Test
  public void isCauseInterruptedException_returns_CeTaskInterruptedException_or_subclass() {
    String message = randomAlphabetic(50);
    CeActivityDto.Status status = randomStatus();
    CeTaskInterruptedException e1 = new CeTaskInterruptedException(message, status) {

    };
    CeTaskInterruptedException e2 = new CeTaskInterruptedExceptionSubclass(message, status);

    assertThat(isTaskInterruptedException(e1)).contains(e1);
    assertThat(isTaskInterruptedException(e2)).contains(e2);
    assertThat(isTaskInterruptedException(new RuntimeException())).isEmpty();
    assertThat(isTaskInterruptedException(new Exception())).isEmpty();
  }

  @Test
  public void isCauseInterruptedException_returns_CeTaskInterruptedException_or_subclass_in_cause_chain() {
    String message = randomAlphabetic(50);
    CeActivityDto.Status status = randomStatus();
    CeTaskInterruptedException e1 = new CeTaskInterruptedException(message, status) {

    };
    CeTaskInterruptedException e2 = new CeTaskInterruptedExceptionSubclass(message, status);

    assertThat(isTaskInterruptedException(new RuntimeException(e1))).contains(e1);
    assertThat(isTaskInterruptedException(new Exception(new RuntimeException(e2)))).contains(e2);
  }

  private static CeActivityDto.Status randomStatus() {
    return CeActivityDto.Status.values()[new Random().nextInt(CeActivityDto.Status.values().length)];
  }

  private static class CeTaskInterruptedExceptionSubclass extends CeTaskInterruptedException {
    public CeTaskInterruptedExceptionSubclass(String message, CeActivityDto.Status status) {
      super(message, status);
    }
  }

}

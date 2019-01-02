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

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.sonar.db.ce.CeActivityDto;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskTimeoutExceptionTest {
  private String message = RandomStringUtils.randomAlphabetic(50);
  private CeTaskTimeoutException underTest = new CeTaskTimeoutException(message);

  @Test
  public void verify_message_and_type() {
    assertThat(underTest.getMessage()).isEqualTo(message);
    assertThat(underTest.getType()).isEqualTo("TIMEOUT");
  }

  @Test
  public void getStatus_returns_FAILED() {
    assertThat(underTest.getStatus()).isEqualTo(CeActivityDto.Status.FAILED);
  }


  @Test
  public void noStacktrace() {
    StringWriter stacktrace = new StringWriter();
    underTest.printStackTrace(new PrintWriter(stacktrace));
    assertThat(stacktrace.toString())
      .isEqualTo(CeTaskTimeoutException.class.getCanonicalName() + ": " + message + System.lineSeparator());
  }
}

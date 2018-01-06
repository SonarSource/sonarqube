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
package org.sonar.ce.taskprocessor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.queue.CeTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ReportTaskProcessorDeclarationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ReportTaskProcessorDeclaration underTest = new ReportTaskProcessorDeclaration();

  @Test
  public void getHandledCeTaskTypes_returns_REPORT() {
    assertThat(underTest.getHandledCeTaskTypes()).containsOnly("REPORT");
  }

  @Test
  public void process_throws_UOE() {
    expectedException.expect(UnsupportedOperationException.class);
    expectedException.expectMessage("process must not be called in WebServer");

    underTest.process(mock(CeTask.class));
  }
}

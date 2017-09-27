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
package org.sonar.server.computation.task.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExecuteTaskInitExtensionsStepTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_getDescription() {
    ExecuteTaskInitExtensionsStep underTest = new ExecuteTaskInitExtensionsStep();

    assertThat(underTest.getDescription()).isEqualTo("Initialize");
  }

  @Test
  public void do_nothing_if_no_extensions() {
    ExecuteTaskInitExtensionsStep underTest = new ExecuteTaskInitExtensionsStep();

    // no failure
    underTest.execute();
  }

  @Test
  public void execute_extensions() {
    TaskInitExtension ext1 = mock(TaskInitExtension.class);
    TaskInitExtension ext2 = mock(TaskInitExtension.class);

    ExecuteTaskInitExtensionsStep underTest = new ExecuteTaskInitExtensionsStep(new TaskInitExtension[] {ext1, ext2});
    underTest.execute();

    verify(ext1).onInit();
    verify(ext2).onInit();
  }

  @Test
  public void fail_if_an_extension_throws_an_exception() {
    TaskInitExtension ext1 = mock(TaskInitExtension.class);
    doThrow(new IllegalStateException("BOOM")).when(ext1).onInit();
    TaskInitExtension ext2 = mock(TaskInitExtension.class);

    ExecuteTaskInitExtensionsStep underTest = new ExecuteTaskInitExtensionsStep(new TaskInitExtension[] {ext1, ext2});

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("BOOM");

    underTest.execute();
  }

}

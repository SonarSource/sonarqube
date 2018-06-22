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
package org.sonar.server.computation.task.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ExecuteStatelessInitExtensionsStepTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_getDescription() {
    ExecuteStatelessInitExtensionsStep underTest = new ExecuteStatelessInitExtensionsStep();

    assertThat(underTest.getDescription()).isEqualTo("Initialize");
  }

  @Test
  public void do_nothing_if_no_extensions() {
    ExecuteStatelessInitExtensionsStep underTest = new ExecuteStatelessInitExtensionsStep();

    // no failure
    underTest.execute();
  }

  @Test
  public void execute_extensions() {
    StatelessInitExtension ext1 = mock(StatelessInitExtension.class);
    StatelessInitExtension ext2 = mock(StatelessInitExtension.class);

    ExecuteStatelessInitExtensionsStep underTest = new ExecuteStatelessInitExtensionsStep(
      new StatelessInitExtension[] {ext1, ext2});
    underTest.execute();

    InOrder inOrder = inOrder(ext1, ext2);
    inOrder.verify(ext1).onInit();
    inOrder.verify(ext2).onInit();
  }

  @Test
  public void fail_if_an_extension_throws_an_exception() {
    StatelessInitExtension ext1 = mock(StatelessInitExtension.class);
    StatelessInitExtension ext2 = mock(StatelessInitExtension.class);
    doThrow(new IllegalStateException("BOOM")).when(ext2).onInit();
    StatelessInitExtension ext3 = mock(StatelessInitExtension.class);

    ExecuteStatelessInitExtensionsStep underTest = new ExecuteStatelessInitExtensionsStep(
      new StatelessInitExtension[] {ext1, ext2, ext3});

    try {
      underTest.execute();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("BOOM");
      verify(ext1).onInit();
      verify(ext3, never()).onInit();
    }
  }

}

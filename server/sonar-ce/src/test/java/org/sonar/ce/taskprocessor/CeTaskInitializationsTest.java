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
package org.sonar.ce.taskprocessor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.queue.CeTask;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CeTaskInitializationsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTask task = mock(CeTask.class);

  @Test
  public void execute_extensions() {
    CeTaskInitialization extension1 = mock(CeTaskInitialization.class);
    CeTaskInitialization extension2 = mock(CeTaskInitialization.class);

    CeTaskInitializations underTest = new CeTaskInitializations(new CeTaskInitialization[]{extension1, extension2});
    underTest.onInit(task);

    verify(extension1).onInit(task);
    verify(extension2).onInit(task);
  }

  @Test
  public void break_if_extension_throws_exception() {
    CeTaskInitialization extension1 = mock(CeTaskInitialization.class);
    doThrow(new IllegalStateException("BOOM")).when(extension1).onInit(task);
    CeTaskInitialization extension2 = mock(CeTaskInitialization.class);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("BOOM");

    CeTaskInitializations underTest = new CeTaskInitializations(new CeTaskInitialization[]{extension1, extension2});
    underTest.onInit(task);

    verifyZeroInteractions(extension2);
  }

  @Test
  public void do_nothing_by_default() {
    CeTaskInitializations underTest = new CeTaskInitializations();

    // no failure
    underTest.onInit(task);
  }
}

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
package org.sonar.ce.async;

import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SynchronousAsyncExecutionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SynchronousAsyncExecution underTest = new SynchronousAsyncExecution();

  @Test
  public void addToQueue_fails_with_NPE_if_Runnable_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.addToQueue(null);
  }

  @Test
  public void addToQueue_executes_Runnable_synchronously() {
    Set<String> s = new HashSet<>();

    underTest.addToQueue(() -> s.add("done"));

    assertThat(s).containsOnly("done");
  }
}

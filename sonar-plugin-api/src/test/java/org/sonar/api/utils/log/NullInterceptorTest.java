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
package org.sonar.api.utils.log;

import org.junit.Test;

import static org.mockito.Mockito.mock;

public class NullInterceptorTest {

  @Test
  public void do_not_throws_exception() {
    // verify that... it does nothing
    NullInterceptor.NULL_INSTANCE.log(LoggerLevel.INFO, "foo");
    NullInterceptor.NULL_INSTANCE.log(LoggerLevel.INFO, "foo {}", 42);
    NullInterceptor.NULL_INSTANCE.log(LoggerLevel.INFO, "foo {} {}", 42, 66);
    NullInterceptor.NULL_INSTANCE.log(LoggerLevel.INFO, "foo {} {} {}", 42, 66, 84);
    NullInterceptor.NULL_INSTANCE.log(LoggerLevel.INFO, "foo", mock(Exception.class));
  }
}

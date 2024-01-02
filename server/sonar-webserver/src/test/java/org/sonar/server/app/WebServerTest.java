/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.app;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class WebServerTest {

  @Test
  public void main_givenNoArguments() {
    String[] arguments = {};

    ThrowingRunnable runnable = () -> WebServer.main(arguments);

    Assert.assertThrows("Only a single command-line argument is accepted (absolute path to configuration file)",
      IllegalArgumentException.class, runnable);
  }
}

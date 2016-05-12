/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.monitor;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessId;

public class JavaProcessLauncherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fail_to_launch() throws Exception {
    File tempDir = temp.newFolder();
    JavaCommand command = new JavaCommand(ProcessId.ELASTICSEARCH);
    JavaProcessLauncher launcher = new JavaProcessLauncher(new Timeouts(), tempDir);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to launch [es]");

    // command is not correct (missing options), java.lang.ProcessBuilder#start()
    // throws an exception
    launcher.launch(command);
  }
}

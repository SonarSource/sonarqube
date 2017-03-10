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
package org.sonar.application.process;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface JavaProcessLauncher extends Closeable {

  class SystemProcessBuilder {
    private final ProcessBuilder builder = new ProcessBuilder();

    /**
     * @see ProcessBuilder#command()
     */
    public List<String> command() {
      return builder.command();
    }

    /**
     * @see ProcessBuilder#command(List)
     */
    public SystemProcessBuilder command(List<String> commands) {
      builder.command(commands);
      return this;
    }

    /**
     * @see ProcessBuilder#directory(File)
     */
    public SystemProcessBuilder directory(File dir) {
      builder.directory(dir);
      return this;
    }

    /**
     * @see ProcessBuilder#environment()
     */
    public Map<String, String> environment() {
      return builder.environment();
    }

    /**
     * @see ProcessBuilder#redirectErrorStream(boolean)
     */
    public SystemProcessBuilder redirectErrorStream(boolean b) {
      builder.redirectErrorStream(b);
      return this;
    }

    /**
     * @see ProcessBuilder#start()
     */
    public Process start() throws IOException {
      return builder.start();
    }
  }

  @Override
  void close();

  /**
   * Launch Java command. An {@link IllegalStateException} is thrown
   * on error.
   */
  ProcessMonitor launch(JavaCommand javaCommand);
}

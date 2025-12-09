/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.bootstrap;

import java.lang.reflect.Method;
import org.eclipse.jgit.internal.util.CleanupService;

/**
 * Normally, JGit terminates with a shutdown hook. Since we also want to support running the Scanner Engine in the same JVM, this allows triggering shutdown manually.
 */
class JGitCleanupService implements AutoCloseable {

  private final Method shutDownMethod;
  private final CleanupService cleanupService;

  public JGitCleanupService() {
    cleanupService = new CleanupService();
    try {
      shutDownMethod = CleanupService.class.getDeclaredMethod("shutDown");
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unable to find method 'shutDown' on JGit CleanupService", e);
    }
    shutDownMethod.setAccessible(true);
  }

  @Override
  public void close() throws Exception {
    shutDownMethod.invoke(cleanupService);
  }
}

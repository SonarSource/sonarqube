/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user;

import org.sonar.core.permission.GlobalPermissions;

import java.util.Locale;

/**
 * Allow code to be executed with the highest privileges possible, as if executed by a {@link GlobalPermissions#SYSTEM_ADMIN} account.
 * @since 4.3
 */
public final class DoPrivileged {

  private DoPrivileged() {
    // Only static stuff
  }

  /**
   * Executes the task's <code>{@link Task#doPrivileged() doPrivileged}</code> method in a privileged environment.
   * @param task
   */
  public static void execute(Task task) {
    try {
      task.start();
      task.doPrivileged();
    } finally {
      task.stop();
    }
  }

  /**
   * Define a task that will be executed using the highest privileges available. The privileged section is restricted
   * to the execution of the {@link #doPrivileged()} method.
   */
  public abstract static class Task {

    /**
     * Code placed in this method will be executed in a privileged environment.
     */
    protected abstract void doPrivileged();

    private void start() {
      UserSession.set(new UserSession() {
        @Override
        public boolean hasGlobalPermission(String globalPermission) {
          return true;
        }
        @Override
        public boolean hasProjectPermission(String permission, String projectKey) {
          return true;
        }
      }.setLocale(Locale.getDefault()));
    }
  
    private void stop() {
      UserSession.remove();
    }
  }
}

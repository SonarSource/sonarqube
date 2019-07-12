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
package org.sonar.server.user;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;

/**
 * Allow code to be executed with the highest privileges possible, as if executed by a {@link OrganizationPermission#ADMINISTER} account.
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
    private final ThreadLocalUserSession threadLocalUserSession;
    private UserSession oldUserSession;

    protected Task(ThreadLocalUserSession threadLocalUserSession) {
      this.threadLocalUserSession = threadLocalUserSession;
    }

    /**
     * Code placed in this method will be executed in a privileged environment.
     */
    protected abstract void doPrivileged();

    private static class PrivilegedUserSession extends AbstractUserSession {
      @Override
      public String getLogin() {
        return null;
      }

      @Override
      public String getUuid() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public Integer getUserId() {
        return null;
      }

      @Override
      public Collection<GroupDto> getGroups() {
        return Collections.emptyList();
      }

      @Override
      public boolean isLoggedIn() {
        return false;
      }

      @Override
      public boolean isRoot() {
        return true;
      }

      @Override
      public Optional<IdentityProvider> getIdentityProvider() {
        return Optional.empty();
      }

      @Override
      public Optional<ExternalIdentity> getExternalIdentity() {
        return Optional.empty();
      }

      @Override
      protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
        return true;
      }

      @Override
      protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
        // always root so unused
        throw new UnsupportedOperationException();
      }

      @Override
      protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
        return true;
      }

      @Override
      public boolean isSystemAdministrator() {
        return true;
      }

      @Override
      public boolean hasMembershipImpl(OrganizationDto organizationDto) {
        return true;
      }
    }

    private void start() {
      oldUserSession = threadLocalUserSession.hasSession() ? threadLocalUserSession.get() : null;
      threadLocalUserSession.set(new PrivilegedUserSession());
    }

    private void stop() {
      threadLocalUserSession.unload();
      if (oldUserSession != null) {
        threadLocalUserSession.set(oldUserSession);
      }
    }
  }
}

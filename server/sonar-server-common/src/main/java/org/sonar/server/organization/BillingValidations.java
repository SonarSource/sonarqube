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
package org.sonar.server.organization;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.MessageException;

import static java.util.Objects.requireNonNull;

/**
 * Available checks that will be done by the billing plugin.
 * When the billing plugin is not loaded, no check will be done.
 * This is not the interface that should be implemented by the plugin, but {@link BillingValidationsExtension}
 */
@ComputeEngineSide
@ServerSide
public interface BillingValidations {

  /**
   * @throws BillingValidationsException when projects analysis on organization is not allowed
   */
  void checkBeforeProjectAnalysis(Organization organization);

  /**
   * @throws BillingValidationsException when the organization cannot use private projects
   */
  void checkCanUpdateProjectVisibility(Organization organization, boolean updateToPrivate);

  /**
   * @return true if the organization can use private projects
   */
  boolean canUpdateProjectVisibilityToPrivate(Organization organization);

  /**
   * Actions to do on an organization deletion
   */
  void onDelete(Organization organization);

  class Organization {
    private final String key;
    private final String uuid;
    private final String name;

    public Organization(String key, String uuid, String name) {
      this.key = requireNonNull(key, "Organization key cannot be null");
      this.uuid = requireNonNull(uuid, "Organization uuid cannot be null");
      this.name = requireNonNull(name, "Organization name cannot be null");
    }

    public String getKey() {
      return key;
    }

    public String getUuid() {
      return uuid;
    }

    public String getName() {
      return name;
    }
  }

  class BillingValidationsException extends MessageException {
    public BillingValidationsException(String message) {
      super(message);
    }

    /**
     * Does not fill in the stack trace
     *
     * @see java.lang.Throwable#fillInStackTrace()
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }

    @Override
    public String toString() {
      return getMessage();
    }
  }
}

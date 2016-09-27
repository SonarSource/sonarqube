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
package org.sonar.server.organization;

import com.google.common.base.Preconditions;
import org.junit.rules.ExternalResource;
import org.sonar.core.util.UuidFactoryImpl;

public class DefaultOrganizationProviderRule extends ExternalResource implements DefaultOrganizationProvider {
  private DefaultOrganization defaultOrganization;

  private DefaultOrganizationProviderRule(DefaultOrganization defaultOrganization) {
    this.defaultOrganization = defaultOrganization;
  }

  /**
   * <p>
   * This method is meant to be statically imported.
   * </p>
   */
  public static DefaultOrganizationProviderRule someDefaultOrganization() {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    return new DefaultOrganizationProviderRule(DefaultOrganization.newBuilder()
      .setUuid(uuid)
      .setName("Default organization " + uuid)
      .setKey(uuid + "_key")
      .setCreatedAt(uuid.hashCode())
      .setUpdatedAt(uuid.hashCode())
      .build());
  }

  /**
   * <p>
   * This method is meant to be statically imported.
   * </p>
   */
  public static DefaultOrganizationProviderRule defaultOrganizationWithName(String name) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    return new DefaultOrganizationProviderRule(DefaultOrganization.newBuilder()
      .setUuid(uuid)
      .setName(name)
      .setKey(uuid + "_key")
      .setCreatedAt(uuid.hashCode())
      .setUpdatedAt(uuid.hashCode())
      .build());
  }

  @Override
  public DefaultOrganization get() {
    Preconditions.checkState(defaultOrganization != null, "No default organization is set");
    return defaultOrganization;
  }
}

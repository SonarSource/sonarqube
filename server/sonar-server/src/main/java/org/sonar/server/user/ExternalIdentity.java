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
package org.sonar.server.user;

import static java.util.Objects.requireNonNull;

public class ExternalIdentity {

  public static final String SQ_AUTHORITY = "sonarqube";

  private String provider;
  private String id;

  public ExternalIdentity(String provider, String id) {
    this.provider = requireNonNull(provider, "Identity provider cannot be null");
    this.id = requireNonNull(id, "Identity id cannot be null");
  }

  public String getProvider() {
    return provider;
  }

  public String getId() {
    return id;
  }
}

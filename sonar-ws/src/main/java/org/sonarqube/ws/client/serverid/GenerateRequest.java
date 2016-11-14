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

package org.sonarqube.ws.client.serverid;

import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class GenerateRequest {
  private final String organization;
  private final String ip;

  private GenerateRequest(Builder builder) {
    this.organization = builder.organization;
    this.ip = builder.ip;
  }

  public String getOrganization() {
    return organization;
  }

  public String getIp() {
    return ip;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private String ip;

    private Builder() {
      // enforce static constructor
    }

    public Builder setOrganization(String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setIp(String ip) {
      this.ip = ip;
      return this;
    }

    public GenerateRequest build() {
      checkArgument(organization != null && !organization.isEmpty(), "Organization must not be null or empty");
      checkArgument(ip != null && !ip.isEmpty(), "IP must not be null or empty");
      return new GenerateRequest(this);
    }
  }
}

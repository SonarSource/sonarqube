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
package org.sonarqube.ws.client.qualityprofile;

import java.io.File;
import java.util.Optional;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class RestoreWsRequest {

  private final File backup;
  private final Optional<String> organization;

  private RestoreWsRequest(Builder builder) {
    backup = requireNonNull(builder.backup);
    organization = requireNonNull(builder.organization);
  }

  public File getBackup() {
    return backup;
  }

  public Optional<String> getOrganization() {
    return organization;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private File backup;
    private Optional<String> organization = Optional.empty();

    public Builder setBackup(File backup) {
      this.backup = backup;
      return this;
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = Optional.ofNullable(organization);
      return this;
    }

    public RestoreWsRequest build() {
      return new RestoreWsRequest(this);
    }
  }
}

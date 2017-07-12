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
package org.sonarqube.ws.client.project;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class BulkDeleteRequest {

  private final String organization;
  private final Collection<String> projectKeys;

  private BulkDeleteRequest(Builder builder) {
    this.organization = builder.organization;
    this.projectKeys = builder.projectKeys;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public Collection<String> getProjectKeys() {
    return projectKeys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private final Collection<String> projectKeys = new ArrayList<>();

    private Builder() {
    }

    public Builder setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }

    public Builder setProjectKeys(Collection<String> s) {
      this.projectKeys.addAll(s);
      return this;
    }

    public BulkDeleteRequest build() {
      return new BulkDeleteRequest(this);
    }
  }
}

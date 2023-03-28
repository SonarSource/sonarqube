/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import org.sonar.server.user.ws.SearchUsersRequest;

class SearchQualityGateUsersRequest extends SearchUsersRequest {
  private String qualityGate;

  private SearchQualityGateUsersRequest(Builder builder) {
    this.qualityGate = builder.qualityGate;
    this.organization = builder.getOrganization();
    this.selected = builder.getSelected();
    this.query = builder.getQuery();
    this.page = builder.getPage();
    this.pageSize = builder.getPageSize();
  }

  public String getQualityGate() {
    return qualityGate;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends SearchUsersRequest.Builder<Builder> {
    private String qualityGate;

    public Builder setQualityGate(String qualityGate) {
      this.qualityGate = qualityGate;
      return this;
    }

    public SearchQualityGateUsersRequest build() {
      return new SearchQualityGateUsersRequest(this);
    }
  }
}

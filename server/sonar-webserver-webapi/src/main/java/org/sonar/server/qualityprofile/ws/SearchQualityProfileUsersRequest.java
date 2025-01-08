/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.server.user.ws.SearchUsersRequest;

class SearchQualityProfileUsersRequest extends SearchUsersRequest {
  private String qualityProfile;
  private String language;

  private SearchQualityProfileUsersRequest(Builder builder) {
    this.qualityProfile = builder.qualityProfile;
    this.language = builder.language;
    this.selected = builder.getSelected();
    this.query = builder.getQuery();
    this.page = builder.getPage();
    this.pageSize = builder.getPageSize();
  }

  public String getQualityProfile() {
    return qualityProfile;
  }

  public String getLanguage() {
    return language;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends SearchUsersRequest.Builder<Builder> {
    private String qualityProfile;
    private String language;

    public Builder setQualityProfile(String qualityProfile) {
      this.qualityProfile = qualityProfile;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public SearchQualityProfileUsersRequest build() {
      return new SearchQualityProfileUsersRequest(this);
    }
  }
}

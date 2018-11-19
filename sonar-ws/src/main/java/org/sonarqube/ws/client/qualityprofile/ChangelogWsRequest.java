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
package org.sonarqube.ws.client.qualityprofile;

public class ChangelogWsRequest {

  private final String language;
  private final String organization;
  private final Integer p;
  private final Integer ps;
  private final String qualityProfile;
  private final String since;
  private final String to;

  private ChangelogWsRequest(Builder builder) {
    this.language = builder.language;
    this.organization = builder.organization;
    this.p = builder.p;
    this.ps = builder.ps;
    this.qualityProfile = builder.qualityProfile;
    this.since = builder.since;
    this.to = builder.to;
  }

  public String getLanguage() {
    return language;
  }

  public String getOrganization() {
    return organization;
  }

  public Integer getP() {
    return p;
  }

  public Integer getPs() {
    return ps;
  }

  public String getQualityProfile() {
    return qualityProfile;
  }

  public String getSince() {
    return since;
  }

  public String getTo() {
    return to;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String language;
    private String organization;
    private Integer p;
    private Integer ps;
    private String qualityProfile;
    private String since;
    private String to;

    private Builder() {
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setOrganization(String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setP(Integer p) {
      this.p = p;
      return this;
    }

    public Builder setPs(Integer ps) {
      this.ps = ps;
      return this;
    }

    public Builder setQualityProfile(String qualityProfile) {
      this.qualityProfile = qualityProfile;
      return this;
    }

    public Builder setSince(String since) {
      this.since = since;
      return this;
    }

    public Builder setTo(String to) {
      this.to = to;
      return this;
    }

    public ChangelogWsRequest build() {
      return new ChangelogWsRequest(this);
    }
  }
}

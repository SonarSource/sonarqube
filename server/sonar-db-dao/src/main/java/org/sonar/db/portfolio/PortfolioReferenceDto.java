/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.portfolio;

public class PortfolioReferenceDto {
  private String uuid;
  private String portfolioUuid;
  private String referenceUuid;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  public PortfolioReferenceDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getPortfolioUuid() {
    return portfolioUuid;
  }

  public PortfolioReferenceDto setPortfolioUuid(String portfolioUuid) {
    this.portfolioUuid = portfolioUuid;
    return this;
  }

  public String getReferenceUuid() {
    return referenceUuid;
  }

  public PortfolioReferenceDto setReferenceUuid(String referenceUuid) {
    this.referenceUuid = referenceUuid;
    return this;
  }


  public long getCreatedAt() {
    return createdAt;
  }

  public PortfolioReferenceDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}

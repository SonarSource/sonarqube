/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.user;

public class SamlMessageIdDto {

  private String uuid;

  /**
   * Message ID from the SAML response received during authentication.
   */
  private String messageId;

  /**
   * Expiration date is coming from the NotOnOrAfter attribute of the SAML response.
   *
   * A row that contained an expired date can be safely deleted from database.
   */
  private long expirationDate;

  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  SamlMessageIdDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public SamlMessageIdDto setMessageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public long getExpirationDate() {
    return expirationDate;
  }

  public SamlMessageIdDto setExpirationDate(long expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  SamlMessageIdDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}

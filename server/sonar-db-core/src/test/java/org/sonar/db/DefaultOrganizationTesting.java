/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db;

public class DefaultOrganizationTesting {
  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String DEFAULT_ORGANIZATION_UUID = "def-org";

  private final CoreDbTester db;

  public DefaultOrganizationTesting(CoreDbTester db) {
    this.db = db;
  }

  public String setupDefaultOrganization() {
    insertInternalProperty(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(DEFAULT_ORGANIZATION_UUID);
    return DEFAULT_ORGANIZATION_UUID;
  }

  public String insertOrganization() {
    insertOrganization(DEFAULT_ORGANIZATION_UUID);
    return DEFAULT_ORGANIZATION_UUID;
  }

  public void insertOrganization(String uuid) {
    db.executeInsert(
      TABLE_ORGANIZATIONS,
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", String.valueOf(false),
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }

  public String insertInternalProperty() {
    insertInternalProperty(DEFAULT_ORGANIZATION_UUID);
    return DEFAULT_ORGANIZATION_UUID;
  }

  public void insertInternalProperty(String defaultOrganizationUuid) {
    db.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", defaultOrganizationUuid);
  }

}

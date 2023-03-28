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
package org.sonar.server.user.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ORGANIZATION_UUIDS;
import static org.sonar.server.user.index.UserIndexDefinition.TYPE_USER;

public class UserDoc extends BaseDoc {

  public UserDoc(Map<String, Object> fields) {
    super(TYPE_USER, fields);
  }

  public UserDoc() {
    this(new HashMap<>());
  }

  @Override
  public String getId() {
    return uuid();
  }

  public String uuid() {
    return getField(UserIndexDefinition.FIELD_UUID);
  }

  public String login() {
    return getField(UserIndexDefinition.FIELD_LOGIN);
  }

  public String name() {
    return getField(UserIndexDefinition.FIELD_NAME);
  }

  @Nullable
  public String email() {
    return getNullableField(UserIndexDefinition.FIELD_EMAIL);
  }

  public boolean active() {
    return getField(UserIndexDefinition.FIELD_ACTIVE);
  }

  public List<String> scmAccounts() {
    return getField(UserIndexDefinition.FIELD_SCM_ACCOUNTS);
  }

  public List<String> organizationUuids() {
    return getField(FIELD_ORGANIZATION_UUIDS);
  }

  public UserDoc setUuid(@Nullable String s) {
    setField(UserIndexDefinition.FIELD_UUID, s);
    return this;
  }

  public UserDoc setLogin(@Nullable String s) {
    setField(UserIndexDefinition.FIELD_LOGIN, s);
    return this;
  }

  public UserDoc setName(@Nullable String s) {
    setField(UserIndexDefinition.FIELD_NAME, s);
    return this;
  }

  public UserDoc setEmail(@Nullable String s) {
    setField(UserIndexDefinition.FIELD_EMAIL, s);
    return this;
  }

  public UserDoc setActive(boolean b) {
    setField(UserIndexDefinition.FIELD_ACTIVE, b);
    return this;
  }

  public UserDoc setScmAccounts(@Nullable List<String> s) {
    setField(UserIndexDefinition.FIELD_SCM_ACCOUNTS, s);
    return this;
  }

  public UserDoc setOrganizationUuids(@Nullable List<String> organizationUuids) {
    setField(FIELD_ORGANIZATION_UUIDS, organizationUuids);
    return this;
  }
}

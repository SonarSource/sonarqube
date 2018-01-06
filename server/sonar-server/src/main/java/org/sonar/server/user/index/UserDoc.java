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
package org.sonar.server.user.index;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.user.User;
import org.sonar.server.es.BaseDoc;

import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ORGANIZATION_UUIDS;

public class UserDoc extends BaseDoc implements User {

  public UserDoc(Map<String, Object> fields) {
    super(fields);
  }

  public UserDoc() {
    this(Maps.newHashMap());
  }

  @Override
  public String getId() {
    return login();
  }

  @Override
  public String getRouting() {
    return null;
  }

  @Override
  public String getParent() {
    return null;
  }

  @Override
  public String login() {
    return getField(UserIndexDefinition.FIELD_LOGIN);
  }

  @Override
  public String name() {
    return getField(UserIndexDefinition.FIELD_NAME);
  }

  @Override
  @Nullable
  public String email() {
    return getNullableField(UserIndexDefinition.FIELD_EMAIL);
  }

  @Override
  public boolean active() {
    return (Boolean) getField(UserIndexDefinition.FIELD_ACTIVE);
  }

  public List<String> scmAccounts() {
    return getField(UserIndexDefinition.FIELD_SCM_ACCOUNTS);
  }

  public List<String> organizationUuids() {
    return getField(FIELD_ORGANIZATION_UUIDS);
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

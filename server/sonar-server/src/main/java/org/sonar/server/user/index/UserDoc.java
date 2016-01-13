/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.user.User;
import org.sonar.server.search.BaseDoc;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class UserDoc extends BaseDoc implements User {

  public UserDoc(Map<String, Object> fields) {
    super(fields);
  }

  public UserDoc() {
    this(Maps.<String, Object>newHashMap());
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
    return (List<String>) getField(UserIndexDefinition.FIELD_SCM_ACCOUNTS);
  }

  public long createdAt() {
    return getField(UserIndexDefinition.FIELD_CREATED_AT);
  }

  public long updatedAt() {
    return getField(UserIndexDefinition.FIELD_UPDATED_AT);
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

  public UserDoc setCreatedAt(long l) {
    setField(UserIndexDefinition.FIELD_CREATED_AT, l);
    return this;
  }

  public UserDoc setUpdatedAt(long l) {
    setField(UserIndexDefinition.FIELD_UPDATED_AT, l);
    return this;
  }
}

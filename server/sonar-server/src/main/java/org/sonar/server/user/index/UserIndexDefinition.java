/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

/**
 * Definition of ES index "users", including settings and fields.
 */
public class UserIndexDefinition implements IndexDefinition {

  public static final String INDEX = "users";

  public static final String TYPE_USER = "user";

  public static final String FIELD_LOGIN = "login";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_UPDATED_AT = "updatedAt";
  public static final String FIELD_ACTIVE = "active";
  public static final String FIELD_SCM_ACCOUNTS = "scmAccounts";

  private final Settings settings;

  public UserIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.setShards(settings);

    // type "user"
    NewIndex.NewIndexType mapping = index.createType(TYPE_USER);
    mapping.setAttribute("_id", ImmutableMap.of("path", FIELD_LOGIN));
    mapping.stringFieldBuilder(FIELD_LOGIN).build();
    mapping.stringFieldBuilder(FIELD_NAME).build();
    mapping.stringFieldBuilder(FIELD_EMAIL).enableSorting().build();
    mapping.createDateTimeField(FIELD_CREATED_AT);
    mapping.createDateTimeField(FIELD_UPDATED_AT);
    mapping.createBooleanField(FIELD_ACTIVE);
    mapping.stringFieldBuilder(FIELD_SCM_ACCOUNTS).build();
  }
}

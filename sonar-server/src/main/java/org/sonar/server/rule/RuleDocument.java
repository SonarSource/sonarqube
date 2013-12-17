/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.rule;

public final class RuleDocument {

  public static final String FIELD_PARAMS = "params";
  public static final String FIELD_UPDATED_AT = "updatedAt";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_SEVERITY = "severity";
  public static final String FIELD_REPOSITORY_KEY = "repositoryKey";
  public static final String FIELD_PARENT_KEY = "parentKey";
  public static final String FIELD_DESCRIPTION = "description";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_LANGUAGE = "language";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_ID = "id";

  public static final String FIELD_PARAM_KEY = "key";
  public static final String FIELD_PARAM_TYPE = "type";
  public static final String FIELD_PARAM_DEFAULT_VALUE = "defaultValue";
  public static final String FIELD_PARAM_DESCRIPTION = "description";

  private RuleDocument() {
    // Only constants
  }

}

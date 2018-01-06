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
package org.sonar.db.dialect;

import java.util.Collections;
import java.util.List;

/**
 * @since 3.2
 */
abstract class AbstractDialect implements Dialect {
  private final String id;
  private final String defaultDriverClassName;
  private final String trueSqlValue;
  private final String falseSqlValue;
  private final String validationQuery;

  protected AbstractDialect(String id, String defaultDriverClassName, String trueSqlValue, String falseSqlValue,
    String validationQuery) {
    this.id = id;
    this.defaultDriverClassName = defaultDriverClassName;
    this.trueSqlValue = trueSqlValue;
    this.falseSqlValue = falseSqlValue;
    this.validationQuery = validationQuery;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDefaultDriverClassName() {
    return defaultDriverClassName;
  }

  @Override
  public final String getTrueSqlValue() {
    return trueSqlValue;
  }

  @Override
  public final String getFalseSqlValue() {
    return falseSqlValue;
  }

  @Override
  public final String getValidationQuery() {
    return validationQuery;
  }

  @Override
  public List<String> getConnectionInitStatements() {
    return Collections.emptyList();
  }

  @Override
  public int getScrollDefaultFetchSize() {
    return 200;
  }

  @Override
  public int getScrollSingleRowFetchSize() {
    return 1;
  }
}

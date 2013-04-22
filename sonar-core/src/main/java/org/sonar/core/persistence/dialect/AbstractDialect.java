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
package org.sonar.core.persistence.dialect;

import java.util.Collections;
import java.util.List;

/**
 * @since 3.2
 */
abstract class AbstractDialect implements Dialect {
  private final String id;
  private final String activeRecordDialectCode;
  private final String defaultDriverClassName;
  private final String trueSqlValue;
  private final String falseSqlValue;
  private final String validationQuery;

  protected AbstractDialect(String id, String activeRecordDialectCode, String defaultDriverClassName, String trueSqlValue, String falseSqlValue,
      String validationQuery) {
    this.id = id;
    this.activeRecordDialectCode = activeRecordDialectCode;
    this.defaultDriverClassName = defaultDriverClassName;
    this.trueSqlValue = trueSqlValue;
    this.falseSqlValue = falseSqlValue;
    this.validationQuery = validationQuery;
  }

  public String getId() {
    return id;
  }

  public String getActiveRecordDialectCode() {
    return activeRecordDialectCode;
  }

  public String getDefaultDriverClassName() {
    return defaultDriverClassName;
  }

  public final String getTrueSqlValue() {
    return trueSqlValue;
  }

  public final String getFalseSqlValue() {
    return falseSqlValue;
  }

  public final String getValidationQuery() {
    return validationQuery;
  }

  public List<String> getConnectionInitStatements(String schema) {
    return Collections.emptyList();
  }
}

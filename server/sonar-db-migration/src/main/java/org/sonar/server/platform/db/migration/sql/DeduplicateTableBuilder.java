/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import java.util.ArrayList;
import java.util.List;
import org.sonar.server.platform.db.migration.def.Validations;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

public class DeduplicateTableBuilder {

  private final String tableName;
  private final List<String> referenceColumns = new ArrayList<>();
  private String identityColumn;

  public DeduplicateTableBuilder(String tableName) {
    this.tableName = tableName;
  }

  public DeduplicateTableBuilder setIdentityColumn(String identityColumn) {
    this.identityColumn = identityColumn;
    return this;
  }

  public DeduplicateTableBuilder addReferenceColumn(String referenceColumn) {
    this.referenceColumns.add(referenceColumn);
    return this;
  }

  public List<String> build() {
    validateTableName(tableName);
    validateColumnName(identityColumn);
    checkArgument(!referenceColumns.isEmpty(), "At least one reference column must be specified");
    referenceColumns.forEach(Validations::validateColumnName);
    return singletonList(createSqlStatement());
  }

  private String createSqlStatement() {
    StringBuilder sql = new StringBuilder().append("delete from ").append(tableName).append(" ");
    sql.append("where ").append(identityColumn).append(" not in (select min(").append(identityColumn).append(") from ").append(tableName).append(" ");
    sql.append("group by ").append(String.join(", ", referenceColumns)).append(")");
    return sql.toString();
  }
}

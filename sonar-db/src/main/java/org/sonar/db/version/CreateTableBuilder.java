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
package org.sonar.db.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import org.sonar.db.dialect.Dialect;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CreateTableBuilder {
  private final Dialect dialect;
  private final String tableName;
  private final List<ColumnDef> columnDefs = new ArrayList<>();
  private final List<ColumnDef> pkColumnDefs = new ArrayList<>(2);
  @CheckForNull
  private String pkConstraintName;

  public CreateTableBuilder(Dialect dialect, String tableName) {
    this.dialect = requireNonNull(dialect, "dialect can't be null");
    this.tableName = requireNonNull(tableName, "table name can't be null").toLowerCase(Locale.ENGLISH);
  }

  public List<String> build() {
    checkState(!columnDefs.isEmpty() || !pkColumnDefs.isEmpty(), "at least one column must be specified");

    return Collections.singletonList(createTableStatement());
  }

  public CreateTableBuilder addColumn(ColumnDef columnDef) {
    columnDefs.add(requireNonNull(columnDef, "column def can't be null"));
    return this;
  }

  public CreateTableBuilder addPkColumn(ColumnDef columnDef) {
    pkColumnDefs.add(requireNonNull(columnDef, "column def can't be null"));
    return this;
  }

  public CreateTableBuilder withPkConstraintName(String pkConstraintName) {
    this.pkConstraintName = requireNonNull(pkConstraintName, "primary key constraint name can't be null");
    return this;
  }

  private String createTableStatement() {
    StringBuilder res = new StringBuilder("CREATE TABLE ");
    res.append(tableName);
    res.append(" (");
    addPkColumns(res);
    addColumns(res, dialect, columnDefs);
    addPkConstraint(res);
    res.append(")");
    return res.toString();
  }

  private void addPkColumns(StringBuilder res) {
    addColumns(res, dialect, pkColumnDefs);
    if (!pkColumnDefs.isEmpty() && !columnDefs.isEmpty()) {
      res.append(',');
    }
  }

  private static void addColumns(StringBuilder res, Dialect dialect, List<ColumnDef> columnDefs) {
    if (columnDefs.isEmpty()) {
      return;
    }
    Iterator<ColumnDef> columnDefIterator = columnDefs.iterator();
    while (columnDefIterator.hasNext()) {
      ColumnDef columnDef = columnDefIterator.next();
      res.append(columnDef.getName()).append(' ').append(columnDef.generateSqlType(dialect));
      addNullConstraint(res, columnDef);
      if (columnDefIterator.hasNext()) {
        res.append(',');
      }
    }
  }

  private static void addNullConstraint(StringBuilder res, ColumnDef columnDef) {
    if (columnDef.isNullable()) {
      res.append(" NULL");
    } else {
      res.append(" NOT NULL");
    }
  }

  private void addPkConstraint(StringBuilder res) {
    if (pkColumnDefs.isEmpty()) {
      return;
    }
    res.append(", ");
    res.append("CONSTRAINT ");
    addPkConstraintName(res);
    res.append(" PRIMARY KEY ");
    res.append('(');
    appendColumnNames(res, pkColumnDefs);
    res.append(')');
  }

  private void addPkConstraintName(StringBuilder res) {
    if (pkConstraintName == null) {
      res.append("pk_").append(tableName);
    } else {
      res.append(pkConstraintName.toLowerCase(Locale.ENGLISH));
    }
  }

  private static void appendColumnNames(StringBuilder res, List<ColumnDef> columnDefs) {
    Iterator<ColumnDef> columnDefIterator = columnDefs.iterator();
    while (columnDefIterator.hasNext()) {
      res.append(columnDefIterator.next().getName());
      if (columnDefIterator.hasNext()) {
        res.append(',');
      }
    }
  }

}

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
package org.sonar.server.platform.db.migration.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.of;
import static org.sonar.server.platform.db.migration.def.Validations.validateConstraintName;
import static org.sonar.server.platform.db.migration.def.Validations.validateTableName;

public class CreateTableBuilder {

  private final Dialect dialect;
  private final String tableName;
  private final List<ColumnDef> columnDefs = new ArrayList<>();
  private final List<ColumnDef> pkColumnDefs = new ArrayList<>(2);
  private final Multimap<ColumnDef, ColumnFlag> flagsByColumn = HashMultimap.create(1, 1);
  @CheckForNull
  private String pkConstraintName;

  public CreateTableBuilder(Dialect dialect, String tableName) {
    this.dialect = requireNonNull(dialect, "dialect can't be null");
    this.tableName = validateTableName(tableName);
  }

  public boolean tableExists(Database database) throws SQLException {
    try (Connection connection = database.getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(tableName, connection);
    }
  }

  public List<String> build() {
    checkState(!columnDefs.isEmpty() || !pkColumnDefs.isEmpty(), "at least one column must be specified");

    return Stream.concat(of(createTableStatement()), createOracleAutoIncrementStatements())
      .collect(MoreCollectors.toList());
  }

  public CreateTableBuilder addColumn(ColumnDef columnDef) {
    columnDefs.add(requireNonNull(columnDef, "column def can't be null"));
    return this;
  }

  public CreateTableBuilder addPkColumn(ColumnDef columnDef, ColumnFlag... flags) {
    pkColumnDefs.add(requireNonNull(columnDef, "column def can't be null"));
    addFlags(columnDef, flags);
    return this;
  }

  private void addFlags(ColumnDef columnDef, ColumnFlag[] flags) {
    Arrays.stream(flags)
      .forEach(flag -> {
        requireNonNull(flag, "flag can't be null");
        if (flag == ColumnFlag.AUTO_INCREMENT) {
          validateColumnDefForAutoIncrement(columnDef);
        }
        flagsByColumn.put(columnDef, flag);
      });
  }

  private void validateColumnDefForAutoIncrement(ColumnDef columnDef) {
    checkArgument("id".equals(columnDef.getName()),
      "Auto increment column name must be id");
    checkArgument(columnDef instanceof BigIntegerColumnDef
      || columnDef instanceof IntegerColumnDef,
      "Auto increment column must either be BigInteger or Integer");
    checkArgument(!columnDef.isNullable(),
      "Auto increment column can't be nullable");
    checkState(pkColumnDefs.stream().filter(this::isAutoIncrement).count() == 0,
      "There can't be more than one auto increment column");
  }

  public CreateTableBuilder withPkConstraintName(String pkConstraintName) {
    this.pkConstraintName = validateConstraintName(pkConstraintName);
    return this;
  }

  private String createTableStatement() {
    StringBuilder res = new StringBuilder("CREATE TABLE ");
    res.append(tableName);
    res.append(" (");
    appendPkColumns(res);
    appendColumns(res, dialect, columnDefs);
    appendPkConstraint(res);
    res.append(')');
    return res.toString();
  }

  private void appendPkColumns(StringBuilder res) {
    appendColumns(res, dialect, pkColumnDefs);
    if (!pkColumnDefs.isEmpty() && !columnDefs.isEmpty()) {
      res.append(',');
    }
  }

  private void appendColumns(StringBuilder res, Dialect dialect, List<ColumnDef> columnDefs) {
    if (columnDefs.isEmpty()) {
      return;
    }
    Iterator<ColumnDef> columnDefIterator = columnDefs.iterator();
    while (columnDefIterator.hasNext()) {
      ColumnDef columnDef = columnDefIterator.next();
      res.append(columnDef.getName());
      res.append(' ');
      appendDataType(res, dialect, columnDef);
      appendDefaultValue(res, columnDef);
      appendNullConstraint(res, columnDef);
      appendColumnFlags(res, dialect, columnDef);
      if (columnDefIterator.hasNext()) {
        res.append(',');
      }
    }
  }

  private void appendDataType(StringBuilder res, Dialect dialect, ColumnDef columnDef) {
    if (PostgreSql.ID.equals(dialect.getId()) && isAutoIncrement(columnDef)) {
      if (columnDef instanceof BigIntegerColumnDef) {
        res.append("BIGSERIAL");
      } else if (columnDef instanceof IntegerColumnDef) {
        res.append("SERIAL");
      } else {
        throw new IllegalStateException("Column with autoincrement is neither BigInteger nor Integer");
      }
    } else {
      res.append(columnDef.generateSqlType(dialect));
    }
  }

  private boolean isAutoIncrement(ColumnDef columnDef) {
    Collection<ColumnFlag> columnFlags = this.flagsByColumn.get(columnDef);
    return columnFlags != null && columnFlags.contains(ColumnFlag.AUTO_INCREMENT);
  }

  private static void appendNullConstraint(StringBuilder res, ColumnDef columnDef) {
    if (columnDef.isNullable()) {
      res.append(" NULL");
    } else {
      res.append(" NOT NULL");
    }
  }

  private void appendDefaultValue(StringBuilder sql, ColumnDef columnDef) {
    Object defaultValue = columnDef.getDefaultValue();
    if (defaultValue != null) {
      sql.append(" DEFAULT ");

      if (defaultValue instanceof String) {
        sql.append(format("'%s'", defaultValue));
      } else if (defaultValue instanceof Boolean) {
        sql.append((boolean) defaultValue ? dialect.getTrueSqlValue() : dialect.getFalseSqlValue());
      } else {
        sql.append(defaultValue);
      }
    }
  }

  private void appendColumnFlags(StringBuilder res, Dialect dialect, ColumnDef columnDef) {
    Collection<ColumnFlag> columnFlags = this.flagsByColumn.get(columnDef);
    if (columnFlags != null && columnFlags.contains(ColumnFlag.AUTO_INCREMENT)) {
      switch (dialect.getId()) {
        case Oracle.ID:
          // no auto increment on Oracle, must use a sequence
          break;
        case PostgreSql.ID:
          // no specific clause on PostgreSQL but a specific type
          break;
        case MsSql.ID:
          res.append(" IDENTITY (1,1)");
          break;
        case H2.ID:
          res.append(" AUTO_INCREMENT (1,1)");
          break;
        default:
          throw new IllegalArgumentException("Unsupported dialect id " + dialect.getId());
      }
    }
  }

  private void appendPkConstraint(StringBuilder res) {
    if (pkColumnDefs.isEmpty()) {
      return;
    }
    res.append(", ");
    res.append("CONSTRAINT ");
    appendPkConstraintName(res);
    res.append(" PRIMARY KEY ");
    res.append('(');
    appendColumnNames(res, pkColumnDefs);
    res.append(')');
  }

  private void appendPkConstraintName(StringBuilder res) {
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

  private Stream<String> createOracleAutoIncrementStatements() {
    if (!Oracle.ID.equals(dialect.getId())) {
      return Stream.empty();
    }
    return pkColumnDefs.stream()
      .filter(this::isAutoIncrement)
      .flatMap(columnDef -> of(createSequenceFor(tableName), createOracleTriggerForTable(tableName)));
  }

  private static String createSequenceFor(String tableName) {
    return "CREATE SEQUENCE " + tableName + "_seq START WITH 1 INCREMENT BY 1";
  }

  static String createOracleTriggerForTable(String tableName) {
    return "CREATE OR REPLACE TRIGGER " + tableName + "_idt" +
      " BEFORE INSERT ON " + tableName +
      " FOR EACH ROW" +
      " BEGIN" +
      " IF :new.id IS null THEN" +
      " SELECT " + tableName + "_seq.nextval INTO :new.id FROM dual;" +
      " END IF;" +
      " END;";
  }

  public enum ColumnFlag {
    AUTO_INCREMENT
  }

}

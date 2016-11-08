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

import java.util.List;
import java.util.stream.Stream;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.Oracle;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.of;
import static org.sonar.db.version.Validations.TABLE_NAME_MAX_SIZE;
import static org.sonar.db.version.Validations.checkDbIdentifier;

public class DropTableBuilder {

  private final Dialect dialect;
  private final String tableName;

  public DropTableBuilder(Dialect dialect, String tableName) {
    this.dialect = requireNonNull(dialect, "dialect can't be null");
    this.tableName = checkDbIdentifier(tableName, "Table name", TABLE_NAME_MAX_SIZE);
  }

  public List<String> build() {
    return Stream.concat(dropOracleSequenceAndTriggerStatements(), of(dropTableStatement())).collect(Collectors.toList());
  }

  private String dropTableStatement() {
    return "DROP TABLE " + tableName;
  }

  private Stream<String> dropOracleSequenceAndTriggerStatements() {
    if (!Oracle.ID.equals(dialect.getId())) {
      return Stream.empty();
    }
    return Stream.of(dropSequenceFor(tableName), dropTriggerFor(tableName));
  }

  private static String dropSequenceFor(String tableName) {
    return "DROP SEQUENCE " + tableName + "_seq";
  }

  private static String dropTriggerFor(String tableName) {
    return "DROP TRIGGER " + tableName + "_idt";
  }

}

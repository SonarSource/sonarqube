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
package org.sonar.core.persistence.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.type.Type;

import java.util.Properties;

/**
 * if the underlying database is PostgreSQL, the sequence
 * naming convention is different and includes the primary key
 * column name
 *
 * @since 1.10
 */
public class PostgreSQLSequenceGenerator extends SequenceGenerator {

  public static final String SEQUENCE_NAME_SEPARATOR = "_";
  public static final String SEQUENCE_NAME_SUFFIX = "seq";

  @Override
  public void configure(Type type, Properties params, Dialect dialect) {

    String tableName = params.getProperty(PersistentIdentifierGenerator.TABLE);
    String columnName = params.getProperty(PersistentIdentifierGenerator.PK);

    if (tableName != null && columnName != null) {
      StringBuilder sequenceNameBuilder = new StringBuilder();

      sequenceNameBuilder.append(tableName);
      sequenceNameBuilder.append(SEQUENCE_NAME_SEPARATOR);
      sequenceNameBuilder.append(columnName);
      sequenceNameBuilder.append(SEQUENCE_NAME_SEPARATOR);
      sequenceNameBuilder.append(SEQUENCE_NAME_SUFFIX);

      params.setProperty(SEQUENCE, sequenceNameBuilder.toString());
    }

    super.configure(type, params, dialect);
  }

}

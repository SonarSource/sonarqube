/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.dialect;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.type.Type;

import java.util.Properties;

/**
 * @since 1.10
 */
public class OracleSequenceGenerator extends SequenceGenerator {

  public static final String SEQUENCE_NAME_SUFFIX = "_SEQ";

  @Override
  public void configure(Type type, Properties params, Dialect dialect)
      throws MappingException {

    String tableName = params.getProperty(PersistentIdentifierGenerator.TABLE);

    if (tableName != null) {
      StringBuilder sequenceNameBuilder = new StringBuilder();

      sequenceNameBuilder.append(tableName);
      sequenceNameBuilder.append(SEQUENCE_NAME_SUFFIX);

      params.setProperty(SEQUENCE, sequenceNameBuilder.toString().toUpperCase());
    }

    super.configure(type, params, dialect);
  }

}

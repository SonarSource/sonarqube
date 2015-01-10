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

import org.hibernate.id.PersistentIdentifierGenerator;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgreSQLSequenceGeneratorTest {

  @Test
  public void sequenceNameShouldFollowRailsConventions() {
    Properties props = new Properties();
    props.setProperty(PersistentIdentifierGenerator.TABLE, "my_table");
    props.setProperty(PersistentIdentifierGenerator.PK, "id");

    PostgreSQLSequenceGenerator generator = new PostgreSQLSequenceGenerator();
    generator.configure(null, props, new PostgreSql.PostgreSQLWithDecimalDialect());
    assertThat(generator.getSequenceName()).isEqualTo("my_table_id_seq");
  }

  @Test
  public void should_not_fail_if_table_name_can_not_be_loaded() {
    Properties props = new Properties();
    PostgreSQLSequenceGenerator generator = new PostgreSQLSequenceGenerator();
    generator.configure(null, props, new PostgreSql.PostgreSQLWithDecimalDialect());
    assertThat(generator.getSequenceName()).isNotEmpty();
  }
}

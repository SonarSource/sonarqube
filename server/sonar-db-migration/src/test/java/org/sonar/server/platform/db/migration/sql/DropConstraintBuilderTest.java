/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;

public class DropConstraintBuilderTest {
  @Test
  public void fail_if_constraint_name_starts_with_pk() {
    DropConstraintBuilder builder = new DropConstraintBuilder(new Oracle());
    Assert.assertThrows("This builder should not be used with primary keys", IllegalArgumentException.class, () -> builder.setName("pk_constraint"));
  }

  @Test
  public void succeeds_for_oracle() {
    DropConstraintBuilder builder = new DropConstraintBuilder(new Oracle());
    List<String> queries = builder.setName("constraint1").setTable("table1").build();
    assertThat(queries).containsOnly("ALTER TABLE table1 DROP CONSTRAINT constraint1");
  }
}

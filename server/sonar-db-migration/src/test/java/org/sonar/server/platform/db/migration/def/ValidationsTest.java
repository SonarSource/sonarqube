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
package org.sonar.server.platform.db.migration.def;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.def.Validations.validateColumnName;
import static org.sonar.server.platform.db.migration.def.Validations.validateIndexName;

public class ValidationsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void accept_valid_table_name() {
    validateColumnName("date_in_ms");
    validateColumnName("date_in_ms_1");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    validateColumnName(null);
  }

  @Test
  public void fail_when_column_name_is_in_upper_case() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name must be lower case and contain only alphanumeric chars or '_', got 'DATE_IN_MS'");

    validateColumnName("DATE_IN_MS");
  }

  @Test
  public void fail_when_column_name_contains_invalid_character() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name must be lower case and contain only alphanumeric chars or '_', got 'date-in/ms'");

    validateColumnName("date-in/ms");
  }

  @Test
  public void validateIndexName_throws_IAE_when_index_name_contains_invalid_characters() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Index name must be lower case and contain only alphanumeric chars or '_', got '(not/valid)'");

    validateIndexName("(not/valid)");
  }

  @Test
  public void validateIndexName_throws_NPE_when_index_name_is_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Index name cannot be null");

    validateIndexName(null);
  }

  @Test
  public void validateIndexName_returns_valid_name() {
    assertThat(validateIndexName("foo")).isEqualTo("foo");
  }
}

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

package org.sonar.db.version;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.sonar.db.version.ColumnDefValidation.validateColumnName;

public class ColumnDefValidationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void accept_valid_table_name() throws Exception {
    validateColumnName("date_in_ms");
    validateColumnName("date_in_ms_1");
  }

  @Test
  public void fail_with_NPE_if_name_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Column name cannot be null");

    validateColumnName(null);
  }

  @Test
  public void fail_when_column_name_is_in_upper_case() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name should only contains lowercase and _ characters, got 'DATE_IN_MS'");

    validateColumnName("DATE_IN_MS");
  }

  @Test
  public void fail_when_column_name_contains_invalid_character() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Column name should only contains lowercase and _ characters, got 'date-in/ms'");

    validateColumnName("date-in/ms");
  }

}

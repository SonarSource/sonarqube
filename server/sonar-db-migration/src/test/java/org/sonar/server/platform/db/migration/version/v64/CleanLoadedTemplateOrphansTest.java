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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanLoadedTemplateOrphansTest {

  private static final String TABLE_LOADED_TEMPLATES = "loaded_templates";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanLoadedTemplateOrphansTest.class, "loaded-templates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CleanLoadedTemplateOrphans underTest = new CleanLoadedTemplateOrphans(db.database());

  @Test
  public void execute_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_deletes_rows_with_null_in_template_type_column() throws SQLException {
    insertLoadedTemplate(null, "value1");
    insertLoadedTemplate("", "value2");
    insertLoadedTemplate("non_empty", "value3");

    underTest.execute();

    assertThat(db.select("select kee as \"value\" from loaded_templates"))
      .extracting(s -> s.get("value"))
      .containsOnly("value2", "value3");
  }

  private void insertLoadedTemplate(@Nullable String type, String key) {
    db.executeInsert(
      TABLE_LOADED_TEMPLATES,
      "TEMPLATE_TYPE", type,
      "KEE", key);
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class ExtendLoadedTemplateTypeColumnTest {

  private static final String TABLE_LOADED_TEMPLATES = "loaded_templates";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(ExtendLoadedTemplateTypeColumnTest.class, "loaded-templates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExtendLoadedTemplateTypeColumn underTest = new ExtendLoadedTemplateTypeColumn(db.database());

  @Test
  public void extend_and_make_non_nullable_column_template_type() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_makes_analysis_uuid_not_nullable_on_populated_table() throws SQLException {
    insertLoadedTemplate("type1", "val1");
    insertLoadedTemplate("type2", "val2");

    underTest.execute();

    verifyColumnDefinitions();
  }

  @Test
  public void migration_fails_if_type_column_has_null_values() throws SQLException {
    insertLoadedTemplate(null, "some value");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void insertLoadedTemplate(String type, String key) {
    db.executeInsert(
      TABLE_LOADED_TEMPLATES,
      "TEMPLATE_TYPE", type,
      "KEE", key);
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition(TABLE_LOADED_TEMPLATES, "template_type", Types.VARCHAR, 64, false);
  }

}

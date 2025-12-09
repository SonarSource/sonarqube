/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202505;

import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

class PopulateAnnouncementHtmlMessagePropertyTest {
  private static final String SOURCE_PROPERTY = "sonar.announcement.message";
  private static final String TARGET_PROPERTY = "sonar.announcement.htmlMessage";
  private static final long NOW = 1_000_000_000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateAnnouncementHtmlMessageProperty.class);

  private final DataChange underTest = new PopulateAnnouncementHtmlMessageProperty(db.database(), system2, new SequenceUuidFactory());

  @Test
  void execute_shouldPopulateHtmlMessage_whenSourcePropertyExists() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "This is a *bold* announcement", 100_000L);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("This is a <strong>bold</strong> announcement");

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + SOURCE_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("This is a *bold* announcement");
  }

  @Test
  void execute_shouldNotPopulateHtmlMessage_whenSourcePropertyIsEmpty() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "", 100_000L);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .isEmpty();
  }

  @Test
  void execute_shouldNotPopulateHtmlMessage_whenSourcePropertyDoesNotExist() throws SQLException {
    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .isEmpty();
  }

  @Test
  void execute_shouldNotOverwriteHtmlMessage_whenTargetPropertyAlreadyHasValue() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "New announcement", 100_000L);
    insertProperty(TARGET_PROPERTY, "Existing <em>HTML</em> value", 200_000L);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("Existing <em>HTML</em> value");
  }

  @Test
  void execute_shouldPopulateHtmlMessage_whenTargetPropertyExistsButIsEmpty() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "Announcement with *emphasis*", 100_000L);
    insertProperty(TARGET_PROPERTY, "", 200_000L);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("Announcement with <strong>emphasis</strong>");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "System maintenance tonight", 100_000L);

    underTest.execute();
    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .hasSize(1);
  }

  @Test
  void execute_shouldConvertMarkdownToHtml() throws SQLException {
    insertProperty(SOURCE_PROPERTY, "= Header\n\n* Item 1\n* Item 2", 100_000L);

    underTest.execute();

    Assertions.assertThat(db.select("select text_value from properties where prop_key = '" + TARGET_PROPERTY + "'"))
      .hasSize(1)
      .extracting(m -> m.get("text_value"))
      .containsExactly("<h1>Header</h1><ul><li>Item 1</li>\n<li>Item 2</li></ul>");
  }

  private void insertProperty(String key, String value, long createdAt) {
    db.executeInsert("properties",
      "prop_key", key,
      "is_empty", value.isEmpty(),
      "text_value", value,
      "created_at", createdAt,
      "uuid", "uuid-" + key);
  }
}

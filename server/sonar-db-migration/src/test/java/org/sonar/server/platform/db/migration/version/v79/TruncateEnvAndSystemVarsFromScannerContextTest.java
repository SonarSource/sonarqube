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
package org.sonar.server.platform.db.migration.version.v79;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class TruncateEnvAndSystemVarsFromScannerContextTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(TruncateEnvAndSystemVarsFromScannerContextTest.class, "ce_scanner_context.sql");

  private TruncateEnvAndSystemVarsFromScannerContext underTest = new TruncateEnvAndSystemVarsFromScannerContext(db.database());

  private static final String ENTRY_WITH_ENV_AND_SYSTEM_VARS = "Environment variables:\n" +
    "  - PATH=blablahblah\n" +
    "System properties:\n" +
    "  - java.class.version=55.0\n" +
    "\n" +
    "  - user.language=en\n" +
    "SonarQube plugins:\n" +
    "  - SonarCSS 1.0.3.724 (cssfamily)\n" +
    "Global server settings:\n" +
    "  - sonar.core.startTime=2019-06-18T12:53:09+0200\n" +
    "Project server settings:\n" +
    "Project scanner properties:\n" +
    "  - sonar.verbose=true";

  private static final String TRUNCATED_ENTRY = "SonarQube plugins:\n" +
    "  - SonarCSS 1.0.3.724 (cssfamily)\n" +
    "Global server settings:\n" +
    "  - sonar.core.startTime=2019-06-18T12:53:09+0200\n" +
    "Project server settings:\n" +
    "Project scanner properties:\n" +
    "  - sonar.verbose=true";

  @Test
  public void execute() throws SQLException {
    insert_entry("887f07ff-91bb-4e62-8e4b-660d5b40b16a", ENTRY_WITH_ENV_AND_SYSTEM_VARS);
    insert_entry("9d2429cc-155a-4327-8ed7-48fdbe93edd0", TRUNCATED_ENTRY);

    underTest.execute();

    Map<String, Object> result = db.select("select TASK_UUID, CONTEXT_DATA from CE_SCANNER_CONTEXT")
      .stream()
      .collect(HashMap::new, (m, v) -> m.put((String) v.get("TASK_UUID"), readBlob((Blob) v.get("CONTEXT_DATA"))), HashMap::putAll);
    assertThat(result).hasSize(2);
    assertThat(result.get("887f07ff-91bb-4e62-8e4b-660d5b40b16a")).isEqualTo(TRUNCATED_ENTRY);
    assertThat(result.get("9d2429cc-155a-4327-8ed7-48fdbe93edd0")).isEqualTo(TRUNCATED_ENTRY);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insert_entry("887f07ff-91bb-4e62-8e4b-660d5b40b16a", ENTRY_WITH_ENV_AND_SYSTEM_VARS);
    insert_entry("9d2429cc-155a-4327-8ed7-48fdbe93edd0", TRUNCATED_ENTRY);

    underTest.execute();
    underTest.execute();

    Map<String, Object> result = db.select("select TASK_UUID, CONTEXT_DATA from CE_SCANNER_CONTEXT")
      .stream()
      .collect(HashMap::new, (m, v) -> m.put((String) v.get("TASK_UUID"), readBlob((Blob) v.get("CONTEXT_DATA"))), HashMap::putAll);
    assertThat(result).hasSize(2);
    assertThat(result.get("887f07ff-91bb-4e62-8e4b-660d5b40b16a")).isEqualTo(TRUNCATED_ENTRY);
    assertThat(result.get("9d2429cc-155a-4327-8ed7-48fdbe93edd0")).isEqualTo(TRUNCATED_ENTRY);
  }

  private String readBlob(Blob blob) throws IllegalStateException {
    try {
      return new String(blob.getBytes(1, (int) blob.length()), StandardCharsets.UTF_8);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private void insert_entry(String uuid, String content) {
    db.executeInsert(
      "CE_SCANNER_CONTEXT",
      "TASK_UUID", uuid,
      "CONTEXT_DATA", content.getBytes(StandardCharsets.UTF_8),
      "CREATED_AT", new Date().getTime(),
      "UPDATED_AT", new Date().getTime());
  }
}

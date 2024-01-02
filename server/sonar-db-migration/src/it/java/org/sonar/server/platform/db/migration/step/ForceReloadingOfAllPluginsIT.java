/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.step.ForceReloadingOfAllPlugins.OVERWRITE_HASH;

public class ForceReloadingOfAllPluginsIT {
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(ForceReloadingOfAllPluginsIT.class, "schema.sql");

  private final DataChange underTest = new ForceReloadingOfAllPlugins(db.database());

  @Test
  public void migration_overwrite_file_hash_on_all_plugins() throws SQLException {
    String pluginUuid1 = insertPlugin();
    String pluginUuid2 = insertPlugin();

    underTest.execute();

    assertPluginFileHashOverwrite(pluginUuid1);
    assertPluginFileHashOverwrite(pluginUuid2);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String pluginUuid1 = insertPlugin();
    String pluginUuid2 = insertPlugin();

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertPluginFileHashOverwrite(pluginUuid1);
    assertPluginFileHashOverwrite(pluginUuid2);
  }

  private void assertPluginFileHashOverwrite(String pluginUuid) {
    String selectSql = String.format("select file_hash from plugins where uuid='%s'", pluginUuid);
    var selectResult = db.select(selectSql);
    assertThat(selectResult.get(0)).containsEntry("FILE_HASH", OVERWRITE_HASH);
  }

  private String insertPlugin() {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("KEE", randomAlphabetic(20));
    map.put("FILE_HASH", randomAlphabetic(32));
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("UPDATED_AT", System.currentTimeMillis());
    map.put("TYPE", "EXTERNAL");
    map.put("REMOVED", false);
    db.executeInsert("plugins", map);

    return uuid;
  }

}

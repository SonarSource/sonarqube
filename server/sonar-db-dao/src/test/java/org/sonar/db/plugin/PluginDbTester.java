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
package org.sonar.db.plugin;

import java.util.Arrays;
import java.util.function.Consumer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

public class PluginDbTester {
  private final DbClient dbClient;
  private final DbSession dbSession;

  public PluginDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  @SafeVarargs
  public final PluginDto insertPlugin(Consumer<PluginDto>... consumers) {
    PluginDto pluginDto = PluginTesting.newPluginDto();
    Arrays.stream(consumers).forEach(c -> c.accept(pluginDto));
    dbClient.pluginDao().insert(dbSession, pluginDto);
    dbSession.commit();
    return pluginDto;
  }

}

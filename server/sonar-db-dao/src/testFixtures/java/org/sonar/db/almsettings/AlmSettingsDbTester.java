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
package org.sonar.db.almsettings;

import java.util.function.Consumer;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.util.Arrays.stream;
import static org.sonar.db.almsettings.AlmSettingsTesting.newAzureAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class AlmSettingsDbTester {

  private final DbTester db;

  public AlmSettingsDbTester(DbTester db) {
    this.db = db;
  }

  @SafeVarargs
  public final AlmSettingDto insertGitHubAlmSetting(Consumer<AlmSettingDto>... populators) {
    AlmSettingDto dto = newGithubAlmSettingDto();
    stream(populators).forEach(p -> p.accept(dto));

    db.getDbClient().almSettingDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @SafeVarargs
  public final AlmSettingDto insertAzureAlmSetting(Consumer<AlmSettingDto>... populators) {
    AlmSettingDto dto = newAzureAlmSettingDto();
    stream(populators).forEach(p -> p.accept(dto));

    db.getDbClient().almSettingDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

}

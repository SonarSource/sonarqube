/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.setting.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;

public class ScannerSettingsTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private PropertyDefinitions definitions = new PropertyDefinitions();

  private ScannerSettings underTest = new ScannerSettings(db.getDbClient(), definitions);

  @Test
  public void return_license_keys() throws Exception {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("myplugin.license.secured").type(LICENSE).build()));

    assertThat(underTest.getScannerSettingKeys(db.getSession())).contains("myplugin.license.secured");
  }

  @Test
  public void return_license_hash_keys() throws Exception {
    db.properties().insertProperty(newGlobalPropertyDto("sonar.myplugin.licenseHash.secured", "hash"));

    assertThat(underTest.getScannerSettingKeys(db.getSession())).contains("sonar.myplugin.licenseHash.secured");
  }

  @Test
  public void return_server_settings() throws Exception {
    definitions.addComponents(asList(
      PropertyDefinition.builder("foo").build(),
      PropertyDefinition.builder("myplugin.license.secured").type(LICENSE).build()));

    assertThat(underTest.getScannerSettingKeys(db.getSession())).contains("sonar.server_id", "sonar.core.id", "sonar.core.startTime");
  }
}

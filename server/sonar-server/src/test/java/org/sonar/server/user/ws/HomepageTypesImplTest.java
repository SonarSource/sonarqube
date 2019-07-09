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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbTester;
import org.sonar.server.organization.TestOrganizationFlags;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.server.user.ws.HomepageTypes.Type.ISSUES;
import static org.sonar.server.user.ws.HomepageTypes.Type.MY_ISSUES;
import static org.sonar.server.user.ws.HomepageTypes.Type.MY_PROJECTS;
import static org.sonar.server.user.ws.HomepageTypes.Type.ORGANIZATION;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECT;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECTS;

public class HomepageTypesImplTest {

  @Rule
  public DbTester db = DbTester.create();

  private MapSettings settings = new MapSettings();
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();

  private HomepageTypesImpl underTest = new HomepageTypesImpl(settings.asConfig(), organizationFlags, db.getDbClient());

  @Test
  public void types_on_sonarcloud_and_organization_disabled() {
    settings.setProperty("sonar.sonarcloud.enabled", true);
    organizationFlags.setEnabled(false);

    underTest.start();

    assertThat(underTest.getTypes()).containsExactlyInAnyOrder(PROJECT, MY_PROJECTS, MY_ISSUES);
  }

  @Test
  public void types_on_sonarcloud_and_organization_enabled() {
    settings.setProperty("sonar.sonarcloud.enabled", true);
    organizationFlags.setEnabled(true);

    underTest.start();

    assertThat(underTest.getTypes()).containsExactlyInAnyOrder(PROJECT, MY_PROJECTS, MY_ISSUES, ORGANIZATION);
  }

  @Test
  public void types_on_sonarqube_and_organization_disabled() {
    settings.setProperty("sonar.sonarcloud.enabled", false);
    organizationFlags.setEnabled(false);

    underTest.start();

    assertThat(underTest.getTypes()).containsExactlyInAnyOrder(PROJECT, PROJECTS, ISSUES);
  }

  @Test
  public void types_on_sonarqube_and_organization_enabled() {
    settings.setProperty("sonar.sonarcloud.enabled", false);
    organizationFlags.setEnabled(true);

    underTest.start();

    assertThat(underTest.getTypes()).containsExactlyInAnyOrder(PROJECT, PROJECTS, ISSUES, ORGANIZATION);
  }

  @Test
  public void default_type_on_sonarcloud() {
    settings.setProperty("sonar.sonarcloud.enabled", true);

    underTest.start();

    assertThat(underTest.getDefaultType()).isEqualTo(MY_PROJECTS);
  }

  @Test
  public void default_type_on_sonarqube() {
    settings.setProperty("sonar.sonarcloud.enabled", false);

    underTest.start();

    assertThat(underTest.getDefaultType()).isEqualTo(PROJECTS);
  }

}

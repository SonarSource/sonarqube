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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderImpl;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.TestSettingsRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAssigneeTest {

  public static final String PROJECT_KEY = "PROJECT_KEY";
  public static final String ORGANIZATION_UUID = "ORGANIZATION_UUID";
  public static final String QUALITY_GATE_UUID = "QUALITY_GATE_UUID";

  @Rule
  public DbTester db = DbTester.create();

  private MapSettings settings = new MapSettings();
  private ConfigurationRepository settingsRepository = new TestSettingsRepository(settings.asConfig());
  private AnalysisMetadataHolderImpl analysisMetadataHolder = new AnalysisMetadataHolderImpl();
  private OrganizationDto organizationDto;

  private DefaultAssignee underTest = new DefaultAssignee(db.getDbClient(), settingsRepository, analysisMetadataHolder);

  @Before
  public void setUp() throws Exception {
    organizationDto = db.organizations().insertForUuid(ORGANIZATION_UUID);
    analysisMetadataHolder.setOrganization(Organization.from(
      new OrganizationDto().setUuid(ORGANIZATION_UUID).setKey("Organization key").setName("Organization name").setDefaultQualityGateUuid(QUALITY_GATE_UUID)));
  }

  @Test
  public void no_default_assignee() {
    assertThat(underTest.loadDefaultAssigneeLogin()).isNull();
  }

  @Test
  public void set_default_assignee() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    UserDto userDto = db.users().insertUser("erik");
    db.organizations().addMember(organizationDto, userDto);

    assertThat(underTest.loadDefaultAssigneeLogin()).isEqualTo("erik");
  }

  @Test
  public void configured_login_does_not_exist() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");

    assertThat(underTest.loadDefaultAssigneeLogin()).isNull();
  }

  @Test
  public void configured_login_is_disabled() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    db.users().insertUser(user -> user.setLogin("erik").setActive(false));

    assertThat(underTest.loadDefaultAssigneeLogin()).isNull();
  }

  @Test
  public void configured_login_is_not_member_of_organization() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    OrganizationDto otherOrganization = db.organizations().insert();
    UserDto userDto = db.users().insertUser("erik");
    db.organizations().addMember(otherOrganization, userDto);

    assertThat(underTest.loadDefaultAssigneeLogin()).isNull();
  }

  @Test
  public void default_assignee_is_cached() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    UserDto userDto = db.users().insertUser("erik");
    db.organizations().addMember(organizationDto, userDto);
    assertThat(underTest.loadDefaultAssigneeLogin()).isEqualTo("erik");

    // The setting is updated but the assignee hasn't changed
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "other");
    assertThat(underTest.loadDefaultAssigneeLogin()).isEqualTo("erik");
  }
}

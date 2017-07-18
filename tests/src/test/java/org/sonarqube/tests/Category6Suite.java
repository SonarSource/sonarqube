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
package org.sonarqube.tests;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.authorisation.PermissionTemplateTest;
import org.sonarqube.tests.issue.IssueTagsTest;
import org.sonarqube.tests.issue.OrganizationIssueAssignTest;
import org.sonarqube.tests.organization.BillingTest;
import org.sonarqube.tests.organization.OrganizationMembershipTest;
import org.sonarqube.tests.organization.OrganizationMembershipUiTest;
import org.sonarqube.tests.organization.OrganizationTest;
import org.sonarqube.tests.organization.PersonalOrganizationTest;
import org.sonarqube.tests.organization.RootUserOnOrganizationTest;
import org.sonarqube.tests.projectAdministration.ProjectDeletionTest;
import org.sonarqube.tests.projectAdministration.ProjectKeyUpdateTest;
import org.sonarqube.tests.projectAdministration.ProjectProvisioningTest;
import org.sonarqube.tests.projectSearch.LeakProjectsPageTest;
import org.sonarqube.tests.projectSearch.SearchProjectsTest;
import org.sonarqube.tests.qualityProfile.BuiltInQualityProfilesTest;
import org.sonarqube.tests.qualityProfile.CustomQualityProfilesTest;
import org.sonarqube.tests.qualityProfile.OrganizationQualityProfilesUiTest;
import org.sonarqube.tests.qualityProfile.QualityProfilesWsTest;
import org.sonarqube.tests.rule.RulesWsTest;
import org.sonarqube.tests.ui.OrganizationUiExtensionsTest;
import org.sonarqube.tests.user.OrganizationIdentityProviderTest;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

/**
 * This category is used only when organizations feature is activated
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  OrganizationIdentityProviderTest.class,
  OrganizationIssueAssignTest.class,
  OrganizationMembershipTest.class,
  OrganizationMembershipUiTest.class,
  OrganizationQualityProfilesUiTest.class,
  OrganizationTest.class,
  RootUserOnOrganizationTest.class,
  OrganizationUiExtensionsTest.class,
  PersonalOrganizationTest.class,
  BuiltInQualityProfilesTest.class,
  QualityProfilesWsTest.class,
  CustomQualityProfilesTest.class,
  BillingTest.class,
  IssueTagsTest.class,
  LeakProjectsPageTest.class,
  SearchProjectsTest.class,
  RulesWsTest.class,
  ProjectDeletionTest.class,
  ProjectProvisioningTest.class,
  ProjectKeyUpdateTest.class,
  PermissionTemplateTest.class
})
public class Category6Suite {

  public static final int SEARCH_HTTP_PORT = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()

    // for ES resiliency tests
    .setServerProperty("sonar.search.httpPort", "" + SEARCH_HTTP_PORT)
    .setServerProperty("sonar.search.recovery.delayInMs", "1000")
    .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")

    .addPlugin(xooPlugin())
    .addPlugin(pluginArtifact("base-auth-plugin"))
    .addPlugin(pluginArtifact("fake-billing-plugin"))
    .addPlugin(pluginArtifact("ui-extensions-plugin"))
    .build();
}

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
package it;

import com.sonar.orchestrator.Orchestrator;
import it.issue.IssueTagsTest;
import it.issue.OrganizationIssueAssignTest;
import it.organization.BillingTest;
import it.organization.OrganizationMembershipTest;
import it.organization.OrganizationMembershipUiTest;
import it.organization.OrganizationTest;
import it.organization.PersonalOrganizationTest;
import it.organization.RootUserOnOrganizationTest;
import it.projectSearch.LeakProjectsPageTest;
import it.projectSearch.SearchProjectsTest;
import it.qualityProfile.BuiltInQualityProfilesTest;
import it.qualityProfile.CustomQualityProfilesTest;
import it.qualityProfile.OrganizationQualityProfilesUiTest;
import it.uiExtension.OrganizationUiExtensionsTest;
import it.user.OrganizationIdentityProviderTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

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
  CustomQualityProfilesTest.class,
  BillingTest.class,
  IssueTagsTest.class,
  LeakProjectsPageTest.class,
  SearchProjectsTest.class
})
public class Category6Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .addPlugin(pluginArtifact("base-auth-plugin"))
    .addPlugin(pluginArtifact("fake-billing-plugin"))
    .addPlugin(pluginArtifact("ui-extensions-plugin"))
    .build();
}

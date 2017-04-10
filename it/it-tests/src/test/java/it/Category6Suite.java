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
import it.organization.IssueAssignTest;
import it.organization.OrganizationMembershipTest;
import it.organization.OrganizationQualityProfilesPageTest;
import it.organization.OrganizationTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static java.util.Collections.emptyMap;
import static util.ItUtils.xooPlugin;

/**
 * This category is used only when organizations feature is activated
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  // organization
  OrganizationTest.class,
  OrganizationMembershipTest.class,
  IssueAssignTest.class,
  OrganizationQualityProfilesPageTest.class
})
public class Category6Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .build();

  @BeforeClass
  public static void enableOrganizations() {
    enableOrganizationsSupport();
  }

  public static void enableOrganizationsSupport() {
    ORCHESTRATOR.getServer().post("api/organizations/enable_support", emptyMap());
  }
}

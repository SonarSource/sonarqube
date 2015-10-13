/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package administration.suite;

import administration.suite.administration.BulkDeletionTest;
import administration.suite.administration.ProjectAdministrationTest;
import administration.suite.administration.PropertySetsTest;
import administration.suite.administration.SubCategoriesTest;
import administration.suite.ui.I18nTest;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import util.ItUtils;

import static util.ItUtils.pluginArtifact;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BulkDeletionTest.class,
  ProjectAdministrationTest.class,
  PropertySetsTest.class,
  SubCategoriesTest.class,
  I18nTest.class
})
public class AdministrationTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(ItUtils.pluginArtifact("property-sets-plugin"))
    .addPlugin(ItUtils.pluginArtifact("sonar-subcategories-plugin"))

    // Used in I18nTest
    .addPlugin(pluginArtifact("l10n-fr-pack"))

    .addPlugin(ItUtils.xooPlugin())
    .build();
}

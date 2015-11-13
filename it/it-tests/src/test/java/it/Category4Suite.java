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
package it;

import com.sonar.orchestrator.Orchestrator;
import it.analysisExclusion.FileExclusionsTest;
import it.analysisExclusion.IssueExclusionsTest;
import it.componentSearch.ProjectSearchTest;
import it.dbCleaner.PurgeTest;
import it.duplication.CrossProjectDuplicationsOnRemoveFileTest;
import it.duplication.CrossProjectDuplicationsTest;
import it.duplication.DuplicationsTest;
import it.serverSystem.DevModeTest;
import it.serverSystem.HttpsTest;
import it.serverSystem.ServerSystemRestartingOrchestrator;
import it.serverSystem.ServerSystemTest;
import it.updateCenter.UpdateCenterTest;
import it.user.FavouriteTest;
import it.user.ForceAuthenticationTest;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  // server system
  DevModeTest.class,
  HttpsTest.class,
  ServerSystemTest.class,
  ServerSystemRestartingOrchestrator.class,
  // user
  ForceAuthenticationTest.class,
  FavouriteTest.class,
  // project search
  ProjectSearchTest.class,
  // update center
  UpdateCenterTest.class,
  // analysis exclusion
  FileExclusionsTest.class,
  IssueExclusionsTest.class,
  // duplication
  CrossProjectDuplicationsTest.class,
  CrossProjectDuplicationsOnRemoveFileTest.class,
  DuplicationsTest.class,
  // db cleaner
  PurgeTest.class
})
public class Category4Suite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .build();
}

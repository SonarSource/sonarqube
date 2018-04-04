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
package org.sonarqube.tests.performance.scanner;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.tests.performance.AbstractPerfTest;

import static util.ItUtils.newOrchestratorBuilder;
import static util.ItUtils.xooPlugin;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BootstrappingTest.class,
  DuplicationTest.class,
  FileSystemTest.class,
  HighlightingTest.class,
  IssuesModeTest.class,
  MemoryTest.class
})
public class ScannerPerformanceSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = newOrchestratorBuilder()
    .addPlugin(xooPlugin())
    // should not be so high, but required as long embedded h2 is used -> requires more memory on server
    .setServerProperty("sonar.web.javaOpts", "-Xmx1G -XX:+HeapDumpOnOutOfMemoryError")
    // Needed by DuplicationTest::hugeJavaFile
    .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "LATEST_RELEASE"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/one-xoo-issue-per-line.xml"))
    .build();

  @BeforeClass
  public static void setUp() {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    ORCHESTRATOR.executeBuild(AbstractPerfTest.newScanner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }
}

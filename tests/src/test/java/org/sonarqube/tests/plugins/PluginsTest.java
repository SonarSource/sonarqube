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
package org.sonarqube.tests.plugins;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.URLLocation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonarqube.tests.plugins.checks.AbapCheck;
import org.sonarqube.tests.plugins.checks.CCheck;
import org.sonarqube.tests.plugins.checks.Check;
import org.sonarqube.tests.plugins.checks.CobolCheck;
import org.sonarqube.tests.plugins.checks.CppCheck;
import org.sonarqube.tests.plugins.checks.FlexCheck;
import org.sonarqube.tests.plugins.checks.GoCheck;
import org.sonarqube.tests.plugins.checks.GroovyCheck;
import org.sonarqube.tests.plugins.checks.JavaCheck;
import org.sonarqube.tests.plugins.checks.JavascriptCheck;
import org.sonarqube.tests.plugins.checks.PhpCheck;
import org.sonarqube.tests.plugins.checks.PliCheck;
import org.sonarqube.tests.plugins.checks.PlsqlCheck;
import org.sonarqube.tests.plugins.checks.PythonCheck;
import org.sonarqube.tests.plugins.checks.RpgCheck;
import org.sonarqube.tests.plugins.checks.SwiftCheck;
import org.sonarqube.tests.plugins.checks.Validation;
import org.sonarqube.tests.plugins.checks.WebCheck;

import static org.assertj.core.api.Assertions.fail;
import static util.ItUtils.newOrchestratorBuilder;

/**
 * Verify that latest releases of the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  private static final List<Check> CHECKS = Arrays.asList(
    new AbapCheck(),
    new CCheck(), new CppCheck(),
    new CobolCheck(),
    new FlexCheck(),
    new GoCheck(),
    new GroovyCheck(),
    new JavaCheck(),
    new JavascriptCheck(),
    new PhpCheck(),
    new PliCheck(),
    new PlsqlCheck(),
    new PythonCheck(),
    new RpgCheck(),
    new SwiftCheck(),
    new WebCheck());

  private static Orchestrator ORCHESTRATOR;

  @BeforeClass
  public static void startServer() throws MalformedURLException {
    OrchestratorBuilder builder = newOrchestratorBuilder();

    installPlugin(builder, "com.sonarsource.abap", "sonar-abap-plugin");
    installPlugin(builder, "org.codehaus.sonar-plugins.android", "sonar-android-plugin");
    installPlugin(builder, "org.sonarsource.auth.bitbucket", "sonar-auth-bitbucket-plugin");
    installPlugin(builder, "org.sonarsource.auth.github", "sonar-auth-github-plugin");
    installPlugin(builder, "org.sonarsource.clover", "sonar-clover-plugin");
    installPlugin(builder, "com.sonarsource.cobol", "sonar-cobol-plugin");
    installPlugin(builder, "com.sonarsource.cpp", "sonar-cfamily-plugin");
    installPlugin(builder, "org.sonarsource.dotnet", "sonar-csharp-plugin");
    installPlugin(builder, "org.sonarsource.sonar-findbugs-plugin", "sonar-findbugs-plugin");
    installPlugin(builder, "org.sonarsource.flex", "sonar-flex-plugin");
    installPlugin(builder, "org.sonarsource.sonar-plugins.github", "sonar-github-plugin");
    installPlugin(builder, "org.sonarsource.go", "sonar-go-plugin");
    installPlugin(builder, "org.sonarsource.groovy", "sonar-groovy-plugin");
    installPlugin(builder, "org.sonarsource.java", "sonar-java-plugin");
    installPlugin(builder, "org.sonarsource.javascript", "sonar-javascript-plugin");
    installPlugin(builder, "org.sonarsource.ldap", "sonar-ldap-plugin");
    installPlugin(builder, "org.sonarsource.php", "sonar-php-plugin");
    installPlugin(builder, "com.sonarsource.pli", "sonar-pli-plugin");
    installPlugin(builder, "com.sonarsource.plsql", "sonar-plsql-plugin");
    installPlugin(builder, "org.sonarsource.pmd", "sonar-pmd-plugin");
    installPlugin(builder, "org.sonarsource.python", "sonar-python-plugin");
    installPlugin(builder, "com.sonarsource.rpg", "sonar-rpg-plugin");
    builder.addPlugin(URLLocation.create(new URL("https://sonarsource.bintray.com/Distribution/sonar-scm-clearcase-plugin/sonar-scm-clearcase-plugin-1.1.jar")));
    installPlugin(builder, "org.codehaus.sonar-plugins", "sonar-scm-cvs-plugin");
    installPlugin(builder, "org.sonarsource.scm.git", "sonar-scm-git-plugin");
    builder.addPlugin(URLLocation.create(new URL("http://downloads.sonarsource.com/plugins/org/codehaus/sonar-plugins/sonar-scm-jazzrtc-plugin/1.1/sonar-scm-jazzrtc-plugin-1.1.jar")));
    installPlugin(builder, "org.sonarsource.scm.mercurial", "sonar-scm-mercurial-plugin");
    installPlugin(builder, "org.sonarsource.scm.perforce", "sonar-scm-perforce-plugin");
    installPlugin(builder, "org.sonarsource.scm.svn", "sonar-scm-svn-plugin");
    installPlugin(builder, "com.sonarsource.swift", "sonar-swift-plugin");
    installPlugin(builder, "com.sonarsource.vbnet", "sonar-vbnet-plugin");
    installPlugin(builder, "org.sonarsource.web", "sonar-web-plugin");
    installPlugin(builder, "org.sonarsource.xml", "sonar-xml-plugin");
    installPlugin(builder, "com.sonarsource.license", "sonar-dev-license-plugin");

    builder.activateLicense();
    ORCHESTRATOR = builder.build();
    ORCHESTRATOR.start();
  }

  @Rule
  public ErrorCollector errorCollector = new ErrorCollector();

  @Test
  public void analysis_of_project_with_all_supported_languages() {
    SonarScanner analysis = newAnalysis();
    BuildResult result = ORCHESTRATOR.executeBuildQuietly(analysis);
    if (result.getLastStatus() != 0) {
      fail(result.getLogs());
    }
    for (Check check : CHECKS) {
      System.out.println(check.getClass().getSimpleName() + "...");
      check.validate(new Validation(ORCHESTRATOR, errorCollector));
    }
  }

  @Test
  public void preview_analysis_of_project_with_all_supported_languages() {
    SonarScanner analysis = newAnalysis();
    analysis.setProperty("sonar.analysis.mode", "issues");
    BuildResult result = ORCHESTRATOR.executeBuildQuietly(analysis);
    if (result.getLastStatus() != 0) {
      fail(result.getLogs());
    }
  }

  private static SonarScanner newAnalysis() {
    SonarScanner analysis = SonarScanner.create(Project.basedir());

    // required to bypass usage of build-wrapper
    analysis.setProperties("sonar.cfamily.build-wrapper-output.bypass", "true");
    return analysis;
  }

  private static void installPlugin(OrchestratorBuilder builder, String groupId, String artifactId) {
    builder.addPlugin(MavenLocation.of(groupId, artifactId, "LATEST_RELEASE"));
  }
}

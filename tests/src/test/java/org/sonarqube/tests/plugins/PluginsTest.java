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
import java.io.File;
import java.net.MalformedURLException;
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

import static com.sonar.orchestrator.locator.FileLocation.byWildcardMavenFilename;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verify that latest releases of the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  private static final List<Check> CHECKS = Arrays.asList(
    new AbapCheck(),
    new CCheck(), new CppCheck(),
    new CobolCheck(),
    // FIXME css plugin is temporary disabled as for the moment incompatible with the web plugin
    // new CssCheck(),
    new FlexCheck(),
    new GroovyCheck(),
    new JavaCheck(),
    new JavascriptCheck(),
    new PhpCheck(),
    new PliCheck(),
    new PlsqlCheck(),
    new PythonCheck(),
    new RpgCheck(),
    new SwiftCheck(),
    // SONAR-7618 Visual Basic 2.2 not compatible with CE not loading @ServerSide
    // new VbCheck(),
    new WebCheck());

  private static Orchestrator ORCHESTRATOR;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setZipFile(byWildcardMavenFilename(new File("../sonar-application/target"), "sonar*.zip").getFile());

    builder.addPlugin(MavenLocation.of("com.sonarsource.license", "sonar-dev-license-plugin", "3.2.0.1163"));

    // FIXME JSON plugin is temporarily disabled as for the moment the github repo doesn't exist anymore installPlugin(builder, "JSON");;
    installPlugin(builder, "Sonargraph");
    installPlugin(builder, "abap");
    // FIXME AEM Rules plugin is disabled because it is no more compatible with SonarQube 6.4 (ClassNotFoundException: com.google.common.base.Functions) installPlugin(builder, "aemrules");
    installPlugin(builder, "android");
    installPlugin(builder, "authbitbucket");
    installPlugin(builder, "authgithub");
    installPlugin(builder, "checkstyle");
    installPlugin(builder, "clover");
    installPlugin(builder, "cobol");
    installPlugin(builder, "codecrackercsharp");
    installPlugin(builder, "cpp");
    installPlugin(builder, "csharp");
    // FIXME css plugin is temporarily disabled as for the moment incompatible with the web plugin installPlugin(builder, "css");
    // FIXME erlang plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    // FIXME findbugs plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    installPlugin(builder, "flex");
    installPlugin(builder, "github");
    installPlugin(builder, "googleanalytics");
    installPlugin(builder, "groovy");
    installPlugin(builder, "java");
    // FIXME javaProperties plugin is temporarily disabled as for the moment the github repo doesn't exist anymore installPlugin(builder, "javaProperties");
    installPlugin(builder, "javascript");
    installPlugin(builder, "jdepend");
    installPlugin(builder, "l10nde");
    installPlugin(builder, "l10nel");
    installPlugin(builder, "l10nes");
    installPlugin(builder, "l10nfr");
    installPlugin(builder, "l10nit");
    installPlugin(builder, "l10nja");
    installPlugin(builder, "l10nko");
    installPlugin(builder, "l10npt");
    installPlugin(builder, "l10nru");
    installPlugin(builder, "l10nzh");
    installPlugin(builder, "ldap");
    installPlugin(builder, "lua");
    installPlugin(builder, "php");
    installPlugin(builder, "pitest");
    installPlugin(builder, "pli");
    installPlugin(builder, "plsql");
    installPlugin(builder, "pmd");
    // FIXME puppet plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    installPlugin(builder, "python");
    installPlugin(builder, "rci");
    installPlugin(builder, "rpg");
    installPlugin(builder, "scmclearcase");
    installPlugin(builder, "scmcvs");
    installPlugin(builder, "scmgit");
    installPlugin(builder, "scmjazzrtc");
    installPlugin(builder, "scmmercurial");
    installPlugin(builder, "scmperforce");
    installPlugin(builder, "scmsvn");
    installPlugin(builder, "scmtfvc");
    installPlugin(builder, "softvis3d");
    installPlugin(builder, "sonargraphintegration");
    installPlugin(builder, "status");
    installPlugin(builder, "swift");
    // SONAR-7618 Visual Basic 2.2 not compatible with CE not loading @ServerSide installPlugin(builder, "vb");
    installPlugin(builder, "vbnet");
    installPlugin(builder, "web");
    installPlugin(builder, "xanitizer");
    installPlugin(builder, "xml");

    activateLicenses(builder);
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

  private static void activateLicenses(OrchestratorBuilder builder) {
    builder.activateLicense();
  }

  private static void installPlugin(OrchestratorBuilder builder, String pluginKey) {
    builder.setOrchestratorProperty(pluginKey + "Version", "LATEST_RELEASE");
    builder.addPlugin(pluginKey);
  }
}

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
package it.plugins;

import com.google.common.collect.Sets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.plugins.checks.AbapCheck;
import it.plugins.checks.CCheck;
import it.plugins.checks.Check;
import it.plugins.checks.CobolCheck;
import it.plugins.checks.CppCheck;
import it.plugins.checks.FlexCheck;
import it.plugins.checks.GroovyCheck;
import it.plugins.checks.JavaCheck;
import it.plugins.checks.JavascriptCheck;
import it.plugins.checks.PhpCheck;
import it.plugins.checks.PliCheck;
import it.plugins.checks.PythonCheck;
import it.plugins.checks.RpgCheck;
import it.plugins.checks.SwiftCheck;
import it.plugins.checks.Validation;
import it.plugins.checks.VbCheck;
import it.plugins.checks.WebCheck;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static com.sonar.orchestrator.locator.FileLocation.byWildcardMavenFilename;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verify that latest releases of the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  private static final Set<String> LICENSED_PLUGINS = Sets.newHashSet(
    "abap", "cobol", "cpp", "objc", "pli", "plsql", "rpg",
    "swift", "vb", "vbnet");

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
    new PythonCheck(),
    new RpgCheck(),
    new SwiftCheck(),
    new VbCheck(),
    new WebCheck());

  private static Orchestrator ORCHESTRATOR;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setZipFile(byWildcardMavenFilename(new File("../../sonar-application/target"), "sonar*.zip").getFile());

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
    installPlugin(builder, "vb");
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
    LICENSED_PLUGINS.forEach(builder::activateLicense);
  }
  
  private static void installPlugin(OrchestratorBuilder builder, String pluginKey) {
    builder.setOrchestratorProperty(pluginKey + "Version", "LATEST_RELEASE");
    builder.addPlugin(pluginKey);
  }

}

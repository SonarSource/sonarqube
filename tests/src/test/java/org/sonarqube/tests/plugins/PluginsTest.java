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

import com.google.common.collect.Sets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Version;
import org.sonarqube.tests.plugins.checks.Check;
import org.sonarqube.tests.plugins.checks.FlexCheck;
import org.sonarqube.tests.plugins.checks.GroovyCheck;
import org.sonarqube.tests.plugins.checks.JavaCheck;
import org.sonarqube.tests.plugins.checks.JavascriptCheck;
import org.sonarqube.tests.plugins.checks.PhpCheck;
import org.sonarqube.tests.plugins.checks.PythonCheck;
import org.sonarqube.tests.plugins.checks.Validation;
import org.sonarqube.tests.plugins.checks.WebCheck;

import static com.sonar.orchestrator.locator.FileLocation.byWildcardMavenFilename;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verify that latest releases of the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  private static final Set<String> LICENSED_PLUGINS = Sets.newHashSet(
    "abap", "cobol", "cpp", "objc", "pli", "plsql", "rpg",
    "swift", "vb", "vbnet");

  private static List<Plugin> installablePlugins;

  private static final List<Check> CHECKS = Arrays.asList(
    // FIXME plsql is disabled as latest release is not using new license manager new AbapCheck(),
    // FIXME cpp is disabled as latest release is not using new license manager new CCheck(), new CppCheck(),
    // FIXME cobol is disabled as latest release is not using new license manager new CobolCheck(),
    // FIXME css plugin is temporary disabled as for the moment incompatible with the web plugin
    // new CssCheck(),
    new FlexCheck(),
    new GroovyCheck(),
    new JavaCheck(),
    new JavascriptCheck(),
    new PhpCheck(),
    // FIXME pli is disabled as latest release is not using new license manager new PliCheck(),
    // FIXME plsql is disabled as latest release is not using new license manager  new PlsqlCheck(),
    new PythonCheck(),
    // FIXME rpg is disabled as latest release is not using new license manager new RpgCheck(),
    // FIXME swift is disabled as latest release is not using new license manager new SwiftCheck(),
    // SONAR-7618 Visual Basic 2.2 not compatible with CE not loading @ServerSide
    // new VbCheck(),
    new WebCheck());

  private static Orchestrator ORCHESTRATOR;

  @BeforeClass
  public static void startServer() throws MalformedURLException {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setZipFile(byWildcardMavenFilename(new File("../sonar-application/target"), "sonar*.zip").getFile());

    // FIXME JSON plugin is temporarily disabled as for the moment the github repo doesn't exist anymore installPlugin(builder, "JSON");;
    installPlugin(builder, "Sonargraph");
    // FIXME abap is disabled as latest release is not using new license manager installPlugin(builder, "abap");
    // FIXME AEM Rules plugin is disabled because it is no more compatible with SonarQube 6.4 (ClassNotFoundException: com.google.common.base.Functions) installPlugin(builder, "aemrules");
    installPlugin(builder, "android");
    installPlugin(builder, "authbitbucket");
    installPlugin(builder, "authgithub");
    installPlugin(builder, "checkstyle");
    installPlugin(builder, "clover");
    // FIXME cobol is disabled as latest release is not using new license manager installPlugin(builder, "cobol");
    installPlugin(builder, "codecrackercsharp");
    // FIXME cpp is disabled as latest release is not using new license manager installPlugin(builder, "cpp");
    installPlugin(builder, "csharp");
    // FIXME css plugin is temporarily disabled as for the moment incompatible with the web plugin installPlugin(builder, "css");
    // FIXME erlang plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    // FIXME findbugs plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    installPlugin(builder, "flex");
    installPlugin(builder, "github");
    // FIXME google analytics is not compatible with 6.7 as 2/14/18
    // installPlugin(builder, "googleanalytics");
    installPlugin(builder, "groovy");
    installPlugin(builder, "java");
    // FIXME javaProperties plugin is temporarily disabled as for the moment the github repo doesn't exist anymore installPlugin(builder, "javaProperties");
    installPlugin(builder, "javascript");
    installPlugin(builder, "jdepend");
    installPlugin(builder, "l10nde");
    installPlugin(builder, "l10nel");
    installPlugin(builder, "l10nes");
    // FIXME google analytics is not compatible with 6.7 as 6/8/18
    // installPlugin(builder, "l10nfr");
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
    // SONAR-7618 SonarPLI release 1.5.0.702 not compatible with CE not loading @ServerSide. To be reset to LATEST_RELEASE as soon as SonarPLI 1.5.1 is released.
    // FIXME pli is disabled as latest release is not using new license manager installPlugin(builder, new URL("https://sonarsource.bintray.com/CommercialDistribution/sonar-pli-plugin/sonar-pli-plugin-1.5.1.872.jar"));
    // SONAR-7618 SonarPLSQL 2.9.0.901 not compatible with CE not loading @ServerSide. To be reset to LATEST_RELEASE as soon as SonarPLSQL 2.9.1 is released.
    // FIXME plsql is disabled as latest release is not using new license manager installPlugin(builder, new URL("https://sonarsource.bintray.com/CommercialDistribution/sonar-plsql-plugin/sonar-plsql-plugin-2.9.1.1051.jar"));
    installPlugin(builder, "pmd");
    // FIXME puppet plugin is temporarily disabled because it is not compatible with SQ 6.4 until usage of Colorizer API is removed
    installPlugin(builder, "python");
    installPlugin(builder, "rci");
    // FIXME rpg is disabled as latest release is not using new license manager installPlugin(builder, "rpg");
    installPlugin(builder, "scmclearcase");
    installPlugin(builder, "scmcvs");
    installPlugin(builder, "scmgit");
    installPlugin(builder, "scmjazzrtc");
    installPlugin(builder, "scmmercurial");
    installPlugin(builder, "scmperforce");
    installPlugin(builder, "scmsvn");
    installPlugin(builder, "scmtfvc");
    installPlugin(builder, "softvis3d");
    // FIXME google analytics is not compatible with 6.7 as 6/8/18
    // installPlugin(builder, "sonargraphintegration");
    installPlugin(builder, "status");
    // FIXME swift is disabled as latest release is not using new license manager installPlugin(builder, "swift");
    // SONAR-7618 Visual Basic 2.2 not compatible with CE not loading @ServerSide
    // installPlugin(builder, "vb");
    // FIXME vbnet is disabled as latest release is not using new license manager installPlugin(builder, "vbnet");
    installPlugin(builder, "web");
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
    Optional<String> version = getCompatibleVersionFor(builder, pluginKey);
    if (version.isPresent()) {
      builder.setOrchestratorProperty(pluginKey + "Version", version.get());
      builder.addPlugin(pluginKey);
    } else {
      throw new IllegalStateException(
        format("Error : cannot install plugin %s, no compatible version found !", pluginKey));
    }
  }

  private static Optional<String> getCompatibleVersionFor(OrchestratorBuilder builder, String pluginKey) {
    if (installablePlugins == null) {
      installablePlugins = builder.getUpdateCenter()
        .setInstalledSonarVersion(Version.create("6.7.1"))
        .findAllCompatiblePlugins();
    }

    return installablePlugins.stream()
      .filter(p -> p.getKey().equals(pluginKey))
      .map(p -> p.getVersions().first().toString())
      .findFirst();
  }
}

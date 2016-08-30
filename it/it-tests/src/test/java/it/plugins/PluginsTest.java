/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.plugins.checks.CCheck;
import it.plugins.checks.Check;
import it.plugins.checks.CppCheck;
import it.plugins.checks.FlexCheck;
import it.plugins.checks.GroovyCheck;
import it.plugins.checks.JavaCheck;
import it.plugins.checks.JavascriptCheck;
import it.plugins.checks.PhpCheck;
import it.plugins.checks.PliCheck;
import it.plugins.checks.PythonCheck;
import it.plugins.checks.SwiftCheck;
import it.plugins.checks.Validation;
import it.plugins.checks.VbCheck;
import it.plugins.checks.WebCheck;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.fail;

/**
 * Verify that the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  /**
   * Temporarily disabled plugins. To be re-enabled.
   */
  static final Set<String> DISABLED_PLUGINS = Sets.newHashSet(
    // internal plugin used for integration tests of language plugins
    "lits",

    "citymodel",
    // temporary incompatibility with cobol 3.1
    // https://jira.sonarsource.com/browse/MAIF-213
    "maifcobolplugin",

    // SONAR-7770 Realm plugins cannot be installed as no external configuration is used
    "crowd", "ldap", "pam");

  static final Set<String> LICENSED_PLUGINS = Sets.newHashSet(
    "abap", "cobol", "cpp", "devcockpit", "governance", "objc", "pli", "plsql", "report", "rpg",
    "swift", "vb", "vbnet");

  static final Set<String> DISABLED_PLUGINS_FOR_PREVIEW_MODE = Sets.newHashSet("mantis",

    // Caused by: Access to the secured property 'sonar.scm.user.secured' is not possible in preview mode. The SonarQube plugin which
    // requires
    // this property must be deactivated in preview mode.
    "scmstats");

  static final List<Check> CHECKS = Arrays.<Check>asList(
    // waiting for release of ABAP 3.3 (ABAP-287)
    // new AbapCheck(),
    new CCheck(), new CppCheck(),
    // waiting for new Cobol release (COBOL-1332)
    // new CobolCheck(),
    // waiting for release of CSS 2.1 (https://github.com/SonarQubeCommunity/sonar-css/issues/261)
    // new CssCheck(),
    new FlexCheck(),
    new GroovyCheck(),
    new JavaCheck(),
    new JavascriptCheck(),
    new PhpCheck(),
    new PliCheck(),
    new PythonCheck(),
    // waiting for new RPG release (RPG-136)
    // new RpgCheck(),
    new SwiftCheck(),
    new VbCheck(),
    new WebCheck());

  static Orchestrator orchestrator;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();

    // install latest compatible releases of plugins
    org.sonar.updatecenter.common.Version sonarVersion = org.sonar.updatecenter.common.Version.create(builder.getSonarVersion());
    Version sonarVersionWithoutPatch = Version.create(sonarVersion.getMajor() + "." + sonarVersion.getMinor());
    builder.getUpdateCenter().setInstalledSonarVersion(sonarVersionWithoutPatch);
    for (Plugin plugin : builder.getUpdateCenter().findAllCompatiblePlugins()) {
      if (!DISABLED_PLUGINS.contains(plugin.getKey())) {
        Release release = plugin.getLastCompatibleRelease(sonarVersionWithoutPatch);
        if (release != null) {
          builder.setOrchestratorProperty(plugin.getKey() + "Version", release.getVersion().toString());
          builder.addPlugin(plugin.getKey());
        }
      }
    }
    activateLicenses(builder);
    orchestrator = builder.build();
    orchestrator.start();
  }

  @Rule
  public ErrorCollector errorCollector = new ErrorCollector();

  @Test
  public void analysis_of_project_with_all_supported_languages() {
    SonarScanner analysis = newAnalysis();
    BuildResult result = orchestrator.executeBuildQuietly(analysis);
    if (result.getStatus() != 0) {
      fail(result.getLogs());
    }
    for (Check check : CHECKS) {
      System.out.println(check.getClass().getSimpleName() + "...");
      check.validate(new Validation(orchestrator, errorCollector));
    }
  }

  @Test
  public void preview_analysis_of_project_with_all_supported_languages() {
    SonarScanner analysis = newAnalysis();
    analysis.setProperty("sonar.analysis.mode", "issues");
    analysis.setProperty("sonar.preview.excludePlugins", Joiner.on(",").join(DISABLED_PLUGINS_FOR_PREVIEW_MODE));
    BuildResult result = orchestrator.executeBuildQuietly(analysis);
    if (result.getStatus() != 0) {
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
    for (String licensedPlugin : LICENSED_PLUGINS) {
      if (!DISABLED_PLUGINS.contains(licensedPlugin)) {
        builder.activateLicense(licensedPlugin);
      }
    }
  }

}

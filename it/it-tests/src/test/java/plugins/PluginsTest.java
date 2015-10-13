/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package plugins;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import plugins.checks.AbapCheck;
import plugins.checks.Check;
import plugins.checks.CobolCheck;
import plugins.checks.CssCheck;
import plugins.checks.FlexCheck;
import plugins.checks.GroovyCheck;
import plugins.checks.JavaCheck;
import plugins.checks.JavascriptCheck;
import plugins.checks.PhpCheck;
import plugins.checks.PythonCheck;
import plugins.checks.RpgCheck;
import plugins.checks.SwiftCheck;
import plugins.checks.Validation;
import plugins.checks.WebCheck;

import static org.assertj.core.api.Assertions.fail;

/**
 * Verify that the plugins available in update center
 * are correctly supported.
 */
public class PluginsTest {

  /**
   * Temporarily disabled plugins. To be re-enabled.
   */
  static final Set<String> DISABLED_PLUGINS = Sets.newHashSet("devcockpit", "views",
    // internal plugin used for integration tests of language plugins
    "lits");

  static final Set<String> LICENSED_PLUGINS = Sets.newHashSet(
    "abap", "cobol", "cpp", "devcockpit", "objc", "pli", "plsql", "report", "rpg",
    "sqale", "swift", "vb", "vbnet", "views");

  static final Set<String> DISABLED_PLUGINS_FOR_PREVIEW_MODE = Sets.newHashSet("mantis",

  // Caused by: Access to the secured property 'sonar.scm.user.secured' is not possible in preview mode. The SonarQube plugin which requires
  // this property must be deactivated in preview mode.
    "scmstats");

  // TODO new PliCheck() is temporarily disabled as PLI plugin does not support multi-language feature. See sonar-project.properties
  // TODO new CCheck(), CppCheck() and VbCheck() are temporarily disabled as there is no version compatible with SQ 5.2 (they are using
  // Violation API).
  static final List<Check> CHECKS = Arrays.asList((Check) new AbapCheck(), new CobolCheck(), new CssCheck(),
    new FlexCheck(), new GroovyCheck(), new JavaCheck(), new JavascriptCheck(), new PhpCheck(), new RpgCheck(),
    new PythonCheck(), new SwiftCheck(), new WebCheck());

  static Orchestrator orchestrator;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();

    // install latest compatible releases of plugins
    org.sonar.updatecenter.common.Version sonarVersion = org.sonar.updatecenter.common.Version.create(builder.getSonarVersion());
    builder.getUpdateCenter().setInstalledSonarVersion(sonarVersion);
    for (Plugin plugin : builder.getUpdateCenter().findAllCompatiblePlugins()) {
      if (!DISABLED_PLUGINS.contains(plugin.getKey())) {
        Release release = plugin.getLastCompatibleRelease(sonarVersion);
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
    SonarRunner analysis = newAnalysis();
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
    SonarRunner analysis = newAnalysis();
    analysis.setProperty("sonar.analysis.mode", "issues");
    analysis.setProperty("sonar.preview.excludePlugins", Joiner.on(",").join(DISABLED_PLUGINS_FOR_PREVIEW_MODE));
    BuildResult result = orchestrator.executeBuildQuietly(analysis);
    if (result.getStatus() != 0) {
      fail(result.getLogs());
    }
  }

  private static SonarRunner newAnalysis() {
    SonarRunner analysis = SonarRunner.create(Project.basedir());
    analysis.setEnvironmentVariable("SONAR_RUNNER_OPTS", "-XX:MaxPermSize=128m");
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

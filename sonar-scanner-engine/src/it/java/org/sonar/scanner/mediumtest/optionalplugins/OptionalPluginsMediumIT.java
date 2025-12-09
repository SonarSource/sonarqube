/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.mediumtest.optionalplugins;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.core.config.ScannerProperties.PLUGIN_LOADING_OPTIMIZATION_KEY;

public class OptionalPluginsMediumIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .setEdition(SonarEdition.COMMUNITY)
    .registerPlugin("xoo", new XooPlugin())
    .registerPlugin("nonoptional-plugin", new NonOptionalPlugin())
    .registerOptionalPlugin("optional-plugin", Set.of("xoo"), new OptionalPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  @Before
  public void prepare() throws IOException {
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void should_load_input_file_filters_for_required_and_optional_plugins() {
    File projectDir = new File("test-resources/mediumtest/xoo/sample-with-input-file-filters");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .properties(Map.of(PLUGIN_LOADING_OPTIMIZATION_KEY, "true"))
      .execute();

    assertThat(result.inputFiles()).hasSize(1);

    assertThat(logTester.logs()).contains("'xources/hello/xoo_exclude2.xoo' excluded by org.sonar.scanner.mediumtest.optionalplugins" +
      ".OptionalPluginsMediumIT$NonOptionalPlugin$NonOptionalXooFileFilter");
    assertThat(logTester.logs()).contains("'xources/hello/xoo_exclude3.xoo' excluded by org.sonar.scanner.mediumtest.optionalplugins" +
      ".OptionalPluginsMediumIT$OptionalPlugin$OptionalXooFileFilter");
    assertThat(logTester.logs()).contains("'xources/hello/xoo_exclude.xoo' excluded by org.sonar.xoo.extensions.XooExcludeFileFilter");
    assertThat(logTester.logs()).contains("'xources/hello/HelloJava.xoo' indexed with language 'xoo'");

    assertThat(result.inputFile("xources/hello/xoo_exclude.xoo")).isNull();
    assertThat(result.inputFile("xources/hello/xoo_exclude2.xoo")).isNull();
    assertThat(result.inputFile("xources/hello/xoo_exclude3.xoo")).isNull();
    assertThat(result.inputFile("xources/hello/HelloJava.xoo")).isNotNull();
  }

  @Test
  public void should_load_issue_filters_for_optional_plugins() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .properties(Map.of(PLUGIN_LOADING_OPTIMIZATION_KEY, "true"))
      .execute();

    List<ScannerReport.Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(8 - 1 /* line 2 excluded by  NonOptionalIssueFiler*/ - 1 /* line 3 excluded by  OptionalIssueFiler*/);
  }

  @Test
  public void should_save_measures_for_optional_plugins() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .properties(Map.of(PLUGIN_LOADING_OPTIMIZATION_KEY, "true"))
      .execute();

    assertThat(result.allMeasures().get(result.project().key()))
      .extracting(ScannerReport.Measure::getMetricKey, m -> m.getIntValue().getValue())
      .containsExactlyInAnyOrder(
        tuple(CoreMetrics.CLASSES_KEY, 1),
        tuple(CoreMetrics.FUNCTIONS_KEY, 2));
  }

  public static class NonOptionalPlugin implements Plugin {

    @Override
    public void define(Context context) {
      context.addExtensions(
        NonOptionalXooFileFilter.class,
        NonOptionalIssueFilter.class,
        NonOptionalSensor.class);
    }

    public static class NonOptionalXooFileFilter implements InputFileFilter {

      @Override
      public boolean accept(InputFile f) {
        return !f.filename().endsWith("_exclude2.xoo");
      }
    }

    public static class NonOptionalIssueFilter implements IssueFilter {

      @Override
      public boolean accept(FilterableIssue filterableIssue, IssueFilterChain issueFilterChain) {
        if (!issueFilterChain.accept(filterableIssue)) {
          return false;
        }
        // Suppress issues on line 2
        var line = filterableIssue.line();
        return line == null || line != 2;
      }
    }

    public static class NonOptionalSensor implements ProjectSensor {

      private final MetricFinder metricFinder;
      private final FileLinesContextFactory fileLinesContextFactory;

      public NonOptionalSensor(MetricFinder metricFinder, FileLinesContextFactory fileLinesContextFactory) {
        this.metricFinder = metricFinder;
        // Simply verify that FileLinesContextFactory is correctly injected
        this.fileLinesContextFactory = fileLinesContextFactory;
      }

      @Override
      public void describe(SensorDescriptor descriptor) {
        descriptor.name("NonOptionalSensor");
      }

      @Override
      public void execute(SensorContext context) {
        var metric = metricFinder.findByKey(CoreMetrics.CLASSES_KEY);
        context.newMeasure().forMetric(metric).withValue(1).on(context.project()).save();
      }
    }
  }

  public static class OptionalPlugin implements Plugin {

    @Override
    public void define(Context context) {
      context.addExtensions(
        OptionalXooFileFilter.class,
        OptionalIssueFilter.class,
        OptionalSensor.class);
    }

    public static class OptionalXooFileFilter implements InputFileFilter {

      @Override
      public boolean accept(InputFile f) {
        return !f.filename().endsWith("_exclude3.xoo");
      }
    }

    public static class OptionalIssueFilter implements IssueFilter {

      @Override
      public boolean accept(FilterableIssue filterableIssue, IssueFilterChain issueFilterChain) {
        if (!issueFilterChain.accept(filterableIssue)) {
          return false;
        }
        // Suppress issues on line 3
        var line = filterableIssue.line();
        return line == null || line != 3;
      }
    }

    public static class OptionalSensor implements ProjectSensor {

      private final MetricFinder metricFinder;
      private final FileLinesContextFactory fileLinesContextFactory;

      public OptionalSensor(MetricFinder metricFinder, FileLinesContextFactory fileLinesContextFactory) {
        this.metricFinder = metricFinder;
        // Simply verify that FileLinesContextFactory is correctly injected
        this.fileLinesContextFactory = fileLinesContextFactory;
      }

      @Override
      public void describe(SensorDescriptor descriptor) {
        descriptor.name("OptionalSensor");
      }

      @Override
      public void execute(SensorContext context) {
        var metric = metricFinder.findByKey(CoreMetrics.FUNCTIONS_KEY);
        context.newMeasure().forMetric(metric).withValue(2).on(context.project()).save();
      }
    }
  }


}

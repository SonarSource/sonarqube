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
package org.sonar.batch.mediumtest;

import com.google.common.base.Function;
import com.google.common.io.Files;
import org.apache.commons.io.Charsets;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.issue.tracking.ServerLineHashesLoader;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.GlobalRepositories;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.repository.GlobalRepositoriesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.core.component.ComponentKeys;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Main utility class for writing batch medium tests.
 * 
 */
public class BatchMediumTester {

  public static final String MEDIUM_TEST_ENABLED = "sonar.mediumTest.enabled";
  private Batch batch;

  public static BatchMediumTesterBuilder builder() {
    BatchMediumTesterBuilder builder = new BatchMediumTesterBuilder().registerCoreMetrics();
    builder.bootstrapProperties.put(MEDIUM_TEST_ENABLED, "true");
    builder.bootstrapProperties.put(ReportPublisher.KEEP_REPORT_PROP_KEY, "true");
    builder.bootstrapProperties.put(CoreProperties.WORKING_DIRECTORY, Files.createTempDir().getAbsolutePath());
    return builder;
  }

  public static class BatchMediumTesterBuilder {
    private final FakeGlobalRepositoriesLoader globalRefProvider = new FakeGlobalRepositoriesLoader();
    private final FakeProjectRepositoriesLoader projectRefProvider = new FakeProjectRepositoriesLoader();
    private final FakePluginInstaller pluginInstaller = new FakePluginInstaller();
    private final FakeServerIssuesLoader serverIssues = new FakeServerIssuesLoader();
    private final FakeServerLineHashesLoader serverLineHashes = new FakeServerLineHashesLoader();
    private final Map<String, String> bootstrapProperties = new HashMap<>();

    public BatchMediumTester build() {
      return new BatchMediumTester(this);
    }

    public BatchMediumTesterBuilder registerPlugin(String pluginKey, File location) {
      pluginInstaller.add(pluginKey, location);
      return this;
    }

    public BatchMediumTesterBuilder registerPlugin(String pluginKey, SonarPlugin instance) {
      pluginInstaller.add(pluginKey, instance);
      return this;
    }

    public BatchMediumTesterBuilder registerCoreMetrics() {
      for (Metric<?> m : CoreMetrics.getMetrics()) {
        registerMetric(m);
      }
      return this;
    }

    public BatchMediumTesterBuilder registerMetric(Metric<?> metric) {
      globalRefProvider.add(metric);
      return this;
    }

    public BatchMediumTesterBuilder addQProfile(String language, String name) {
      projectRefProvider.addQProfile(language, name);
      return this;
    }

    public BatchMediumTesterBuilder addDefaultQProfile(String language, String name) {
      addQProfile(language, name);
      globalRefProvider.globalSettings().put("sonar.profile." + language, name);
      return this;
    }

    public BatchMediumTesterBuilder setPreviousAnalysisDate(Date previousAnalysis) {
      projectRefProvider.ref.setLastAnalysisDate(previousAnalysis);
      return this;
    }

    public BatchMediumTesterBuilder bootstrapProperties(Map<String, String> props) {
      bootstrapProperties.putAll(props);
      return this;
    }

    public BatchMediumTesterBuilder activateRule(ActiveRule activeRule) {
      projectRefProvider.addActiveRule(activeRule);
      return this;
    }

    public BatchMediumTesterBuilder addFileData(String moduleKey, String path, FileData fileData) {
      projectRefProvider.addFileData(moduleKey, path, fileData);
      return this;
    }

    public BatchMediumTesterBuilder mockServerIssue(ServerIssue issue) {
      serverIssues.getServerIssues().add(issue);
      return this;
    }

    public BatchMediumTesterBuilder mockLineHashes(String fileKey, String[] lineHashes) {
      serverLineHashes.byKey.put(fileKey, lineHashes);
      return this;
    }

    public BatchMediumTesterBuilder registerMetrics(List<Metric> metrics) {
      for (Metric<?> m : metrics) {
        registerMetric(m);
      }
      return this;
    }

  }

  public void start() {
    batch.start();
  }

  public void stop() {
    batch.stop();
  }

  private BatchMediumTester(BatchMediumTesterBuilder builder) {
    batch = Batch.builder()
      .setEnableLoggingConfiguration(true)
      .addComponents(
        new EnvironmentInformation("mediumTest", "1.0"),
        builder.pluginInstaller,
        builder.globalRefProvider,
        builder.projectRefProvider,
        builder.serverIssues,
        builder.serverLineHashes,
        new DefaultDebtModel())
      .setBootstrapProperties(builder.bootstrapProperties)
      .build();
  }

  public TaskBuilder newTask() {
    return new TaskBuilder(this);
  }

  public TaskBuilder newScanTask(File sonarProps) {
    Properties prop = new Properties();
    try (Reader reader = new InputStreamReader(new FileInputStream(sonarProps), Charsets.UTF_8)) {
      prop.load(reader);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read configuration file", e);
    }
    TaskBuilder builder = new TaskBuilder(this);
    builder.property("sonar.task", "scan");
    builder.property("sonar.projectBaseDir", sonarProps.getParentFile().getAbsolutePath());
    for (Map.Entry entry : prop.entrySet()) {
      builder.property(entry.getKey().toString(), entry.getValue().toString());
    }
    return builder;
  }

  public static class TaskBuilder {
    private final Map<String, String> taskProperties = new HashMap<>();
    private BatchMediumTester tester;

    public TaskBuilder(BatchMediumTester tester) {
      this.tester = tester;
    }

    public TaskResult start() {
      TaskResult result = new TaskResult();
      Map<String, String> props = new HashMap<>();
      props.putAll(taskProperties);
      tester.batch.executeTask(props, result);
      return result;
    }

    public TaskBuilder properties(Map<String, String> props) {
      taskProperties.putAll(props);
      return this;
    }

    public TaskBuilder property(String key, String value) {
      taskProperties.put(key, value);
      return this;
    }
  }

  private static class FakeGlobalRepositoriesLoader implements GlobalRepositoriesLoader {

    private int metricId = 1;

    private GlobalRepositories ref = new GlobalRepositories();

    @Override
    public GlobalRepositories load() {
      return ref;
    }

    public Map<String, String> globalSettings() {
      return ref.globalSettings();
    }

    public FakeGlobalRepositoriesLoader add(Metric metric) {
      Boolean optimizedBestValue = metric.isOptimizedBestValue();
      ref.metrics().add(new org.sonar.batch.protocol.input.Metric(metricId,
        metric.key(),
        metric.getType().name(),
        metric.getDescription(),
        metric.getDirection(),
        metric.getName(),
        metric.getQualitative(),
        metric.getUserManaged(),
        metric.getWorstValue(),
        metric.getBestValue(),
        optimizedBestValue != null ? optimizedBestValue : false));
      metricId++;
      return this;
    }
  }

  private static class FakeProjectRepositoriesLoader implements ProjectRepositoriesLoader {

    private ProjectRepositories ref = new ProjectRepositories();

    @Override
    public ProjectRepositories load(ProjectReactor reactor, TaskProperties taskProperties) {
      return ref;
    }

    public FakeProjectRepositoriesLoader addQProfile(String language, String name) {
      ref.addQProfile(new org.sonar.batch.protocol.input.QProfile(name, name, language, new Date()));
      return this;
    }

    public FakeProjectRepositoriesLoader addActiveRule(ActiveRule activeRule) {
      ref.addActiveRule(activeRule);
      return this;
    }

    public FakeProjectRepositoriesLoader addFileData(String moduleKey, String path, FileData fileData) {
      ref.addFileData(moduleKey, path, fileData);
      return this;
    }

  }

  private static class FakeServerIssuesLoader implements ServerIssuesLoader {

    private List<ServerIssue> serverIssues = new ArrayList<>();

    public List<ServerIssue> getServerIssues() {
      return serverIssues;
    }

    @Override
    public void load(String componentKey, Function<ServerIssue, Void> consumer, boolean incremental) {
      for (ServerIssue serverIssue : serverIssues) {
        if (!incremental || ComponentKeys.createEffectiveKey(serverIssue.getModuleKey(), serverIssue.hasPath() ? serverIssue.getPath() : null).equals(componentKey)) {
          consumer.apply(serverIssue);
        }
      }

    }

  }

  private static class FakeServerLineHashesLoader implements ServerLineHashesLoader {
    private Map<String, String[]> byKey = new HashMap<String, String[]>();

    @Override
    public String[] getLineHashes(String fileKey) {
      if (byKey.containsKey(fileKey)) {
        return byKey.get(fileKey);
      } else {
        throw new IllegalStateException("You forgot to mock line hashes for " + fileKey);
      }
    }
  }

}

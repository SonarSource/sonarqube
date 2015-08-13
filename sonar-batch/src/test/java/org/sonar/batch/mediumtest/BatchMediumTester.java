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

import org.sonar.batch.analysis.AnalysisProperties;

import org.apache.commons.lang.mutable.MutableBoolean;

import javax.annotation.Nullable;

import org.sonar.batch.cache.ProjectCacheStatus;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonarqube.ws.Rules.ListResponse.Rule;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.batch.rule.RulesLoader;
import com.google.common.base.Function;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.bootstrapper.LogOutput;
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
    private final FakeRulesLoader rulesLoader = new FakeRulesLoader();
    private final FakeProjectCacheStatus projectCacheStatus = new FakeProjectCacheStatus();
    private LogOutput logOutput = null;

    public BatchMediumTester build() {
      return new BatchMediumTester(this);
    }

    public BatchMediumTesterBuilder setLogOutput(LogOutput logOutput) {
      this.logOutput = logOutput;
      return this;
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

    public BatchMediumTesterBuilder addRule(Rule rule) {
      rulesLoader.addRule(rule);
      return this;
    }

    public BatchMediumTesterBuilder addRule(String key, String repoKey, String internalKey, String name) {
      Rule.Builder builder = Rule.newBuilder();
      builder.setKey(key);
      builder.setRepository(repoKey);
      if (internalKey != null) {
        builder.setInternalKey(internalKey);
      }
      builder.setName(name);

      rulesLoader.addRule(builder.build());
      return this;
    }

    public BatchMediumTesterBuilder addRules(RulesDefinition rulesDefinition) {
      RulesDefinition.Context context = new RulesDefinition.Context();
      rulesDefinition.define(context);
      List<Repository> repositories = context.repositories();
      for (Repository repo : repositories) {
        for (RulesDefinition.Rule rule : repo.rules()) {
          this.addRule(rule.key(), rule.repository().key(), rule.internalKey(), rule.name());
        }
      }
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

    public BatchMediumTesterBuilder setLastBuildDate(Date d) {
      projectRefProvider.setLastAnalysisDate(d);
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

  }

  public void start() {
    batch.start();
  }

  public void stop() {
    batch.stop();
  }

  public void syncProject(String projectKey) {
    batch.syncProject(projectKey);
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
        builder.rulesLoader,
        builder.projectCacheStatus,
        new DefaultDebtModel())
      .setBootstrapProperties(builder.bootstrapProperties)
      .setLogOutput(builder.logOutput)
      .build();
  }

  public TaskBuilder newTask() {
    return new TaskBuilder(this);
  }

  public TaskBuilder newScanTask(File sonarProps) {
    Properties prop = new Properties();
    try (Reader reader = new InputStreamReader(new FileInputStream(sonarProps), StandardCharsets.UTF_8)) {
      prop.load(reader);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read configuration file", e);
    }
    TaskBuilder builder = new TaskBuilder(this);
    builder.property("sonar.projectBaseDir", sonarProps.getParentFile().getAbsolutePath());
    for (Map.Entry<Object, Object> entry : prop.entrySet()) {
      builder.property(entry.getKey().toString(), entry.getValue().toString());
    }
    return builder;
  }

  public static class TaskBuilder {
    private final Map<String, String> taskProperties = new HashMap<>();
    private BatchMediumTester tester;
    private IssueListener issueListener = null;

    public TaskBuilder(BatchMediumTester tester) {
      this.tester = tester;
    }

    public TaskResult start() {
      TaskResult result = new TaskResult();
      Map<String, String> props = new HashMap<>();
      props.putAll(taskProperties);
      if (issueListener != null) {
        tester.batch.executeTask(props, result, issueListener);
      } else {
        tester.batch.executeTask(props, result);
      }
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

    public TaskBuilder setIssueListener(IssueListener issueListener) {
      this.issueListener = issueListener;
      return this;
    }
  }

  private static class FakeRulesLoader implements RulesLoader {
    private List<org.sonarqube.ws.Rules.ListResponse.Rule> rules = new LinkedList<>();

    public FakeRulesLoader addRule(Rule rule) {
      rules.add(rule);
      return this;
    }

    @Override
    public List<Rule> load(@Nullable MutableBoolean fromCache) {
      return rules;
    }
  }

  private static class FakeGlobalRepositoriesLoader implements GlobalRepositoriesLoader {

    private int metricId = 1;

    private GlobalRepositories ref = new GlobalRepositories();

    @Override
    public GlobalRepositories load(@Nullable MutableBoolean fromCache) {
      return ref;
    }

    public Map<String, String> globalSettings() {
      return ref.globalSettings();
    }

    public FakeGlobalRepositoriesLoader add(Metric<?> metric) {
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
    public ProjectRepositories load(ProjectDefinition projDefinition, AnalysisProperties taskProperties, @Nullable MutableBoolean fromCache) {
      return ref;
    }

    public FakeProjectRepositoriesLoader addQProfile(String language, String name) {
      // Use a fixed date to allow assertions
      ref.addQProfile(new org.sonar.batch.protocol.input.QProfile(name, name, language, new Date(1234567891212L)));
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

    public FakeProjectRepositoriesLoader setLastAnalysisDate(Date d) {
      ref.setLastAnalysisDate(d);
      return this;
    }

  }

  private static class FakeServerIssuesLoader implements ServerIssuesLoader {

    private List<ServerIssue> serverIssues = new ArrayList<>();

    public List<ServerIssue> getServerIssues() {
      return serverIssues;
    }

    @Override
    public boolean load(String componentKey, Function<ServerIssue, Void> consumer) {
      for (ServerIssue serverIssue : serverIssues) {
        consumer.apply(serverIssue);
      }
      return true;
    }

  }

  private static class FakeProjectCacheStatus implements ProjectCacheStatus {

    @Override
    public void save(String projectKey) {
    }

    @Override
    public void delete(String projectKey) {
    }

    @Override
    public Date getSyncStatus(String projectKey) {
      return new Date();
    }

  }

  private static class FakeServerLineHashesLoader implements ServerLineHashesLoader {
    private Map<String, String[]> byKey = new HashMap<>();

    @Override
    public String[] getLineHashes(String fileKey, @Nullable MutableBoolean fromCache) {
      if (byKey.containsKey(fileKey)) {
        return byKey.get(fileKey);
      } else {
        throw new IllegalStateException("You forgot to mock line hashes for " + fileKey);
      }
    }
  }

}

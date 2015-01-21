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
import org.apache.commons.io.Charsets;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.batch.bootstrap.PluginsReferential;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.GlobalReferentials;
import org.sonar.batch.protocol.input.ProjectRepository;
import org.sonar.batch.protocol.input.issues.PreviousIssue;
import org.sonar.batch.repository.GlobalReferentialsLoader;
import org.sonar.batch.repository.PreviousIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.core.plugins.RemotePlugin;

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

  private Batch batch;

  public static BatchMediumTesterBuilder builder() {
    return new BatchMediumTesterBuilder().registerCoreMetrics();
  }

  public static class BatchMediumTesterBuilder {
    private final FakeGlobalReferentialsLoader globalRefProvider = new FakeGlobalReferentialsLoader();
    private final FakeProjectReferentialsLoader projectRefProvider = new FakeProjectReferentialsLoader();
    private final FakePluginsReferential pluginsReferential = new FakePluginsReferential();
    private final FakePreviousIssuesLoader previousIssues = new FakePreviousIssuesLoader();
    private final Map<String, String> bootstrapProperties = new HashMap<String, String>();

    public BatchMediumTester build() {
      return new BatchMediumTester(this);
    }

    public BatchMediumTesterBuilder registerPlugin(String pluginKey, File location) {
      pluginsReferential.addPlugin(pluginKey, location);
      return this;
    }

    public BatchMediumTesterBuilder registerPlugin(String pluginKey, SonarPlugin instance) {
      pluginsReferential.addPlugin(pluginKey, instance);
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

    public BatchMediumTesterBuilder bootstrapProperties(Map<String, String> props) {
      bootstrapProperties.putAll(props);
      return this;
    }

    public BatchMediumTesterBuilder activateRule(ActiveRule activeRule) {
      projectRefProvider.addActiveRule(activeRule);
      return this;
    }

    public BatchMediumTesterBuilder addPreviousIssue(PreviousIssue issue) {
      previousIssues.getPreviousIssues().add(issue);
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
        builder.pluginsReferential,
        builder.globalRefProvider,
        builder.projectRefProvider,
        builder.previousIssues,
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
      tester.batch.executeTask(taskProperties,
        result
        );
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

  private static class FakeGlobalReferentialsLoader implements GlobalReferentialsLoader {

    private int metricId = 1;

    private GlobalReferentials ref = new GlobalReferentials();

    @Override
    public GlobalReferentials load() {
      return ref;
    }

    public Map<String, String> globalSettings() {
      return ref.globalSettings();
    }

    public FakeGlobalReferentialsLoader add(Metric metric) {
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

  private static class FakeProjectReferentialsLoader implements ProjectRepositoriesLoader {

    private ProjectRepository ref = new ProjectRepository();

    @Override
    public ProjectRepository load(ProjectReactor reactor, TaskProperties taskProperties) {
      return ref;
    }

    public FakeProjectReferentialsLoader addQProfile(String language, String name) {
      ref.addQProfile(new org.sonar.batch.protocol.input.QProfile(name, name, language, new Date()));
      return this;
    }

    public FakeProjectReferentialsLoader addActiveRule(ActiveRule activeRule) {
      ref.addActiveRule(activeRule);
      return this;
    }
  }

  private static class FakePluginsReferential implements PluginsReferential {

    private List<RemotePlugin> pluginList = new ArrayList<RemotePlugin>();
    private Map<RemotePlugin, File> pluginFiles = new HashMap<RemotePlugin, File>();
    Map<PluginMetadata, SonarPlugin> localPlugins = new HashMap<PluginMetadata, SonarPlugin>();

    @Override
    public List<RemotePlugin> pluginList() {
      return pluginList;
    }

    @Override
    public File pluginFile(RemotePlugin remote) {
      return pluginFiles.get(remote);
    }

    public FakePluginsReferential addPlugin(String pluginKey, File location) {
      RemotePlugin plugin = new RemotePlugin(pluginKey, false);
      pluginList.add(plugin);
      pluginFiles.put(plugin, location);
      return this;
    }

    public FakePluginsReferential addPlugin(String pluginKey, SonarPlugin pluginInstance) {
      localPlugins.put(DefaultPluginMetadata.create(pluginKey), pluginInstance);
      return this;
    }

    @Override
    public Map<PluginMetadata, SonarPlugin> localPlugins() {
      return localPlugins;
    }

  }

  private static class FakePreviousIssuesLoader implements PreviousIssuesLoader {

    List<PreviousIssue> previousIssues = new ArrayList<>();

    public List<PreviousIssue> getPreviousIssues() {
      return previousIssues;
    }

    @Override
    public void load(ProjectReactor reactor, Function<PreviousIssue, Void> consumer) {
      for (PreviousIssue previousIssue : previousIssues) {
        consumer.apply(previousIssue);
      }

    }

  }

}

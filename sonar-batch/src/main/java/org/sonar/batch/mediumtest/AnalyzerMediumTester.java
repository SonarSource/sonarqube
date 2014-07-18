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

import org.apache.commons.io.IOUtils;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssue;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.resources.Languages;
import org.sonar.batch.bootstrap.PluginsReferential;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.batch.referential.ProjectReferentialsLoader;
import org.sonar.batch.scan.filesystem.InputFileCache;
import org.sonar.batch.scan2.AnalyzerIssueCache;
import org.sonar.batch.scan2.AnalyzerMeasureCache;
import org.sonar.batch.scan2.ProjectScanContainer;
import org.sonar.batch.scan2.ScanTaskObserver;
import org.sonar.batch.settings.SettingsReferential;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AnalyzerMediumTester {

  private Batch batch;

  public static AnalyzerMediumTesterBuilder builder() {
    return new AnalyzerMediumTesterBuilder().registerCoreMetrics();
  }

  public static class AnalyzerMediumTesterBuilder {
    private final FakeProjectReferentialsLoader refProvider = new FakeProjectReferentialsLoader();
    private final FakeSettingsReferential settingsReferential = new FakeSettingsReferential();
    private final FackPluginsReferential pluginsReferential = new FackPluginsReferential();
    private final Map<String, String> bootstrapProperties = new HashMap<String, String>();

    public AnalyzerMediumTester build() {
      return new AnalyzerMediumTester(this);
    }

    public AnalyzerMediumTesterBuilder registerPlugin(String pluginKey, File location) {
      pluginsReferential.addPlugin(pluginKey, location);
      return this;
    }

    public AnalyzerMediumTesterBuilder registerPlugin(String pluginKey, SonarPlugin instance) {
      pluginsReferential.addPlugin(pluginKey, instance);
      return this;
    }

    public AnalyzerMediumTesterBuilder registerCoreMetrics() {
      for (Metric<?> m : CoreMetrics.getMetrics()) {
        registerMetric(m);
      }
      return this;
    }

    public AnalyzerMediumTesterBuilder registerMetric(Metric<?> metric) {
      refProvider.add(metric);
      return this;
    }

    public AnalyzerMediumTesterBuilder addQProfile(String language, String name) {
      refProvider.addQProfile(language, name);
      return this;
    }

    public AnalyzerMediumTesterBuilder addDefaultQProfile(String language, String name) {
      addQProfile(language, name);
      settingsReferential.globalSettings().put("sonar.profile." + language, name);
      return this;
    }

    public AnalyzerMediumTesterBuilder bootstrapProperties(Map<String, String> props) {
      bootstrapProperties.putAll(props);
      return this;
    }

    public AnalyzerMediumTesterBuilder activateRule(ActiveRule activeRule) {
      refProvider.addActiveRule(activeRule);
      return this;
    }

  }

  public void start() {
    batch.start();
  }

  public void stop() {
    batch.stop();
  }

  private AnalyzerMediumTester(AnalyzerMediumTesterBuilder builder) {
    batch = Batch.builder()
      .setEnableLoggingConfiguration(true)
      .addComponents(
        new EnvironmentInformation("mediumTest", "1.0"),
        builder.settingsReferential,
        builder.pluginsReferential,
        builder.refProvider,
        new DefaultDebtModel())
      .setBootstrapProperties(builder.bootstrapProperties)
      .build();
  }

  public TaskBuilder newTask() {
    return new TaskBuilder(this);
  }

  public TaskBuilder newScanTask(File sonarProps) {
    Properties prop = new Properties();
    FileReader reader = null;
    try {
      reader = new FileReader(sonarProps);
      prop.load(reader);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read configuration file", e);
    } finally {
      if (reader != null) {
        IOUtils.closeQuietly(reader);
      }
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
    private final Map<String, String> taskProperties = new HashMap<String, String>();
    private AnalyzerMediumTester tester;

    public TaskBuilder(AnalyzerMediumTester tester) {
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

  public static class TaskResult implements ScanTaskObserver {
    private List<AnalyzerIssue> issues = new ArrayList<AnalyzerIssue>();
    private List<AnalyzerMeasure> measures = new ArrayList<AnalyzerMeasure>();
    private List<InputFile> inputFiles = new ArrayList<InputFile>();

    @Override
    public void scanTaskCompleted(ProjectScanContainer container) {
      for (AnalyzerIssue issue : container.getComponentByType(AnalyzerIssueCache.class).all()) {
        issues.add(issue);
      }

      for (AnalyzerMeasure<?> measure : container.getComponentByType(AnalyzerMeasureCache.class).all()) {
        measures.add(measure);
      }

      InputFileCache inputFileCache = container.getComponentByType(InputFileCache.class);
      for (InputFile inputFile : inputFileCache.all()) {
        inputFiles.add(inputFile);
      }
    }

    public List<AnalyzerIssue> issues() {
      return issues;
    }

    public List<AnalyzerMeasure> measures() {
      return measures;
    }

    public List<InputFile> inputFiles() {
      return inputFiles;
    }

  }

  private static class FakeProjectReferentialsLoader implements ProjectReferentialsLoader {

    private ProjectReferentials ref = new ProjectReferentials();

    @Override
    public ProjectReferentials load(ProjectReactor reactor, Settings settings, Languages languages) {
      return ref;
    }

    public FakeProjectReferentialsLoader addQProfile(String language, String name) {
      ref.addQProfile(new org.sonar.batch.protocol.input.QProfile(name, name, language, new Date()));
      return this;
    }

    public FakeProjectReferentialsLoader add(Metric metric) {
      ref.metrics().add(new org.sonar.batch.protocol.input.Metric(metric.key(), metric.getType().name()));
      return this;
    }

    public FakeProjectReferentialsLoader addActiveRule(ActiveRule activeRule) {
      ref.addActiveRule(activeRule);
      return this;
    }
  }

  private static class FakeSettingsReferential implements SettingsReferential {

    private Map<String, String> globalSettings = new HashMap<String, String>();
    private Map<String, Map<String, String>> projectSettings = new HashMap<String, Map<String, String>>();

    @Override
    public Map<String, String> globalSettings() {
      return globalSettings;
    }

    @Override
    public Map<String, String> projectSettings(String projectKey) {
      return projectSettings.containsKey(projectKey) ? projectSettings.get(projectKey) : Collections.<String, String>emptyMap();
    }

  }

  private static class FackPluginsReferential implements PluginsReferential {

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

    public FackPluginsReferential addPlugin(String pluginKey, File location) {
      RemotePlugin plugin = new RemotePlugin(pluginKey, false);
      pluginList.add(plugin);
      pluginFiles.put(plugin, location);
      return this;
    }

    public FackPluginsReferential addPlugin(String pluginKey, SonarPlugin pluginInstance) {
      localPlugins.put(DefaultPluginMetadata.create(null).setKey(pluginKey), pluginInstance);
      return this;
    }

    @Override
    public Map<PluginMetadata, SonarPlugin> localPlugins() {
      return localPlugins;
    }

  }

}

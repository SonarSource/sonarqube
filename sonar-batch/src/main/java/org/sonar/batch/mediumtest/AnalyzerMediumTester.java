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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssue;
import org.sonar.api.batch.analyzer.measure.AnalyzerMeasure;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.batch.rules.QProfile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.batch.bootstrap.PluginsReferential;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.languages.Language;
import org.sonar.batch.languages.LanguagesReferential;
import org.sonar.batch.rules.QProfilesReferential;
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
import java.util.Collection;
import java.util.Collections;
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
    private final FakeSettingsReferential settingsReferential = new FakeSettingsReferential();
    private final FackPluginsReferential pluginsReferential = new FackPluginsReferential();
    private final FakeMetricFinder metricFinder = new FakeMetricFinder();
    private final FakeRuleFinder ruleFinder = new FakeRuleFinder();
    private final FakeQProfileReferential qProfileReferential = new FakeQProfileReferential();
    private final FakeLanguageReferential languageReferential = new FakeLanguageReferential();
    private final Map<String, String> bootstrapProperties = new HashMap<String, String>();
    private final RulesBuilder rulesBuilder = new RulesBuilder();
    private final ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    private int metricId = 1;

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
      metricFinder.add(metricId++, metric);
      return this;
    }

    public AnalyzerMediumTesterBuilder addQProfile(String language, String name) {
      qProfileReferential.add(new QProfile("TODO", name, language));
      return this;
    }

    public AnalyzerMediumTesterBuilder addDefaultQProfile(String language, String name) {
      qProfileReferential.add(new QProfile("TODO", name, language));
      settingsReferential.globalSettings().put("sonar.profile." + language, name);
      return this;
    }

    public AnalyzerMediumTesterBuilder registerLanguage(org.sonar.api.resources.Language... languages) {
      languageReferential.register(languages);
      return this;
    }

    public AnalyzerMediumTesterBuilder bootstrapProperties(Map<String, String> props) {
      bootstrapProperties.putAll(props);
      return this;
    }

    public AnalyzerMediumTesterBuilder activateRule(RuleKey key) {
      rulesBuilder.add(key);
      activeRulesBuilder.create(key).activate();
      return this;
    }

    public AnalyzerMediumTesterBuilder registerInactiveRule(RuleKey key) {
      rulesBuilder.add(key);
      return this;
    }

  }

  public void start() throws Throwable {
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
        builder.metricFinder,
        builder.ruleFinder,
        builder.qProfileReferential,
        builder.rulesBuilder.build(),
        builder.activeRulesBuilder.build(),
        new DefaultDebtModel(),
        builder.languageReferential)
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

    @Override
    public void scanTaskCompleted(ProjectScanContainer container) {
      for (AnalyzerIssue issue : container.getComponentByType(AnalyzerIssueCache.class).all()) {
        issues.add(issue);
      }

      for (AnalyzerMeasure<?> measure : container.getComponentByType(AnalyzerMeasureCache.class).all()) {
        measures.add(measure);
      }
    }

    public List<AnalyzerIssue> issues() {
      return issues;
    }

    public List<AnalyzerMeasure> measures() {
      return measures;
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

  private static class FakeMetricFinder implements MetricFinder {

    private Map<String, Metric> metricsByKey = Maps.newLinkedHashMap();
    private Map<Integer, Metric> metricsById = Maps.newLinkedHashMap();

    public FakeMetricFinder add(int id, Metric metric) {
      metricsByKey.put(metric.getKey(), metric);
      metricsById.put(id, metric);
      return this;
    }

    @Override
    public Metric findById(int metricId) {
      return metricsById.get(metricId);
    }

    @Override
    public Metric findByKey(String key) {
      return metricsByKey.get(key);
    }

    @Override
    public Collection<Metric> findAll(List<String> metricKeys) {
      List<Metric> result = Lists.newLinkedList();
      for (String metricKey : metricKeys) {
        Metric metric = findByKey(metricKey);
        if (metric != null) {
          result.add(metric);
        }
      }
      return result;
    }

    @Override
    public Collection<Metric> findAll() {
      return metricsByKey.values();
    }

  }

  private static class FakeRuleFinder implements RuleFinder {
    private BiMap<Integer, Rule> rulesById = HashBiMap.create();
    private Map<String, Map<String, Rule>> rulesByRepoKeyAndRuleKey = Maps.newHashMap();

    @Override
    public Rule findById(int ruleId) {
      return rulesById.get(ruleId);
    }

    @Override
    public Rule findByKey(String repositoryKey, String ruleKey) {
      Map<String, Rule> repository = rulesByRepoKeyAndRuleKey.get(repositoryKey);
      return repository != null ? repository.get(ruleKey) : null;
    }

    @Override
    public Rule findByKey(RuleKey key) {
      return findByKey(key.repository(), key.rule());
    }

    @Override
    public Rule find(RuleQuery query) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Rule> findAll(RuleQuery query) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FakeQProfileReferential implements QProfilesReferential {

    private Map<String, Map<String, QProfile>> profiles = new HashMap<String, Map<String, QProfile>>();

    @Override
    public QProfile get(String language, String name) {
      return profiles.get(language).get(name);
    }

    public void add(QProfile qprofile) {
      if (!profiles.containsKey(qprofile.language())) {
        profiles.put(qprofile.language(), new HashMap<String, QProfile>());
      }
      profiles.get(qprofile.language()).put(qprofile.name(), qprofile);
    }

  }

  private static class FakeLanguageReferential implements LanguagesReferential {

    private Map<String, Language> languages = new HashMap<String, Language>();

    public FakeLanguageReferential register(org.sonar.api.resources.Language... languages) {
      for (org.sonar.api.resources.Language language : languages) {
        this.languages.put(language.getKey(), new Language(language.getKey(), language.getName(), language.getFileSuffixes()));
      }
      return this;
    }

    @Override
    public Language get(String languageKey) {
      return languages.get(languageKey);
    }

    @Override
    public Collection<Language> all() {
      return languages.values();
    }

  }

}

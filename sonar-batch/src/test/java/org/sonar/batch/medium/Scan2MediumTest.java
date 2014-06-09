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
package org.sonar.batch.medium;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.batch.bootstrap.PluginsReferential;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.settings.SettingsReferential;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scan2MediumTest {

  @Test
  public void mediumTest() {
    Batch batch = Batch.builder()
      .setEnableLoggingConfiguration(true)
      .addComponent(new EnvironmentInformation("mediumTest", "1.0"))
      .addComponent(new MockSettingsReferential())
      .addComponent(new MockPluginsReferential())
      .addComponent(new MockMetricFinder())
      .addComponent(new MockRuleFinder())
      .setBootstrapProperties(ImmutableMap.of("sonar.analysis.mode", "sensor"))
      .build();

    batch.start();

    // batch.executeTask(ImmutableMap.<String, String>builder().put("sonar.task", "scan").build());

    batch.stop();
  }

  private static class MockSettingsReferential implements SettingsReferential {

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

  private static class MockPluginsReferential implements PluginsReferential {

    private List<RemotePlugin> pluginList = new ArrayList<RemotePlugin>();
    private Map<RemotePlugin, File> pluginFiles = new HashMap<RemotePlugin, File>();

    @Override
    public List<RemotePlugin> pluginList() {
      return pluginList;
    }

    @Override
    public File pluginFile(RemotePlugin remote) {
      return pluginFiles.get(remote);
    }

  }

  private static class MockMetricFinder implements MetricFinder {

    private Map<String, Metric> metricsByKey = Maps.newLinkedHashMap();
    private Map<Integer, Metric> metricsById = Maps.newLinkedHashMap();

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

  private static class MockRuleFinder implements RuleFinder {
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

}

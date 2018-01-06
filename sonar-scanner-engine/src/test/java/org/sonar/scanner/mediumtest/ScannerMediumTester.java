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
package org.sonar.scanner.mediumtest;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.Plugin;
import org.sonar.api.batch.debt.internal.DefaultDebtModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.bootstrapper.LogOutput;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.issue.tracking.ServerLineHashesLoader;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.MetricsRepository;
import org.sonar.scanner.repository.MetricsRepositoryLoader;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.repository.ProjectRepositoriesLoader;
import org.sonar.scanner.repository.QualityProfileLoader;
import org.sonar.scanner.repository.ServerIssuesLoader;
import org.sonar.scanner.repository.settings.SettingsLoader;
import org.sonar.scanner.rule.ActiveRulesLoader;
import org.sonar.scanner.rule.LoadedActiveRule;
import org.sonar.scanner.rule.RulesLoader;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.Rules.ListResponse.Rule;

/**
 * Main utility class for writing scanner medium tests.
 * 
 */
public class ScannerMediumTester extends ExternalResource {

  private static Path userHome = null;
  private Map<String, String> globalProperties = new HashMap<>();
  private final FakeMetricsRepositoryLoader globalRefProvider = new FakeMetricsRepositoryLoader();
  private final FakeBranchConfigurationLoader branchConfigurationLoader = new FakeBranchConfigurationLoader();
  private final FakeBranchConfiguration branchConfiguration = new FakeBranchConfiguration();
  private final FakeProjectRepositoriesLoader projectRefProvider = new FakeProjectRepositoriesLoader();
  private final FakePluginInstaller pluginInstaller = new FakePluginInstaller();
  private final FakeServerIssuesLoader serverIssues = new FakeServerIssuesLoader();
  private final FakeServerLineHashesLoader serverLineHashes = new FakeServerLineHashesLoader();
  private final FakeRulesLoader rulesLoader = new FakeRulesLoader();
  private final FakeQualityProfileLoader qualityProfiles = new FakeQualityProfileLoader();
  private final FakeActiveRulesLoader activeRules = new FakeActiveRulesLoader();
  private LogOutput logOutput = null;

  private static void createWorkingDirs() throws IOException {
    destroyWorkingDirs();

    userHome = java.nio.file.Files.createTempDirectory("mediumtest-userHome");
  }

  private static void destroyWorkingDirs() throws IOException {
    if (userHome != null) {
      FileUtils.deleteDirectory(userHome.toFile());
      userHome = null;
    }
  }

  public ScannerMediumTester setLogOutput(LogOutput logOutput) {
    this.logOutput = logOutput;
    return this;
  }

  public ScannerMediumTester registerPlugin(String pluginKey, File location) {
    return registerPlugin(pluginKey, location, 1L);
  }

  public ScannerMediumTester registerPlugin(String pluginKey, File location, long lastUpdatedAt) {
    pluginInstaller.add(pluginKey, location, lastUpdatedAt);
    return this;
  }

  public ScannerMediumTester registerPlugin(String pluginKey, Plugin instance) {
    return registerPlugin(pluginKey, instance, 1L);
  }

  public ScannerMediumTester registerPlugin(String pluginKey, Plugin instance, long lastUpdatedAt) {
    pluginInstaller.add(pluginKey, instance, lastUpdatedAt);
    return this;
  }

  public ScannerMediumTester registerCoreMetrics() {
    for (Metric<?> m : CoreMetrics.getMetrics()) {
      registerMetric(m);
    }
    return this;
  }

  public ScannerMediumTester registerMetric(Metric<?> metric) {
    globalRefProvider.add(metric);
    return this;
  }

  public ScannerMediumTester addQProfile(String language, String name) {
    qualityProfiles.add(language, name);
    return this;
  }

  public ScannerMediumTester addRule(Rule rule) {
    rulesLoader.addRule(rule);
    return this;
  }

  public ScannerMediumTester addRule(String key, String repoKey, String internalKey, String name) {
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

  public ScannerMediumTester addRules(RulesDefinition rulesDefinition) {
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

  public ScannerMediumTester addDefaultQProfile(String language, String name) {
    addQProfile(language, name);
    return this;
  }

  public ScannerMediumTester setPreviousAnalysisDate(Date previousAnalysis) {
    projectRefProvider.setLastAnalysisDate(previousAnalysis);
    return this;
  }

  public ScannerMediumTester bootstrapProperties(Map<String, String> props) {
    globalProperties.putAll(props);
    return this;
  }

  public ScannerMediumTester activateRule(LoadedActiveRule activeRule) {
    activeRules.addActiveRule(activeRule);
    return this;
  }

  public ScannerMediumTester addActiveRule(String repositoryKey, String ruleKey, @Nullable String templateRuleKey, String name, @Nullable String severity,
    @Nullable String internalKey, @Nullable String languag) {
    LoadedActiveRule r = new LoadedActiveRule();

    r.setInternalKey(internalKey);
    r.setRuleKey(RuleKey.of(repositoryKey, ruleKey));
    r.setName(name);
    r.setTemplateRuleKey(templateRuleKey);
    r.setLanguage(languag);
    r.setSeverity(severity);

    activeRules.addActiveRule(r);
    return this;
  }

  public ScannerMediumTester addFileData(String moduleKey, String path, FileData fileData) {
    projectRefProvider.addFileData(moduleKey, path, fileData);
    return this;
  }

  public ScannerMediumTester setLastBuildDate(Date d) {
    projectRefProvider.setLastAnalysisDate(d);
    return this;
  }

  public ScannerMediumTester mockServerIssue(ServerIssue issue) {
    serverIssues.getServerIssues().add(issue);
    return this;
  }

  public ScannerMediumTester mockLineHashes(String fileKey, String[] lineHashes) {
    serverLineHashes.byKey.put(fileKey, lineHashes);
    return this;
  }

  @Override
  protected void before() throws Throwable {
    try {
      createWorkingDirs();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    registerCoreMetrics();
    globalProperties.put(GlobalAnalysisMode.MEDIUM_TEST_ENABLED, "true");
    globalProperties.put(ReportPublisher.KEEP_REPORT_PROP_KEY, "true");
    globalProperties.put("sonar.userHome", userHome.toString());
  }

  @Override
  protected void after() {
    try {
      destroyWorkingDirs();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
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
    private ScannerMediumTester tester;

    public TaskBuilder(ScannerMediumTester tester) {
      this.tester = tester;
    }

    public TaskResult execute() {
      TaskResult result = new TaskResult();
      Map<String, String> props = new HashMap<>();
      props.putAll(tester.globalProperties);
      props.putAll(taskProperties);

      Batch.builder()
        .setGlobalProperties(props)
        .setEnableLoggingConfiguration(true)
        .addComponents(new EnvironmentInformation("mediumTest", "1.0"),
          tester.pluginInstaller,
          tester.globalRefProvider,
          tester.qualityProfiles,
          tester.rulesLoader,
          tester.branchConfigurationLoader,
          tester.projectRefProvider,
          tester.activeRules,
          tester.serverIssues,
          new DefaultDebtModel(),
          new FakeSettingsLoader(),
          result)
        .setLogOutput(tester.logOutput)
        .build().execute();

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

  private static class FakeRulesLoader implements RulesLoader {
    private List<org.sonarqube.ws.Rules.ListResponse.Rule> rules = new LinkedList<>();

    public FakeRulesLoader addRule(Rule rule) {
      rules.add(rule);
      return this;
    }

    @Override
    public List<Rule> load() {
      return rules;
    }
  }

  private static class FakeActiveRulesLoader implements ActiveRulesLoader {
    private List<LoadedActiveRule> activeRules = new LinkedList<>();

    public void addActiveRule(LoadedActiveRule activeRule) {
      this.activeRules.add(activeRule);
    }

    @Override
    public List<LoadedActiveRule> load(String qualityProfileKey) {
      return activeRules;
    }
  }

  private static class FakeMetricsRepositoryLoader implements MetricsRepositoryLoader {

    private int metricId = 1;

    private List<Metric> metrics = new ArrayList<>();

    @Override
    public MetricsRepository load() {
      return new MetricsRepository(metrics);
    }

    public FakeMetricsRepositoryLoader add(Metric<?> metric) {
      metric.setId(metricId++);
      metrics.add(metric);
      metricId++;
      return this;
    }

  }

  private static class FakeProjectRepositoriesLoader implements ProjectRepositoriesLoader {

    private Table<String, String, FileData> fileDataTable = HashBasedTable.create();
    private Date lastAnalysisDate;

    @Override
    public ProjectRepositories load(String projectKey, boolean isIssuesMode, @Nullable String branchBase) {
      Table<String, String, String> settings = HashBasedTable.create();
      return new ProjectRepositories(settings, fileDataTable, lastAnalysisDate);
    }

    public FakeProjectRepositoriesLoader addFileData(String moduleKey, String path, FileData fileData) {
      fileDataTable.put(moduleKey, path, fileData);
      return this;
    }

    public FakeProjectRepositoriesLoader setLastAnalysisDate(Date d) {
      lastAnalysisDate = d;
      return this;
    }

  }

  private static class FakeBranchConfiguration implements BranchConfiguration {

    private BranchType branchType = BranchType.LONG;
    private String branchName = null;
    private String branchTarget = null;
    private String branchBase = null;

    @Override
    public BranchType branchType() {
      return branchType;
    }

    @CheckForNull
    @Override
    public String branchName() {
      return branchName;
    }

    @CheckForNull
    @Override
    public String branchTarget() {
      return branchTarget;
    }

    @CheckForNull
    @Override
    public String branchBase() {
      return branchBase;
    }
  }

  public ScannerMediumTester setBranchType(BranchType branchType) {
    branchConfiguration.branchType = branchType;
    return this;
  }

  public ScannerMediumTester setBranchName(String branchName) {
    this.branchConfiguration.branchName = branchName;
    return this;
  }

  public ScannerMediumTester setBranchTarget(String branchTarget) {
    this.branchConfiguration.branchTarget = branchTarget;
    return this;
  }

  private class FakeBranchConfigurationLoader implements BranchConfigurationLoader {
    @Override
    public BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> settingsSupplier, ProjectBranches branches) {
      return branchConfiguration;
    }
  }

  private static class FakeQualityProfileLoader implements QualityProfileLoader {

    private List<QualityProfile> qualityProfiles = new LinkedList<>();

    public void add(String language, String name) {
      qualityProfiles.add(QualityProfile.newBuilder()
        .setLanguage(language)
        .setKey(name)
        .setName(name)
        .setRulesUpdatedAt(DateUtils.formatDateTime(new Date(1234567891212L)))
        .build());
    }

    @Override
    public List<QualityProfile> load(String projectKey, String profileName) {
      return qualityProfiles;
    }

    @Override
    public List<QualityProfile> loadDefault(String profileName) {
      return qualityProfiles;
    }
  }

  private static class FakeServerIssuesLoader implements ServerIssuesLoader {

    private List<ServerIssue> serverIssues = new ArrayList<>();

    public List<ServerIssue> getServerIssues() {
      return serverIssues;
    }

    @Override
    public void load(String componentKey, Consumer<ServerIssue> consumer) {
      for (ServerIssue serverIssue : serverIssues) {
        consumer.accept(serverIssue);
      }
    }
  }

  private static class FakeSettingsLoader implements SettingsLoader {

    @Override
    public Map<String, String> load(String componentKey) {
      return Collections.emptyMap();
    }
  }

  private static class FakeServerLineHashesLoader implements ServerLineHashesLoader {
    private Map<String, String[]> byKey = new HashMap<>();

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

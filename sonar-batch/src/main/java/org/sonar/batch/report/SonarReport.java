/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.batch.report;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.issue.ScanIssues;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.issue.DefaultIssue;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @since 3.6
 */

public class SonarReport implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(SonarReport.class);
  private final Settings settings;
  private final ModuleFileSystem fileSystem;
  private final Server server;
  private final RuleI18nManager ruleI18nManager;
  private final IssueCache issueCache;

  public SonarReport(Settings settings, ModuleFileSystem fileSystem, Server server, RuleI18nManager ruleI18nManager, IssueCache issueCache) {
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.server = server;
    this.ruleI18nManager = ruleI18nManager;
    this.issueCache = issueCache;
  }

  public void execute() {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      exportResults();
    }
  }

  private void exportResults() {
    File exportFile = new File(fileSystem.workingDir(), settings.getString("sonar.report.export.path"));

    LOG.info("Exporting report results to " + exportFile.getAbsolutePath());
    Writer output = null;
    try {
      output = new BufferedWriter(new FileWriter(exportFile));
      createJson().writeJSONString(output);
    } catch (IOException e) {
      throw new SonarException("Unable to write report results in file " + exportFile.getAbsolutePath(), e);
    } finally {
      Closeables.closeQuietly(output);
    }
  }

  @VisibleForTesting
  JSONObject createJson(){
    Set<RuleKey> ruleKeyList = newHashSet();
    Set<String> componentKeyList = newHashSet();

    JSONObject json = new JSONObject();
    put(json, "version", server.getVersion());

    addIssues(json, ruleKeyList, componentKeyList);
    addComponents(json, componentKeyList);
    addRules(json, ruleKeyList);
    return json;
  }

  private void addIssues(JSONObject root, Collection<RuleKey> ruleKeyList, Collection<String> componentKeyList) {
    JSONArray json = new JSONArray();
    for (DefaultIssue issue : getIssues()) {
      JSONObject jsonIssue = new JSONObject();
      put(jsonIssue, "key", issue.key());
      put(jsonIssue, "component", issue.componentKey());
      put(jsonIssue, "line", issue.line());
      put(jsonIssue, "message", issue.message());
      put(jsonIssue, "severity", issue.severity());
      put(jsonIssue, "rule", issue.ruleKey());
      put(jsonIssue, "status", issue.status());
      put(jsonIssue, "resolution", issue.resolution());
      put(jsonIssue, "isNew", issue.isNew());
      put(jsonIssue, "reporter", issue.reporter());
      put(jsonIssue, "assignee", issue.assignee());
      put(jsonIssue, "effortToFix", issue.effortToFix());
      put(jsonIssue, "creationDate", issue.creationDate());
      put(jsonIssue, "updateDate", issue.updateDate());
      put(jsonIssue, "closeDate", issue.closeDate());
      json.add(jsonIssue);

      componentKeyList.add(issue.componentKey());
      ruleKeyList.add(issue.ruleKey());
    }
    root.put("issues", json);
  }

  private void addComponents(JSONObject root, Collection<String> componentKeyList) {
    JSONArray json = new JSONArray();
    for (String componentKey : componentKeyList) {
      JSONObject jsonComponent = new JSONObject();
      // TODO add module key
      put(jsonComponent, "key", componentKey);
      json.add(jsonComponent);
    }
    root.put("components", json);
  }

  private void addRules(JSONObject root, Collection<RuleKey> ruleKeyList) {
    JSONArray json = new JSONArray();
    for (RuleKey ruleKey : ruleKeyList) {
      JSONObject jsonRuleKey = new JSONObject();
      put(jsonRuleKey, "key", ruleKey);
      put(jsonRuleKey, "rule", ruleKey.rule());
      put(jsonRuleKey, "repository", ruleKey.repository());
      put(jsonRuleKey, "name", getRuleName(ruleKey));
      json.add(jsonRuleKey);
    }
    root.put("rules", json);
  }

  private void put(JSONObject json, String key, Object value) {
    if (value != null) {
      json.put(key, value);
    }
  }

  private void put(JSONObject json, String key, RuleKey ruleKey) {
    if (ruleKey != null) {
      json.put(key, ruleKey.toString());
    }
  }

  private void put(JSONObject json, String key, Date date) {
    if (date != null) {
      json.put(key, DateUtils.formatDateTime(date));
    }
  }

  private String getRuleName(RuleKey ruleKey) {
    return ruleI18nManager.getName(ruleKey.repository(), ruleKey.rule(), Locale.getDefault());
  }

  @VisibleForTesting
  Collection<DefaultIssue> getIssues() {
    return issueCache.all();
  }
}

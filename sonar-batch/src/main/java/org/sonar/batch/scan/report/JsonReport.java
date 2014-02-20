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
package org.sonar.batch.scan.report;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.events.BatchStepEvent;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputFileCache;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @since 3.6
 */

public class JsonReport implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(JsonReport.class);
  private final Settings settings;
  private final FileSystem fileSystem;
  private final Server server;
  private final RuleFinder ruleFinder;
  private final IssueCache issueCache;
  private final EventBus eventBus;
  private final AnalysisMode analysisMode;
  private final UserFinder userFinder;
  private final InputFileCache fileCache;
  private final Project rootModule;

  public JsonReport(Settings settings, FileSystem fileSystem, Server server, RuleFinder ruleFinder, IssueCache issueCache,
                    EventBus eventBus, AnalysisMode analysisMode, UserFinder userFinder, Project rootModule, InputFileCache fileCache) {
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.server = server;
    this.ruleFinder = ruleFinder;
    this.issueCache = issueCache;
    this.eventBus = eventBus;
    this.analysisMode = analysisMode;
    this.userFinder = userFinder;
    this.rootModule = rootModule;
    this.fileCache = fileCache;
  }

  public void execute() {
    if (analysisMode.isPreview()) {
      eventBus.fireEvent(new BatchStepEvent("JSON report", true));
      exportResults();
      eventBus.fireEvent(new BatchStepEvent("JSON report", false));
    }
  }

  private void exportResults() {
    File exportFile = new File(fileSystem.workDir(), settings.getString("sonar.report.export.path"));

    LOG.info("Export results to " + exportFile.getAbsolutePath());
    Writer output = null;
    try {
      output = new BufferedWriter(new FileWriter(exportFile));
      writeJson(output);

    } catch (IOException e) {
      throw new IllegalStateException("Unable to write report results in file " + exportFile.getAbsolutePath(), e);
    } finally {
      Closeables.closeQuietly(output);
    }
  }

  @VisibleForTesting
  void writeJson(Writer writer) {
    try {
      JsonWriter json = JsonWriter.of(writer);
      json.beginObject();
      json.prop("version", server.getVersion());

      Set<RuleKey> ruleKeys = newHashSet();
      Set<String> userLogins = newHashSet();
      writeJsonIssues(json, ruleKeys, userLogins);
      writeJsonComponents(json);
      writeJsonRules(json, ruleKeys);
      List<User> users = userFinder.findByLogins(new ArrayList<String>(userLogins));
      writeUsers(json, users);
      json.endObject().close();

    } catch (IOException e) {
      throw new SonarException("Unable to write JSON report", e);
    }
  }

  private void writeJsonIssues(JsonWriter json, Set<RuleKey> ruleKeys, Set<String> logins) throws IOException {
    json.name("issues").beginArray();
    for (DefaultIssue issue : getIssues()) {
      if (issue.resolution() == null) {
        json
          .beginObject()
          .prop("key", issue.key())
          .prop("component", issue.componentKey())
          .prop("line", issue.line())
          .prop("message", issue.message())
          .prop("severity", issue.severity())
          .prop("rule", issue.ruleKey().toString())
          .prop("status", issue.status())
          .prop("resolution", issue.resolution())
          .prop("isNew", issue.isNew())
          .prop("reporter", issue.reporter())
          .prop("assignee", issue.assignee())
          .prop("effortToFix", issue.effortToFix())
          .propDateTime("creationDate", issue.creationDate())
          .propDateTime("updateDate", issue.updateDate())
          .propDateTime("closeDate", issue.closeDate());
        if (issue.reporter() != null) {
          logins.add(issue.reporter());
        }
        if (issue.assignee() != null) {
          logins.add(issue.assignee());
        }
        json.endObject();
        ruleKeys.add(issue.ruleKey());
      }
    }
    json.endArray();
  }

  private void writeJsonComponents(JsonWriter json) throws IOException {
    json.name("components").beginArray();
    // Dump modules
    writeJsonModuleComponents(json, rootModule);
    // TODO we need to dump directories
    for (InputFile inputFile : fileCache.all()) {
      String key = ((DefaultInputFile) inputFile).key();
      json
        .beginObject()
        .prop("key", key)
        .prop("path", inputFile.relativePath())
        .prop("moduleKey", StringUtils.substringBeforeLast(key, ":"))
        .prop("status", inputFile.status().name())
        .endObject();
    }
    json.endArray();
  }

  private void writeJsonModuleComponents(JsonWriter json, Project module) {
    json
      .beginObject()
      .prop("key", module.getEffectiveKey())
      .prop("path", module.getPath())
      .endObject();
    for (Project subModule : module.getModules()) {
      writeJsonModuleComponents(json, subModule);
    }
  }

  private void writeJsonRules(JsonWriter json, Set<RuleKey> ruleKeys) throws IOException {
    json.name("rules").beginArray();
    for (RuleKey ruleKey : ruleKeys) {
      json
        .beginObject()
        .prop("key", ruleKey.toString())
        .prop("rule", ruleKey.rule())
        .prop("repository", ruleKey.repository())
        .prop("name", getRuleName(ruleKey))
        .endObject();
    }
    json.endArray();
  }

  private void writeUsers(JsonWriter json, List<User> users) throws IOException {
    json.name("users").beginArray();
    for (User user : users) {
      json
        .beginObject()
        .prop("login", user.login())
        .prop("name", user.name())
        .endObject();
    }
    json.endArray();
  }

  private String getRuleName(RuleKey ruleKey) {
    Rule rule = ruleFinder.findByKey(ruleKey);
    return rule != null ? rule.getName() : null;
  }

  @VisibleForTesting
  Iterable<DefaultIssue> getIssues() {
    return issueCache.all();
  }
}

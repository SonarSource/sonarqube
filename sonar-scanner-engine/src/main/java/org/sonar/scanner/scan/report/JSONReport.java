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
package org.sonar.scanner.scan.report;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputComponentTree;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

@Properties({
  @Property(
    key = JSONReport.SONAR_REPORT_EXPORT_PATH,
    name = "Report Results Export File",
    type = PropertyType.STRING,
    global = false, project = false)})
public class JSONReport implements Reporter {

  static final String SONAR_REPORT_EXPORT_PATH = "sonar.report.export.path";
  private static final Logger LOG = LoggerFactory.getLogger(JSONReport.class);
  private final Configuration settings;
  private final FileSystem fileSystem;
  private final Server server;
  private final Rules rules;
  private final IssueCache issueCache;
  private final InputComponentStore componentStore;
  private final DefaultInputModule rootModule;
  private final InputModuleHierarchy moduleHierarchy;
  private final InputComponentTree inputComponentTree;

  public JSONReport(InputModuleHierarchy moduleHierarchy, Configuration settings, FileSystem fileSystem, Server server, Rules rules, IssueCache issueCache,
    DefaultInputModule rootModule, InputComponentStore componentStore, InputComponentTree inputComponentTree) {
    this.moduleHierarchy = moduleHierarchy;
    this.settings = settings;
    this.fileSystem = fileSystem;
    this.server = server;
    this.rules = rules;
    this.issueCache = issueCache;
    this.rootModule = rootModule;
    this.componentStore = componentStore;
    this.inputComponentTree = inputComponentTree;
  }

  @Override
  public void execute() {
    settings.get(SONAR_REPORT_EXPORT_PATH).ifPresent(this::exportResults);
  }

  private void exportResults(String exportPath) {
    File exportFile = new File(fileSystem.workDir(), exportPath);

    LOG.info("Export issues to {}", exportFile.getAbsolutePath());
    try (FileOutputStream fos = new FileOutputStream(exportFile)) {
      writeJson(new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write report results in file " + exportFile.getAbsolutePath(), e);
    }
  }

  @VisibleForTesting
  void writeJson(Writer writer) {
    try (JsonWriter json = JsonWriter.of(writer)) {
      json.beginObject();
      json.prop("version", server.getVersion());

      Set<RuleKey> ruleKeys = new LinkedHashSet<>();
      Set<String> userLogins = new LinkedHashSet<>();
      writeJsonIssues(json, ruleKeys, userLogins);
      writeJsonComponents(json);
      writeJsonRules(json, ruleKeys);
      writeUsers(json, userLogins);
      json.endObject();
    }
  }

  private void writeJsonIssues(JsonWriter json, Set<RuleKey> ruleKeys, Set<String> logins) {
    json.name("issues").beginArray();
    for (TrackedIssue issue : getIssues()) {
      if (issue.resolution() == null) {
        InputComponent component = componentStore.getByKey(issue.componentKey());
        String componentKey = getModule(component).definition().getKeyWithBranch();
        if (component instanceof InputPath) {
          componentKey = ComponentKeys.createEffectiveKey(componentKey, (InputPath) component);
        }
        json
          .beginObject()
          .prop("key", issue.key())
          .prop("component", componentKey)
          .prop("line", issue.startLine())
          .prop("startLine", issue.startLine())
          .prop("startOffset", issue.startLineOffset())
          .prop("endLine", issue.endLine())
          .prop("endOffset", issue.endLineOffset())
          .prop("message", issue.getMessage())
          .prop("severity", issue.severity())
          .prop("rule", issue.getRuleKey().toString())
          .prop("status", issue.status())
          .prop("resolution", issue.resolution())
          .prop("isNew", issue.isNew())
          .prop("assignee", issue.assignee())
          .prop("effortToFix", issue.gap())
          .propDateTime("creationDate", issue.creationDate());
        if (!StringUtils.isEmpty(issue.assignee())) {
          logins.add(issue.assignee());
        }
        json.endObject();
        ruleKeys.add(issue.getRuleKey());
      }
    }
    json.endArray();
  }

  private DefaultInputModule getModule(InputComponent component) {
    if (component.isFile()) {
      return (DefaultInputModule) inputComponentTree.getParent(inputComponentTree.getParent(component));
    }
    if (component instanceof InputDir) {
      return (DefaultInputModule) inputComponentTree.getParent(component);
    }
    return (DefaultInputModule) component;
  }

  private void writeJsonComponents(JsonWriter json) {
    json.name("components").beginArray();
    // Dump modules
    writeJsonModuleComponents(json, rootModule);
    for (DefaultInputFile inputFile : componentStore.allFilesToPublish()) {
      String moduleKey = getModule(inputFile).definition().getKeyWithBranch();
      String key = ComponentKeys.createEffectiveKey(moduleKey, inputFile);
      json
        .beginObject()
        .prop("key", key)
        .prop("path", inputFile.relativePath())
        .prop("moduleKey", moduleKey)
        .prop("status", inputFile.status().name())
        .endObject();
    }
    for (InputDir inputDir : componentStore.allDirs()) {
      String moduleKey = getModule(inputDir).definition().getKeyWithBranch();
      String key = ComponentKeys.createEffectiveKey(moduleKey, inputDir);
      json
        .beginObject()
        .prop("key", key)
        .prop("path", inputDir.relativePath())
        .prop("moduleKey", moduleKey)
        .endObject();

    }
    json.endArray();
  }

  private void writeJsonModuleComponents(JsonWriter json, DefaultInputModule module) {
    json
      .beginObject()
      .prop("key", module.definition().getKeyWithBranch())
      .prop("path", moduleHierarchy.relativePath(module))
      .endObject();
    for (DefaultInputModule subModule : moduleHierarchy.children(module)) {
      writeJsonModuleComponents(json, subModule);
    }
  }

  private void writeJsonRules(JsonWriter json, Set<RuleKey> ruleKeys) {
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

  private static void writeUsers(JsonWriter json, Collection<String> userLogins) {
    json.name("users").beginArray();

    // for compatiblity with programs that parse the json report. We no longer get the name for logins.
    for (String user : userLogins) {
      json
        .beginObject()
        .prop("login", user)
        .prop("name", user)
        .endObject();
    }
    json.endArray();
  }

  private String getRuleName(RuleKey ruleKey) {
    Rule rule = rules.find(ruleKey);
    return rule != null ? rule.name() : null;
  }

  @VisibleForTesting
  Iterable<TrackedIssue> getIssues() {
    return issueCache.all();
  }
}

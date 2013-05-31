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

package org.sonar.batch.scan;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.i18n.RuleI18nManager;

import java.io.*;
import java.util.Locale;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @since 3.6
 */

public class JsonReport implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(JsonReport.class);
  private final Settings settings;
  private final ModuleFileSystem fileSystem;
  private final Server server;
  private final RuleI18nManager ruleI18nManager;
  private final IssueCache issueCache;

  public JsonReport(Settings settings, ModuleFileSystem fileSystem, Server server, RuleI18nManager ruleI18nManager, IssueCache issueCache) {
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

    LOG.info("Export results to " + exportFile.getAbsolutePath());
    Writer output = null;
    try {
      output = new BufferedWriter(new FileWriter(exportFile));
      writeJson(output);

    } catch (IOException e) {
      throw new SonarException("Unable to write report results in file " + exportFile.getAbsolutePath(), e);
    } finally {
      Closeables.closeQuietly(output);
    }
  }

  @VisibleForTesting
  void writeJson(Writer writer) {
    JsonWriter json = null;
    try {
      json = new JsonWriter(writer);
      json.setSerializeNulls(false);
      json.beginObject();
      json.name("version").value(server.getVersion());

      Set<RuleKey> ruleKeys = newHashSet();
      Set<String> componentKeys = newHashSet();
      writeJsonIssues(json, ruleKeys, componentKeys);
      writeJsonComponents(json, componentKeys);
      writeJsonRules(json, ruleKeys);
      json.endObject().flush();

    } catch (IOException e) {
      throw new SonarException("Unable to write JSON report", e);
    } finally {
      Closeables.closeQuietly(json);
    }
  }

  private void writeJsonIssues(JsonWriter json, Set<RuleKey> ruleKeys, Set<String> componentKeys) throws IOException {
    json.name("issues").beginArray();
    for (DefaultIssue issue : getIssues()) {
      if (issue.resolution() == null) {
        json
          .beginObject()
          .name("key").value(issue.key())
          .name("component").value(issue.componentKey())
          .name("line").value(issue.line())
          .name("message").value(issue.message())
          .name("severity").value(issue.severity())
          .name("rule").value(issue.ruleKey().toString())
          .name("status").value(issue.status())
          .name("resolution").value(issue.resolution())
          .name("isNew").value(issue.isNew())
          .name("reporter").value(issue.reporter())
          .name("assignee").value(issue.assignee())
          .name("effortToFix").value(issue.effortToFix());
        if (issue.creationDate() != null) {
          json.name("creationDate").value(DateUtils.formatDateTime(issue.creationDate()));
        }
        if (issue.updateDate() != null) {
          json.name("updateDate").value(DateUtils.formatDateTime(issue.updateDate()));
        }
        if (issue.closeDate() != null) {
          json.name("closeDate").value(DateUtils.formatDateTime(issue.closeDate()));
        }
        json.endObject();
        componentKeys.add(issue.componentKey());
        ruleKeys.add(issue.ruleKey());
      }
    }
    json.endArray();
  }

  private void writeJsonComponents(JsonWriter json, Set<String> componentKeys) throws IOException {
    json.name("components").beginArray();
    for (String componentKey : componentKeys) {
      json
        .beginObject()
        .name("key").value(componentKey)
        .endObject();
    }
    json.endArray();
  }

  private void writeJsonRules(JsonWriter json, Set<RuleKey> ruleKeys) throws IOException {
    json.name("rules").beginArray();
    for (RuleKey ruleKey : ruleKeys) {
      json
        .beginObject()
        .name("key").value(ruleKey.toString())
        .name("rule").value(ruleKey.rule())
        .name("repository").value(ruleKey.repository())
        .name("name").value(getRuleName(ruleKey))
        .endObject();
    }
    json.endArray();
  }

  private String getRuleName(RuleKey ruleKey) {
    return ruleI18nManager.getName(ruleKey.repository(), ruleKey.rule(), Locale.getDefault());
  }

  @VisibleForTesting
  Iterable<DefaultIssue> getIssues() {
    return issueCache.all();
  }
}

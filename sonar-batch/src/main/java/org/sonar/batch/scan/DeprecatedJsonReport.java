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
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.issue.DefaultIssue;

import java.io.*;
import java.util.Collection;
import java.util.Locale;

/**
 * Used by Eclipse until version 3.1. Eclipse 3.2 uses issues exported by {@link org.sonar.batch.scan.JsonReport}.
 *
 * @since 3.4
 * @deprecated in 3.6. Replaced by issues.
 */
@Deprecated
public class DeprecatedJsonReport implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedJsonReport.class);

  private final Settings settings;
  private final DefaultIndex sonarIndex;
  private final ModuleFileSystem fileSystem;
  private final Server server;
  private final RuleI18nManager ruleI18nManager;
  private final IssueCache issueCache;

  public DeprecatedJsonReport(Settings settings, DefaultIndex sonarIndex, ModuleFileSystem fileSystem, Server server, RuleI18nManager ruleI18nManager, IssueCache issueCache) {
    this.settings = settings;
    this.sonarIndex = sonarIndex;
    this.fileSystem = fileSystem;
    this.server = server;
    this.ruleI18nManager = ruleI18nManager;
    this.issueCache = issueCache;
  }

  public void execute() {
    if (settings.getBoolean(CoreProperties.DRY_RUN)) {
      exportResults(sonarIndex.getResources());
    }
  }

  private void exportResults(Collection<Resource> resources) {
    File exportFile = new File(fileSystem.workingDir(), settings.getString("sonar.dryRun.export.path"));

    LOG.info("Export (deprecated) dry run results to " + exportFile.getAbsolutePath());
    Writer output = null;
    try {
      output = new BufferedWriter(new FileWriter(exportFile));
      writeJson(resources, output);
    } catch (IOException e) {
      throw new SonarException("Unable to write DryRun results in file " + exportFile.getAbsolutePath(), e);
    } finally {
      Closeables.closeQuietly(output);
    }
  }

  @VisibleForTesting
  void writeJson(Collection<Resource> resources, Writer writer) {
    JsonWriter json = null;
    try {
      json = new JsonWriter(writer);
      json.setSerializeNulls(false);

      json.beginObject()
        .name("version").value(server.getVersion())
        .name("violations_per_resource")
        .beginObject();

      for (Resource resource : resources) {
        Iterable<DefaultIssue> issues = getIssues(resource);
        boolean hasViolation = false;
        for (DefaultIssue issue : issues) {
          if (!hasViolation) {
            json.name(resource.getKey()).beginArray();
            hasViolation = true;
          }
          json
            .beginObject()
            .name("line").value(issue.line())
            .name("message").value(issue.message())
            .name("severity").value(issue.severity())
            .name("rule_key").value(issue.ruleKey().rule())
            .name("rule_repository").value(issue.ruleKey().repository())
            .name("rule_name").value(ruleName(issue.ruleKey()))
            .name("switched_off").value(Issue.RESOLUTION_FALSE_POSITIVE.equals(issue.resolution()))
            .name("is_new").value((issue.isNew()))
            .name("created_at").value(DateUtils.formatDateTime(issue.creationDate()))
            .endObject();
        }
        if (hasViolation) {
          json.endArray();
        }
      }

      json.endObject().endObject().flush();
    } catch (IOException e) {
      throw new SonarException("Unable to export results", e);
    } finally {
      Closeables.closeQuietly(json);
    }
  }

  private String ruleName(RuleKey key) {
    return ruleI18nManager.getName(key.repository(), key.rule(), Locale.getDefault());
  }

  @VisibleForTesting
  Iterable<DefaultIssue> getIssues(Resource resource) {
    return issueCache.byComponent(resource.getEffectiveKey());
  }
}

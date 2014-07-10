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
package org.sonar.server.qualityprofile;

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QProfileExporters implements ServerComponent {

  private final QProfileLoader loader;
  private final RuleFinder ruleFinder;
  private final ProfileExporter[] exporters;

  public QProfileExporters(QProfileLoader loader, RuleFinder ruleFinder, ProfileExporter[] exporters) {
    this.loader = loader;
    this.ruleFinder = ruleFinder;
    this.exporters = exporters;
  }

  public QProfileExporters(QProfileLoader loader, RuleFinder ruleFinder) {
    this(loader, ruleFinder, new ProfileExporter[0]);
  }

  public List<ProfileExporter> exportersForLanguage(String language) {
    List<ProfileExporter> result = new ArrayList<ProfileExporter>();
    for (ProfileExporter exporter : exporters) {
      if (exporter.getSupportedLanguages() == null || exporter.getSupportedLanguages().length == 0 || ArrayUtils.contains(exporter.getSupportedLanguages(), language)) {
        result.add(exporter);
      }
    }
    return result;
  }

  public String mimeType(String exporterKey) {
    ProfileExporter exporter = findExporter(exporterKey);
    return exporter.getMimeType();
  }

  public void export(String profileKey, String exporterKey, Writer writer) {
    ProfileExporter exporter = findExporter(exporterKey);
    QualityProfileDto profile = loader.getByKey(profileKey);
    if (profile == null) {
      throw new NotFoundException("Unknown Quality profile: " + profileKey);
    }
    exporter.exportProfile(wrap(profile), writer);
  }

  /**
   * Only for ruby on rails
   */
  public String export(String profileKey, String tool) {
    StringWriter writer = new StringWriter();
    export(profileKey, tool, writer);
    return writer.toString();
  }

  private RulesProfile wrap(QualityProfileDto profile) {
    RulesProfile target = new RulesProfile(profile.getName(), profile.getLanguage());
    for (ActiveRule activeRule : loader.findActiveRulesByProfile(profile.getKey())) {
      Rule rule = ruleFinder.findByKey(activeRule.key().ruleKey());
      org.sonar.api.rules.ActiveRule wrappedActiveRule = target.activateRule(rule, RulePriority.valueOf(activeRule.severity()));
      for (Map.Entry<String, String> entry : activeRule.params().entrySet()) {
        wrappedActiveRule.setParameter(entry.getKey(), entry.getValue());
      }
    }
    return target;
  }

  private ProfileExporter findExporter(String exporterKey) {
    for (ProfileExporter e : exporters) {
      if (exporterKey.equals(e.getKey())) {
        return e;
      }
    }
    throw new NotFoundException("Unknown quality profile exporter: " + exporterKey);
  }
}

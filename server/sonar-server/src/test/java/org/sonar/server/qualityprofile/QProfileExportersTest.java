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

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.ServerTester;

import java.io.IOException;
import java.io.Writer;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfileExportersTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo().addComponents(
    XooRulesDefinition.class, XooProfileDefinition.class,
    XooExporter.class, StandardExporter.class);

  QProfileExporters exporters = tester.get(QProfileExporters.class);

  @Test
  public void exportersForLanguage() throws Exception {
    assertThat(exporters.exportersForLanguage("xoo")).hasSize(2);
    assertThat(exporters.exportersForLanguage("java")).hasSize(1);
    assertThat(exporters.exportersForLanguage("java").get(0)).isInstanceOf(StandardExporter.class);
  }

  @Test
  public void mimeType() throws Exception {
    assertThat(exporters.mimeType("xootool")).isEqualTo("plain/custom");

    // default mime type
    assertThat(exporters.mimeType("standard")).isEqualTo("plain/text");
  }

  @Test
  public void export() {
    QualityProfileDto profile = tester.get(QProfileLoader.class).getByLangAndName("xoo", "P1");
    assertThat(exporters.export(profile.getKey(), "xootool")).isEqualTo("xoo -> P1 -> 1");
    assertThat(exporters.export(profile.getKey(), "standard")).isEqualTo("standard -> P1 -> 1");
  }

  @Test
  public void fail_if_missing_exporter() {
    QualityProfileDto profile = tester.get(QProfileLoader.class).getByLangAndName("xoo", "P1");
    try {
      exporters.export(profile.getKey(), "unknown");
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Unknown quality profile exporter: unknown");
    }
  }

  @Test
  public void fail_if_missing_profile() {
    try {
      exporters.export("unknown", "xootool");
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Unknown Quality profile: unknown");
    }
  }

  public static class XooExporter extends ProfileExporter {
    public XooExporter() {
      super("xootool", "Xoo Tool");
    }

    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"xoo"};
    }

    @Override
    public String getMimeType() {
      return "plain/custom";
    }

    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
      try {
        writer.write("xoo -> " + profile.getName() + " -> " + profile.getActiveRules().size());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public static class StandardExporter extends ProfileExporter {
    public StandardExporter() {
      super("standard", "Standard");
    }

    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
      try {
        writer.write("standard -> " + profile.getName() + " -> " + profile.getActiveRules().size());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      NewRule x1 = repository.createRule("R1")
        .setName("R1 name")
        .setHtmlDescription("R1 desc")
        .setSeverity(Severity.MINOR);
      x1.createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");
      repository.done();
    }
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      RulesProfile profile = RulesProfile.create("P1", "xoo");
      profile.activateRule(new Rule("xoo", "R1"), RulePriority.BLOCKER).setParameter("acceptWhitespace", "true");
      return profile;
    }
  }

}

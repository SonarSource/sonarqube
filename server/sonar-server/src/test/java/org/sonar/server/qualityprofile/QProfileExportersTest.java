/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.qualityprofile;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class QProfileExportersTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .withEsIndexes()
    .withStartupTasks()
    .addXoo()
    .addComponents(XooRulesDefinition.class, XooProfileDefinition.class, XooExporter.class, StandardExporter.class,
      XooProfileImporter.class, XooProfileImporterWithMessages.class, XooProfileImporterWithError.class);

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  QProfileExporters exporters;
  QProfileLoader loader;
  ActiveRuleIndexer activeRuleIndexer;

  @Before
  public void before() {
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    exporters = tester.get(QProfileExporters.class);
    loader = tester.get(QProfileLoader.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    activeRuleIndexer.setEnabled(true);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void exportersForLanguage() {
    assertThat(exporters.exportersForLanguage("xoo")).hasSize(2);
    assertThat(exporters.exportersForLanguage("java")).hasSize(1);
    assertThat(exporters.exportersForLanguage("java").get(0)).isInstanceOf(StandardExporter.class);
  }

  @Test
  public void mimeType() {
    assertThat(exporters.mimeType("xootool")).isEqualTo("plain/custom");

    // default mime type
    assertThat(exporters.mimeType("standard")).isEqualTo("text/plain");
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

  @Test
  public void profile_importers_for_language() {
    assertThat(exporters.findProfileImportersForLanguage("xoo")).hasSize(3);
  }

  @Test
  public void import_xml() {
    QualityProfileDto profileDto = QProfileTesting.newQProfileDto(QProfileName.createFor("xoo", "import_xml"), "import_xml");
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();

    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, profileDto.getKey())).isEmpty();

    QProfileResult result = exporters.importXml(profileDto, "XooProfileImporter", "<xml/>", dbSession);
    dbSession.commit();
    activeRuleIndexer.index(result.getChanges());

    // Check in db
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, profileDto.getKey());
    assertThat(activeRules).hasSize(1);
    ActiveRuleDto activeRule = activeRules.get(0);
    assertThat(activeRule.getKey().ruleKey()).isEqualTo(RuleKey.of("xoo", "R1"));
    assertThat(activeRule.getSeverityString()).isEqualTo(Severity.CRITICAL);

    // Check in es
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery().setQProfileKey(profileDto.getKey()).setActivation(true))).containsOnly(RuleKey.of("xoo", "R1"));
  }

  @Test
  public void import_xml_return_messages() {
    QProfileResult result = exporters.importXml(QProfileTesting.newXooP1(), "XooProfileImporterWithMessages", "<xml/>", dbSession);
    dbSession.commit();

    assertThat(result.infos()).containsOnly("an info");
    assertThat(result.warnings()).containsOnly("a warning");
  }

  @Test
  public void fail_to_import_xml_when_error_in_importer() {
    try {
      exporters.importXml(QProfileTesting.newXooP1(), "XooProfileImporterWithError", "<xml/>", dbSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("error!");
    }
  }

  @Test
  public void fail_to_import_xml_on_unknown_importer() {
    try {
      exporters.importXml(QProfileTesting.newXooP1(), "Unknown", "<xml/>", dbSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("No such importer : Unknown");
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

  public static class XooProfileImporter extends ProfileImporter {
    public XooProfileImporter() {
      super("XooProfileImporter", "Xoo Profile Importer");
    }

    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"xoo"};
    }

    @Override
    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
      RulesProfile rulesProfile = RulesProfile.create();
      rulesProfile.activateRule(Rule.create("xoo", "R1"), RulePriority.CRITICAL);
      return rulesProfile;
    }
  }

  public static class XooProfileImporterWithMessages extends ProfileImporter {
    public XooProfileImporterWithMessages() {
      super("XooProfileImporterWithMessages", "Xoo Profile Importer With Message");
    }

    @Override
    public String[] getSupportedLanguages() {
      return new String[] {};
    }

    @Override
    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
      messages.addWarningText("a warning");
      messages.addInfoText("an info");
      return RulesProfile.create();
    }
  }

  public static class XooProfileImporterWithError extends ProfileImporter {
    public XooProfileImporterWithError() {
      super("XooProfileImporterWithError", "Xoo Profile Importer With Error");
    }

    @Override
    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
      messages.addErrorText("error!");
      return RulesProfile.create();
    }
  }

}

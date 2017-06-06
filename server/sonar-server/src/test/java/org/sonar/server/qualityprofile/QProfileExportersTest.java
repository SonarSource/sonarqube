/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

public class QProfileExportersTest {

//  @ClassRule
//  public static ServerTester tester = new ServerTester()
//    .withEsIndexes()
//    .withStartupTasks()
//    .addXoo()
//    .addComponents(XooRulesDefinition.class, XooProfileDefinition.class, XooExporter.class, StandardExporter.class,
//      XooProfileImporter.class, XooProfileImporterWithMessages.class, XooProfileImporterWithError.class);
//
//  @org.junit.Rule
//  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);
//
//  private DbClient db;
//  private DbSession dbSession;
//  private QProfileExporters underTest;
//  private ActiveRuleIndexer activeRuleIndexer;
//
//  @Before
//  public void before() {
//    db = tester.get(DbClient.class);
//    dbSession = db.openSession(false);
//    underTest = tester.get(QProfileExporters.class);
//    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
//  }
//
//  @After
//  public void after() {
//    dbSession.close();
//  }
//
//  @Test
//  public void exportersForLanguage() {
//    assertThat(underTest.exportersForLanguage("xoo")).hasSize(2);
//    assertThat(underTest.exportersForLanguage("java")).hasSize(1);
//    assertThat(underTest.exportersForLanguage("java").get(0)).isInstanceOf(StandardExporter.class);
//  }
//
//  @Test
//  public void mimeType() {
//    assertThat(underTest.mimeType("xootool")).isEqualTo("plain/custom");
//
//    // default mime type
//    assertThat(underTest.mimeType("standard")).isEqualTo("text/plain");
//  }
//
//  @Test
//  public void import_xml() {
//    QProfileDto profile = QProfileTesting.newQProfileDto("org-123", QProfileName.createFor("xoo", "import_xml"), "import_xml");
//    db.qualityProfileDao().insert(dbSession, profile);
//    dbSession.commit();
//
//    assertThat(db.activeRuleDao().selectByProfile(dbSession, profile)).isEmpty();
//
//    QProfileResult result = underTest.importXml(profile, "XooProfileImporter", toInputStream("<xml/>", UTF_8), dbSession);
//    dbSession.commit();
//    activeRuleIndexer.index(result.getChanges());
//
//    // Check in db
//    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee());
//    assertThat(activeRules).hasSize(1);
//    ActiveRuleDto activeRule = activeRules.get(0);
//    assertThat(activeRule.getKey().getRuleKey()).isEqualTo(RuleKey.of("xoo", "R1"));
//    assertThat(activeRule.getSeverityString()).isEqualTo(Severity.CRITICAL);
//
//    // Check in es
//    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery().setQProfile(profile).setActivation(true))).containsOnly(RuleKey.of("xoo", "R1"));
//  }
//
//  @Test
//  public void import_xml_return_messages() {
//    QProfileResult result = underTest.importXml(QProfileTesting.newXooP1("org-123"), "XooProfileImporterWithMessages", toInputStream("<xml/>", UTF_8), dbSession);
//    dbSession.commit();
//
//    assertThat(result.infos()).containsOnly("an info");
//    assertThat(result.warnings()).containsOnly("a warning");
//  }
//
//  @Test
//  public void fail_to_import_xml_when_error_in_importer() {
//    try {
//      underTest.importXml(QProfileTesting.newXooP1("org-123"), "XooProfileImporterWithError", toInputStream("<xml/>", UTF_8), dbSession);
//      fail();
//    } catch (Exception e) {
//      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("error!");
//    }
//  }
//
//  @Test
//  public void fail_to_import_xml_on_unknown_importer() {
//    try {
//      underTest.importXml(QProfileTesting.newXooP1("org-123"), "Unknown", toInputStream("<xml/>", UTF_8), dbSession);
//      fail();
//    } catch (Exception e) {
//      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("No such importer : Unknown");
//    }
//  }
//
//  public static class XooExporter extends ProfileExporter {
//    public XooExporter() {
//      super("xootool", "Xoo Tool");
//    }
//
//    @Override
//    public String[] getSupportedLanguages() {
//      return new String[] {"xoo"};
//    }
//
//    @Override
//    public String getMimeType() {
//      return "plain/custom";
//    }
//
//    @Override
//    public void exportProfile(RulesProfile profile, Writer writer) {
//      try {
//        writer.write("xoo -> " + profile.getName() + " -> " + profile.getActiveRules().size());
//      } catch (IOException e) {
//        throw new IllegalStateException(e);
//      }
//    }
//  }
//
//  public static class StandardExporter extends ProfileExporter {
//    public StandardExporter() {
//      super("standard", "Standard");
//    }
//
//    @Override
//    public void exportProfile(RulesProfile profile, Writer writer) {
//      try {
//        writer.write("standard -> " + profile.getName() + " -> " + profile.getActiveRules().size());
//      } catch (IOException e) {
//        throw new IllegalStateException(e);
//      }
//    }
//  }
//
//  public static class XooRulesDefinition implements RulesDefinition {
//    @Override
//    public void define(Context context) {
//      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
//      NewRule x1 = repository.createRule("R1")
//        .setName("R1 name")
//        .setHtmlDescription("R1 desc")
//        .setSeverity(Severity.MINOR);
//      x1.createParam("acceptWhitespace")
//        .setDefaultValue("false")
//        .setType(RuleParamType.BOOLEAN)
//        .setDescription("Accept whitespaces on the line");
//      repository.done();
//    }
//  }
//
//  public static class XooProfileDefinition extends ProfileDefinition {
//    @Override
//    public RulesProfile createProfile(ValidationMessages validation) {
//      RulesProfile profile = RulesProfile.create("P1", "xoo");
//      profile.activateRule(new Rule("xoo", "R1"), RulePriority.BLOCKER).setParameter("acceptWhitespace", "true");
//      return profile;
//    }
//  }
//
//  public static class XooProfileImporter extends ProfileImporter {
//    public XooProfileImporter() {
//      super("XooProfileImporter", "Xoo Profile Importer");
//    }
//
//    @Override
//    public String[] getSupportedLanguages() {
//      return new String[] {"xoo"};
//    }
//
//    @Override
//    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
//      RulesProfile rulesProfile = RulesProfile.create();
//      rulesProfile.activateRule(Rule.create("xoo", "R1"), RulePriority.CRITICAL);
//      return rulesProfile;
//    }
//  }
//
//  public static class XooProfileImporterWithMessages extends ProfileImporter {
//    public XooProfileImporterWithMessages() {
//      super("XooProfileImporterWithMessages", "Xoo Profile Importer With Message");
//    }
//
//    @Override
//    public String[] getSupportedLanguages() {
//      return new String[] {};
//    }
//
//    @Override
//    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
//      messages.addWarningText("a warning");
//      messages.addInfoText("an info");
//      return RulesProfile.create();
//    }
//  }
//
//  public static class XooProfileImporterWithError extends ProfileImporter {
//    public XooProfileImporterWithError() {
//      super("XooProfileImporterWithError", "Xoo Profile Importer With Error");
//    }
//
//    @Override
//    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
//      messages.addErrorText("error!");
//      return RulesProfile.create();
//    }
//  }

}

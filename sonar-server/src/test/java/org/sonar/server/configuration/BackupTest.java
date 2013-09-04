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
package org.sonar.server.configuration;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BackupTest {

  @Test
  public void shouldExportXml() throws Exception {
    SonarConfig sonarConfig = getSonarConfig();
    Backupable backupable = mock(Backupable.class);
    Backup backup = new Backup(Arrays.asList(backupable));

    String xml = backup.exportXml(sonarConfig);
    verify(backupable).exportXml(sonarConfig);
    assertXmlAreSimilar(xml, "backup-empty.xml");
  }

  @Test
  public void shouldReturnAValidXml() throws Exception {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null),
      new RulesBackup((DatabaseSession) null), new ProfilesBackup((DatabaseSession) null)));
    SonarConfig sonarConfig = getSonarConfig();
    sonarConfig.setMetrics(getMetrics());
    sonarConfig.setProperties(getProperties());
    sonarConfig.setProfiles(getProfiles());
    sonarConfig.setRules(getUserRules());

    String xml = backup.getXmlFromSonarConfig(sonarConfig);
    assertXmlAreSimilar(xml, "backup-valid.xml");
  }

  @Test
  public void shouldExportXmlInCdata() throws Exception {
    SonarConfig sonarConfig = getSonarConfig();
    sonarConfig.setProperties(getPropertiesWithXmlIlliciteCharacters());
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = backup.getXmlFromSonarConfig(sonarConfig);
    assertXmlAreSimilar(xml, "backup-with-cdata.xml");
  }

  @Test
  public void shouldExportXmlWithUtf8Characters() throws Exception {
    SonarConfig sonarConfig = getSonarConfig();
    sonarConfig.setProperties(getPropertiesWithUtf8Characters());
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = backup.getXmlFromSonarConfig(sonarConfig);
    assertXmlAreSimilar(xml, "backup-with-utf8-char.xml");
  }

  @Test
  public void shouldImportXml() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null),
      new RulesBackup((DatabaseSession) null), new ProfilesBackup((DatabaseSession) null)));

    String xml = getFileFromClasspath("backup-restore-valid.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertThat(sonarConfig.getMetrics()).isEqualTo(getMetrics());
    assertThat(sonarConfig.getProperties()).isEqualTo(getProperties());
    for (Metric metric : sonarConfig.getMetrics()) {
      assertThat(metric.getEnabled()).isNotNull();
      assertThat(metric.getEnabled()).isTrue();
      assertThat(metric.getUserManaged()).isNotNull();
      assertThat(metric.getUserManaged()).isTrue();
    }

    Collection<RulesProfile> profiles = sonarConfig.getProfiles();
    assertThat(profiles).hasSize(2);

    Iterator<RulesProfile> profilesIter = profiles.iterator();
    RulesProfile testProfile = profilesIter.next();
    assertThat("test name").isEqualTo(testProfile.getName());
    assertThat(testProfile.getDefaultProfile()).isTrue();
    assertThat("test language").isEqualTo(testProfile.getLanguage());
    assertThat(testProfile.getActiveRules()).hasSize(1);

    ActiveRule testActiveRule = testProfile.getActiveRules().get(0);
    assertThat(RulePriority.MAJOR).isEqualTo(testActiveRule.getSeverity());
    assertThat(testActiveRule.getRule()).isNotNull();
    assertThat("test key").isEqualTo(testActiveRule.getRule().getKey());
    assertThat("test plugin").isEqualTo(testActiveRule.getRule().getRepositoryKey());
    assertThat(testActiveRule.getInheritance()).isNull();
    assertThat(testActiveRule.getActiveRuleParams()).hasSize(1);

    ActiveRuleParam testActiveRuleParam = testActiveRule.getActiveRuleParams().get(0);
    assertThat("test value").isEqualTo(testActiveRuleParam.getValue());
    assertThat(testActiveRuleParam.getRuleParam()).isNotNull();
    assertThat("test param key").isEqualTo(testActiveRuleParam.getRuleParam().getKey());

    assertThat(testProfile.getAlerts()).hasSize(2);
    Alert testAlert = testProfile.getAlerts().get(0);
    assertThat(Alert.OPERATOR_GREATER).isEqualTo(testAlert.getOperator());
    assertThat("testError").isEqualTo(testAlert.getValueError());
    assertThat("testWarn").isEqualTo(testAlert.getValueWarning());
    assertThat(testAlert.getPeriod()).isNull();
    assertThat(testAlert.getMetric()).isNotNull();
    assertThat("test key").isEqualTo(testAlert.getMetric().getKey());

    Alert testAlert2 = testProfile.getAlerts().get(1);
    assertThat(Alert.OPERATOR_SMALLER).isEqualTo(testAlert2.getOperator());
    assertThat("testError2").isEqualTo(testAlert2.getValueError());
    assertThat("testWarn2").isEqualTo(testAlert2.getValueWarning());
    assertThat(testAlert2.getPeriod()).isEqualTo(1);
    assertThat(testAlert2.getMetric()).isNotNull();
    assertThat("test key2").isEqualTo(testAlert2.getMetric().getKey());

    // Child profile
    testProfile = profilesIter.next();
    assertThat("test2 name").isEqualTo(testProfile.getName());
    assertThat("test name").isEqualTo(testProfile.getParentName());
    testActiveRule = testProfile.getActiveRules().get(0);
    assertThat(testActiveRule.getInheritance()).isEqualTo(ActiveRule.OVERRIDES);

    Collection<Rule> rules = sonarConfig.getRules();
    assertThat(rules).hasSize(1);

    Rule rule = rules.iterator().next();
    assertThat(rule.getParent().getRepositoryKey()).isEqualTo("test plugin");
    assertThat(rule.getParent().getKey()).isEqualTo("test key");
    assertThat(rule.getRepositoryKey()).isEqualTo("test plugin");
    assertThat(rule.getKey()).isEqualTo("test key2");
    assertThat(rule.getConfigKey()).isEqualTo("test config key");
    assertThat(rule.getName()).isEqualTo("test name");
    assertThat(rule.getDescription()).isEqualTo("test description");
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.INFO);
    assertThat(rule.getParams()).hasSize(1);

    RuleParam param = rule.getParams().get(0);
    assertThat(param.getKey()).isEqualTo("test param key");
    assertThat(param.getDefaultValue()).isEqualTo("test param value");
  }

  @Test
  public void shouldImportXmlWithoutInheritanceInformation() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null),
      new RulesBackup((DatabaseSession) null), new ProfilesBackup((DatabaseSession) null)));

    String xml = getFileFromClasspath("backup-restore-without-inheritance.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    Collection<RulesProfile> profiles = sonarConfig.getProfiles();
    assertThat(profiles).hasSize(1);
    RulesProfile testProfile = profiles.iterator().next();
    assertThat(testProfile.getActiveRules()).hasSize(1);
    ActiveRule activeRule = testProfile.getActiveRules().get(0);
    assertThat(activeRule.getInheritance()).isNull();
  }

  @Test
  public void shouldImportXmlWithXmlIlliciteCharacters() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-cdata.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertThat(sonarConfig.getProperties()).isEqualTo(getPropertiesWithXmlIlliciteCharacters());
  }

  @Test
  public void shouldImportOneDotFiveFormat() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("shouldImportOneDotFiveFormat.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertThat(sonarConfig.getMetrics()).hasSize(1);
    assertThat(sonarConfig.getProfiles()).isNull();
    assertThat(sonarConfig.getProperties()).hasSize(2);
  }

  @Test
  public void shouldImportXmlWithUtf8Character() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-utf8-char.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertThat(sonarConfig.getProperties()).isEqualTo(getPropertiesWithUtf8Characters());
  }

  @Test
  public void shouldNotImportMetricIds() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-id-for-metrics.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    Metric metric = sonarConfig.getMetrics().iterator().next();
    assertThat(metric.getId()).isNull();
  }

  @Test
  public void shouldExportAndImportInnerCDATA() throws Exception {
    SonarConfig sonarConfig = getSonarConfig();
    sonarConfig.setProperties(getPropertiesWithCDATA());

    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));
    String xml = backup.getXmlFromSonarConfig(sonarConfig);
    assertXmlAreSimilar(xml, "backup-with-splitted-cdata.xml");

    sonarConfig = backup.getSonarConfigFromXml(xml);
    assertThat(sonarConfig.getProperties()).isEqualTo(getPropertiesWithCDATA());
  }

  private SonarConfig getSonarConfig() throws ParseException {
    DateFormat dateFormat = new SimpleDateFormat(Backup.DATE_FORMAT);
    Date date = dateFormat.parse("2008-11-18");
    return new SonarConfig(54, date);
  }

  private List<Metric> getMetrics() {
    List<Metric> metrics = new ArrayList<Metric>();

    Metric m1 = new Metric("metric1");
    m1.setEnabled(true);
    m1.setOrigin(Metric.Origin.GUI);

    Metric m2 = new Metric("metric2");
    m2.setEnabled(true);
    m2.setOrigin(Metric.Origin.WS);

    metrics.add(m1);
    metrics.add(m2);

    return metrics;
  }

  private List<RulesProfile> getProfiles() {
    List<RulesProfile> profiles = new ArrayList<RulesProfile>();

    Rule rule = Rule.create("test plugin", "test key", null);
    rule.createParameter("test param key");

    RulesProfile profile1 = RulesProfile.create("test name", "test language");
    profile1.setDefaultProfile(true);
    profile1.setProvided(true);
    profiles.add(profile1);

    ActiveRule activeRule = profile1.activateRule(rule, RulePriority.MAJOR);
    activeRule.setParameter("test param key", "test value");

    RulesProfile profile2 = RulesProfile.create("test2 name", "test language");
    profile2.setParentName(profile1.getName());
    profiles.add(profile2);

    ActiveRule activeRule2 = profile2.activateRule(rule, RulePriority.MINOR);
    activeRule2.setParameter("test param key", "test value");
    activeRule2.setInheritance(ActiveRule.OVERRIDES);

    Alert alert1 = new Alert(null, new Metric("test key"), Alert.OPERATOR_GREATER, "testError", "testWarn");
    Alert alert2 = new Alert(null, new Metric("test key2"), Alert.OPERATOR_SMALLER, "testError2", "testWarn2", 1);

    List<Alert> alerts = profiles.get(0).getAlerts();
    alerts.add(alert1);
    alerts.add(alert2);

    return profiles;
  }

  private List<Rule> getUserRules() {
    List<Rule> rules = Lists.newArrayList();
    Rule parentRule = Rule.create("test plugin", "test key", null);
    Rule rule = Rule.create("test plugin", "test key2", "test name")
      .setDescription("test description")
      .setConfigKey("test config key")
      .setSeverity(RulePriority.INFO)
      .setParent(parentRule);
    rule.createParameter().setKey("test param key").setDefaultValue("test param value");
    rules.add(rule);
    return rules;
  }

  private List<Property> getProperties() {
    List<Property> properties = new ArrayList<Property>();
    properties.add(new Property("key1", "value1"));
    properties.add(new Property("key2", "value2"));
    return properties;
  }

  private List<Property> getPropertiesWithCDATA() {
    List<Property> properties = new ArrayList<Property>();
    properties.add(new Property("key1", "<![CDATA[value1]]>"));
    properties.add(new Property("key2", "]]>value2"));
    properties.add(new Property("key3", "prefix]]>value3"));
    properties.add(new Property("key4", "<name><![CDATA[Forges]]></name>"));
    return properties;
  }

  private List<Property> getPropertiesWithXmlIlliciteCharacters() {
    List<Property> properties = new ArrayList<Property>();
    properties.add(new Property("key", "<value>"));
    return properties;
  }

  private List<Property> getPropertiesWithUtf8Characters() {
    List<Property> properties = new ArrayList<Property>();
    properties.add(new Property("key", "\u00E9"));
    return properties;
  }

  private void assertXmlAreSimilar(String xml, String xmlExpected) {
    try {
      XMLUnit.setIgnoreWhitespace(true);
      Diff diff = XMLUnit.compareXML(getFileFromClasspath(xmlExpected), xml);
      assertThat(diff.similar()).as(diff.toString()).isTrue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getFileFromClasspath(String file) {
    InputStream input = null;
    try {
      input = getClass().getClassLoader().getResourceAsStream("org/sonar/server/configuration/BackupTest/" + file);
      return IOUtils.toString(input, CharEncoding.UTF_8);

    } catch (IOException e) {
      throw new RuntimeException(e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

}

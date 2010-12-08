/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
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
import org.sonar.api.rules.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getMetrics(), getMetrics()));
    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), getProperties()));
    for (Metric metric : sonarConfig.getMetrics()) {
      assertNotNull(metric.getEnabled());
      assertTrue(metric.getEnabled());
      assertNotNull(metric.getUserManaged());
      assertTrue(metric.getUserManaged());
    }

    Collection<RulesProfile> profiles = sonarConfig.getProfiles();
    assertEquals(2, profiles.size());

    RulesProfile testProfile = profiles.iterator().next();
    assertEquals("test name", testProfile.getName());
    assertEquals(true, testProfile.getDefaultProfile());
    assertEquals("test language", testProfile.getLanguage());
    assertEquals(1, testProfile.getActiveRules().size());

    ActiveRule testActiveRule = testProfile.getActiveRules().get(0);
    assertEquals(RulePriority.MAJOR, testActiveRule.getSeverity());
    assertNotNull(testActiveRule.getRule());
    assertEquals("test key", testActiveRule.getRule().getKey());
    assertEquals("test plugin", testActiveRule.getRule().getRepositoryKey());
    assertEquals(1, testActiveRule.getActiveRuleParams().size());

    ActiveRuleParam testActiveRuleParam = testActiveRule.getActiveRuleParams().get(0);
    assertEquals("test value", testActiveRuleParam.getValue());
    assertNotNull(testActiveRuleParam.getRuleParam());
    assertEquals("test param key", testActiveRuleParam.getRuleParam().getKey());

    assertEquals(1, testProfile.getAlerts().size());
    Alert testAlert = testProfile.getAlerts().get(0);
    assertEquals(Alert.OPERATOR_GREATER, testAlert.getOperator());
    assertEquals("testError", testAlert.getValueError());
    assertEquals("testWarn", testAlert.getValueWarning());
    assertNotNull(testAlert.getMetric());
    assertEquals("test key", testAlert.getMetric().getKey());

    Collection<Rule> rules = sonarConfig.getRules();
    assertThat(rules.size(), is(1));
    Rule rule = rules.iterator().next();
    assertThat(rule.getParent().getRepositoryKey(), is("test plugin"));
    assertThat(rule.getParent().getKey(), is("test key"));
    assertThat(rule.getRepositoryKey(), is("test plugin"));
    assertThat(rule.getKey(), is("test key2"));
    assertThat(rule.getName(), is("test name"));
    assertThat(rule.getDescription(), is("test description"));
    assertThat(rule.getSeverity(), is(RulePriority.INFO));
    assertThat(rule.getParams().size(), is(1));
    RuleParam param = rule.getParams().get(0);
    assertThat(param.getKey(), is("test param key"));
    assertThat(param.getDefaultValue(), is("test param value"));
  }

  @Test
  public void shouldImportXmlWithXmlIlliciteCharacters() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-cdata.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), getPropertiesWithXmlIlliciteCharacters()));
  }

  @Test
  public void shouldImportOneDotFiveFormat() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("shouldImportOneDotFiveFormat.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertEquals(1, sonarConfig.getMetrics().size());
    assertTrue(CollectionUtils.isEmpty(sonarConfig.getProfiles()));
    assertEquals(2, sonarConfig.getProperties().size());
  }

  @Test
  public void shouldImportXmlWithUtf8Character() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-utf8-char.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), getPropertiesWithUtf8Characters()));
  }

  @Test
  public void shouldNotImportMetricIds() {
    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));

    String xml = getFileFromClasspath("backup-with-id-for-metrics.xml");
    SonarConfig sonarConfig = backup.getSonarConfigFromXml(xml);

    Metric metric = sonarConfig.getMetrics().iterator().next();
    assertThat(metric.getId(), nullValue());
  }

  @Test
  public void shouldExportAndImportInnerCDATA() throws Exception {
    SonarConfig sonarConfig = getSonarConfig();
    sonarConfig.setProperties(getPropertiesWithCDATA());

    Backup backup = new Backup(Arrays.asList(new MetricsBackup(null), new PropertiesBackup(null)));
    String xml = backup.getXmlFromSonarConfig(sonarConfig);
    assertXmlAreSimilar(xml, "backup-with-splitted-cdata.xml");

    sonarConfig = backup.getSonarConfigFromXml(xml);
    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), getPropertiesWithCDATA()));
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

    RulesProfile profile1 = RulesProfile.create("test name", "test language");
    profile1.setDefaultProfile(true);
    profile1.setProvided(true);
    profiles.add(profile1);

    RulesProfile profile2 = RulesProfile.create("test2 name", "test2 language");
    profiles.add(profile2);

    Rule rule = Rule.create("test plugin", "test key", null);
    rule.createParameter("test param key");

    ActiveRule activeRule = profile1.activateRule(rule, RulePriority.MAJOR);
    activeRule.setParameter("test param key", "test value");

    profiles.get(0).getAlerts().add(new Alert(null, new Metric("test key"), Alert.OPERATOR_GREATER, "testError", "testWarn"));

    return profiles;
  }

  private List<Rule> getUserRules() {
    List<Rule> rules = Lists.newArrayList();
    Rule parentRule = Rule.create("test plugin", "test key", null);
    Rule rule = Rule.create("test plugin", "test key2", "test name")
        .setDescription("test description")
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
      assertTrue(diff.toString(), diff.similar());
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

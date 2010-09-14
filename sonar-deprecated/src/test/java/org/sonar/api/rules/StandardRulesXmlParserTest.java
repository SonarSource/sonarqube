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
package org.sonar.api.rules;

import org.apache.commons.io.IOUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StandardRulesXmlParserTest {
  @Test
  public void checkAllFields() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1'><name>my name</name><configKey>my_config_key</configKey><description>my description</description><param key='param1'><type>s</type><description>param description</description></param><param key='param2'><type>integer</type><description>param description 2</description></param></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertEquals(1, rules.size());

    Rule rule = rules.get(0);
    Assert.assertEquals("key1", rule.getKey());
    Assert.assertEquals("my name", rule.getName());
    Assert.assertEquals("my_config_key", rule.getConfigKey());
    Assert.assertEquals("my description", rule.getDescription());
    Assert.assertEquals(2, rule.getParams().size());
    Assert.assertEquals("param1", rule.getParams().get(0).getKey());
    Assert.assertEquals("s", rule.getParams().get(0).getType());
    Assert.assertEquals("param description", rule.getParams().get(0).getDescription());
  }

  @Test
  public void ruleShouldHaveACategory() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule><category name='cat1' /></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertNotNull(rules.get(0).getRulesCategory());
    Assert.assertEquals("cat1", rules.get(0).getRulesCategory().getName());
    assertNull(rules.get(0).getRulesCategory().getId());
    assertNull(rules.get(0).getRulesCategory().getDescription());
  }

  @Test
  public void ruleCanHaveALevel() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='1' priority='CRITICAL'><category name='cat1'/></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertNotNull(rules.get(0).getRulesCategory());
    Assert.assertEquals(RulePriority.CRITICAL, rules.get(0).getPriority());
    assertNull(rules.get(0).getRulesCategory().getId());
    assertNull(rules.get(0).getRulesCategory().getDescription());
  }

  @Test
  public void ruleShouldHaveADefaultLevel() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='1'><category name='cat1'/></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    Assert.assertEquals(RulePriority.MAJOR, rules.get(0).getPriority());
  }

  @Test
  public void shouldDefineManyRules() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1' /><rule key='key2' category='cat1' /></rules>";
    List<Rule> rules = parser.parse(xml);
    assertEquals(2, rules.size());
    Assert.assertEquals("key1", rules.get(0).getKey());
    Assert.assertEquals("key2", rules.get(1).getKey());
  }

  @Test
  public void someFielsShouldBeNull() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1' /></rules>";
    List<Rule> rules = parser.parse(xml);
    assertNull(rules.get(0).getDescription());
    assertNull(rules.get(0).getName());
    assertNull(rules.get(0).getConfigKey());
  }

  @Test
  public void shouldContainCDataDescription() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1'><description>   <![CDATA[<xml> </nodes> and accents Žˆ˜  ]]>  </description></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertEquals(1, rules.size());
    Assert.assertEquals("<xml> </nodes> and accents Žˆ˜", rules.get(0).getDescription());
  }

  @Test
  public void shouldBeBackwardCompatibleWithDefaultVersionProperty() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1'><name>my name</name><configKey>my_config_key</configKey><param key='param1'><type>s</type><description>param description</description><defaultValue>xxx</defaultValue></param></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertEquals(1, rules.size());

    Rule rule = rules.get(0);
    Assert.assertEquals("key1", rule.getKey());
    Assert.assertEquals(1, rule.getParams().size());
    Assert.assertEquals("param1", rule.getParams().get(0).getKey());
  }

  @Test
  public void shouldParseStringInUt8() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1' ><description>\\u00E9</description></rule></rules>";
    List<Rule> rules = parser.parse(xml);
    assertThat(rules.get(0).getDescription(), is("\\u00E9"));
  }

  @Test
  public void shouldParseInputStreamInUt8() {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    String xml = "<rules><rule key='key1' category='cat1' ><description>\\u00E9</description></rule></rules>";
    List<Rule> rules = parser.parse(IOUtils.toInputStream(xml));
    assertThat(rules.get(0).getDescription(), is("\\u00E9"));
  }
}

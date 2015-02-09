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
package org.sonar.batch.protocol.input;

import org.fest.assertions.MapAssert;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectReferentialsTest {

  @Test
  public void testToJson() throws Exception {
    ProjectReferentials ref = new ProjectReferentials();
    assertThat(ref.settings("foo")).isEmpty();

    ref.addQProfile(new QProfile("squid-java", "Java", "java", new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1984")));
    HashMap<String, String> settings = new HashMap<String, String>();
    settings.put("prop1", "value1");
    ref.addSettings("foo", settings);
    settings = new HashMap<String, String>();
    settings.put("prop2", "value2");
    ref.addSettings("foo", settings);
    ref.settings("foo").put("prop", "value");
    ActiveRule activeRule = new ActiveRule("repo", "rule", "templateRule", "Rule", "MAJOR", "rule", "java");
    activeRule.addParam("param1", "value1");
    ref.addActiveRule(activeRule);
    ref.setTimestamp(10);

    System.out.println(ref.toJson());
    JSONAssert
      .assertEquals(
        "{timestamp:10,"
          + "qprofilesByLanguage:{java:{key:\"squid-java\",name:Java,language:java,rulesUpdatedAt:\"Mar 14, 1984 12:00:00 AM\"}},"
          + "activeRules:[{repositoryKey:repo,ruleKey:rule,templateRuleKey:templateRule,name:Rule,severity:MAJOR,internalKey:rule,language:java,params:{param1:value1}}],"
          + "settingsByModule:{foo:{prop1:value1,prop2:value2,prop:value}}}",
        ref.toJson(), true);
  }

  @Test
  public void testFromJson() throws JSONException, ParseException {
    ProjectReferentials ref = ProjectReferentials.fromJson("{timestamp:1,"
      + "qprofilesByLanguage:{java:{key:\"squid-java\",name:Java,language:java,rulesUpdatedAt:\"Mar 14, 1984 12:00:00 AM\"}},"
      + "activeRules:[{repositoryKey:repo,ruleKey:rule,templateRuleKey:templateRule,name:Rule,severity:MAJOR,internalKey:rule1,language:java,params:{param1:value1}}],"
      + "settingsByModule:{foo:{prop:value}}}");

    assertThat(ref.timestamp()).isEqualTo(1);

    ActiveRule activeRule = ref.activeRules().iterator().next();
    assertThat(activeRule.ruleKey()).isEqualTo("rule");
    assertThat(activeRule.repositoryKey()).isEqualTo("repo");
    assertThat(activeRule.templateRuleKey()).isEqualTo("templateRule");
    assertThat(activeRule.name()).isEqualTo("Rule");
    assertThat(activeRule.severity()).isEqualTo("MAJOR");
    assertThat(activeRule.internalKey()).isEqualTo("rule1");
    assertThat(activeRule.language()).isEqualTo("java");
    assertThat(activeRule.params()).includes(MapAssert.entry("param1", "value1"));
    assertThat(activeRule.param("param1")).isEqualTo("value1");
    QProfile qProfile = ref.qProfiles().iterator().next();
    assertThat(qProfile.key()).isEqualTo("squid-java");
    assertThat(qProfile.name()).isEqualTo("Java");
    assertThat(qProfile.rulesUpdatedAt()).isEqualTo(new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1984"));
    assertThat(ref.settings("foo")).includes(MapAssert.entry("prop", "value"));
  }
}

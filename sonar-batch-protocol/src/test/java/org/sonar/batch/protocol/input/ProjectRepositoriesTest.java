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

import org.junit.Test;
import org.sonar.test.JsonAssert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectRepositoriesTest {

  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  @Test
  public void testToJson() throws Exception {
    ProjectRepositories ref = new ProjectRepositories();
    assertThat(ref.settings("foo")).isEmpty();

    ref.addQProfile(new QProfile("squid-java", "Java", "java", DATE_FORMAT.parse("2013-01-01T12:00:00+0100")));
    HashMap<String, String> settings = new HashMap<>();
    settings.put("prop1", "value1");
    ref.addSettings("foo", settings);
    settings = new HashMap<>();
    settings.put("prop2", "value2");
    ref.addSettings("foo", settings);
    ref.settings("foo").put("prop", "value");
    ActiveRule activeRule = new ActiveRule("repo", "rule", "templateRule", "Rule", "MAJOR", "rule", "java");
    activeRule.addParam("param1", "value1");
    ref.addActiveRule(activeRule);
    ref.setLastAnalysisDate(DATE_FORMAT.parse("2014-05-18T15:50:45+0100"));
    ref.setTimestamp(10);
    ref.addFileData("foo", "src/main/java/Foo.java", new FileData("xyz", true));
    ref.addFileData("foo", "src/main/java/Foo2.java", new FileData("xyz", false));

    JsonAssert.assertJson(ref.toJson())
      .isSimilarTo(getClass().getResource("ProjectRepositoriesTest/testToJson.json"));
  }

  @Test
  public void testFromJson() throws ParseException {
    ProjectRepositories ref = ProjectRepositories
      .fromJson("{timestamp:1,"
        + "qprofilesByLanguage:{java:{key:\"squid-java\",name:Java,language:java,rulesUpdatedAt:\"2013-01-01T12:00:00+0100\"}},"
        + "activeRules:[{repositoryKey:repo,ruleKey:rule,templateRuleKey:templateRule,name:Rule,severity:MAJOR,internalKey:rule1,language:java,params:{param1:value1}}],"
        + "settingsByModule:{foo:{prop:value}},"
        + "fileDataByModuleAndPath:{foo:{\"src/main/java/Foo.java\":{hash:xyz,needBlame:true,scmLastCommitDatetimesByLine:\"1\u003d12345,2\u003d3456\",scmRevisionsByLine:\"1\u003d345,2\u003d345\",scmAuthorsByLine:\"1\u003dhenryju,2\u003dgaudin\"}}},"
        + "lastAnalysisDate:\"2014-10-31T00:00:00+0100\"}");

    assertThat(ref.timestamp()).isEqualTo(1);

    ActiveRule activeRule = ref.activeRules().iterator().next();
    assertThat(activeRule.ruleKey()).isEqualTo("rule");
    assertThat(activeRule.repositoryKey()).isEqualTo("repo");
    assertThat(activeRule.templateRuleKey()).isEqualTo("templateRule");
    assertThat(activeRule.name()).isEqualTo("Rule");
    assertThat(activeRule.severity()).isEqualTo("MAJOR");
    assertThat(activeRule.internalKey()).isEqualTo("rule1");
    assertThat(activeRule.language()).isEqualTo("java");
    assertThat(activeRule.params()).containsEntry("param1", "value1");
    assertThat(activeRule.param("param1")).isEqualTo("value1");
    QProfile qProfile = ref.qProfiles().iterator().next();
    assertThat(qProfile.key()).isEqualTo("squid-java");
    assertThat(qProfile.name()).isEqualTo("Java");
    assertThat(qProfile.rulesUpdatedAt().getTime()).isEqualTo(DATE_FORMAT.parse("2013-01-01T12:00:00+0100").getTime());
    assertThat(ref.settings("foo")).containsEntry("prop", "value");

    assertThat(ref.fileData("foo2", "src/main/java/Foo3.java")).isNull();

    assertThat(ref.fileData("foo", "src/main/java/Foo.java").hash()).isEqualTo("xyz");
    assertThat(ref.fileData("foo", "src/main/java/Foo.java").needBlame()).isTrue();

    assertThat(ref.lastAnalysisDate().getTime()).isEqualTo(DATE_FORMAT.parse("2014-10-31T00:00:00+0100").getTime());
  }
}

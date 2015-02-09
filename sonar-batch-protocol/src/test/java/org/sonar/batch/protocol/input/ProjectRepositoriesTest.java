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

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectRepositoriesTest {

  @Test
  public void testToJson() throws Exception {
    ProjectRepositories ref = new ProjectRepositories();
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
    ref.setLastAnalysisDate(new SimpleDateFormat("dd/MM/yyyy").parse("31/10/2014"));
    ref.setTimestamp(10);
    ref.addFileData("foo", "src/main/java/Foo.java", new FileData("xyz", true, "1=12345,2=3456", "1=345,2=345", "1=henryju,2=gaudin"));
    ref.addFileData("foo", "src/main/java/Foo2.java", new FileData("xyz", false, "1=12345,2=3456", "1=345,2=345", "1=henryju,2=gaudin"));

    JSONAssert
      .assertEquals(
        "{timestamp:10,"
          + "qprofilesByLanguage:{java:{key:\"squid-java\",name:Java,language:java,rulesUpdatedAt:\"1984-03-14T00:00:00+0100\"}},"
          + "activeRules:[{repositoryKey:repo,ruleKey:rule,templateRuleKey:templateRule,name:Rule,severity:MAJOR,internalKey:rule,language:java,params:{param1:value1}}],"
          + "settingsByModule:{foo:{prop1:value1,prop2:value2,prop:value}},"
          + "fileDataByModuleAndPath:{foo:{\"src/main/java/Foo.java\":{hash:xyz,needBlame:true,scmLastCommitDatetimesByLine:\"1\u003d12345,2\u003d3456\",scmRevisionsByLine:\"1\u003d345,2\u003d345\",scmAuthorsByLine:\"1\u003dhenryju,2\u003dgaudin\"},"
          + "\"src/main/java/Foo2.java\":{hash:xyz,needBlame:false,scmLastCommitDatetimesByLine:\"1\u003d12345,2\u003d3456\",scmRevisionsByLine:\"1\u003d345,2\u003d345\",scmAuthorsByLine:\"1\u003dhenryju,2\u003dgaudin\"}}},"
          + "lastAnalysisDate:\"2014-10-31T00:00:00+0100\"}",
        ref.toJson(), true);
  }

  @Test
  public void testFromJson() throws JSONException, ParseException {
    ProjectRepositories ref = ProjectRepositories
      .fromJson("{timestamp:1,"
        + "qprofilesByLanguage:{java:{key:\"squid-java\",name:Java,language:java,rulesUpdatedAt:\"1984-03-14T00:00:00+0100\"}},"
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
    assertThat(qProfile.rulesUpdatedAt()).isEqualTo(new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1984"));
    assertThat(ref.settings("foo")).containsEntry("prop", "value");

    assertThat(ref.fileData("foo2", "src/main/java/Foo3.java")).isNull();

    assertThat(ref.fileData("foo", "src/main/java/Foo.java").hash()).isEqualTo("xyz");
    assertThat(ref.fileData("foo", "src/main/java/Foo.java").needBlame()).isTrue();
    assertThat(ref.fileData("foo", "src/main/java/Foo.java").scmAuthorsByLine()).isEqualTo("1=henryju,2=gaudin");
    assertThat(ref.fileData("foo", "src/main/java/Foo.java").scmLastCommitDatetimesByLine()).isEqualTo("1=12345,2=3456");
    assertThat(ref.fileData("foo", "src/main/java/Foo.java").scmRevisionsByLine()).isEqualTo("1=345,2=345");

    assertThat(ref.lastAnalysisDate()).isEqualTo(new SimpleDateFormat("dd/MM/yyyy").parse("31/10/2014"));
  }
}

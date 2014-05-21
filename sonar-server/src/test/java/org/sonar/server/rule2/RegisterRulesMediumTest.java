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

package org.sonar.server.rule2;

import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.index.RuleQuery;
import org.sonar.server.rule2.index.RuleResult;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RegisterRulesMediumTest {

  @org.junit.Rule
  public ServerTester tester = new ServerTester().addComponents(XooRulesDefinition.class);

  @Test
  public void register_rules_at_startup() throws Exception {
    verifyRulesInDb();

    RuleIndex index = tester.get(RuleIndex.class);


    RuleResult searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(2);
    assertThat(searchResult.getHits()).hasSize(2);
  }

  @Test
  public void index_even_if_no_changes() throws Exception {
    RuleIndex index = tester.get(RuleIndex.class);

    verifyRulesInDb();

    // clear ES but keep db
    tester.clearIndexes();
    verifyRulesInDb();
    RuleResult searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(0);
    assertThat(searchResult.getHits()).hasSize(0);

    // db is not updated (same rules) but es must be reindexed
    tester.get(Platform.class).restart();

    index = tester.get(RuleIndex.class);

    verifyRulesInDb();
    searchResult = index.search(new RuleQuery().setKey("xoo:x1"), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(1);
    assertThat(searchResult.getHits()).hasSize(1);
    assertThat(searchResult.getHits().get(0).params()).hasSize(1);
  }

  private void verifyRulesInDb() {
    RuleDao dao = tester.get(RuleDao.class);
    DbClient dbClient = tester.get(DbClient.class);
    DbSession session = dbClient.openSession(false);
    List<RuleDto> rules = dao.findAll(session);
    assertThat(rules).hasSize(2);
    List<RuleParamDto> ruleParams = dao.findAllRuleParams(session);
    assertThat(ruleParams).hasSize(1);
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR)
        .createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");

      repository.createRule("x2")
        .setName("x2 name")
        .setHtmlDescription("x2 desc")
        .setSeverity(Severity.MAJOR);
      repository.done();
    }
  }
}

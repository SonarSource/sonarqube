/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.rule.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
public class SearchActionTest {

    private static final String JAVA = "java";

    @org.junit.Rule
    public UserSessionRule userSession = UserSessionRule.standalone();

    private final System2 system2 = new AlwaysIncreasingSystem2();
    @org.junit.Rule
    public DbTester db = DbTester.create(system2);
    @org.junit.Rule
    public EsTester es = EsTester.create();

    private final RuleIndex ruleIndex = new RuleIndex(es.client(), system2);
    private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
    private final Languages languages = LanguageTesting.newLanguages(JAVA, "js");
    private final ActiveRuleCompleter activeRuleCompleter = new ActiveRuleCompleter(db.getDbClient(), languages);
    private final RuleQueryFactory ruleQueryFactory = new RuleQueryFactory(db.getDbClient());
    private final MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
    private final RuleMapper ruleMapper = new RuleMapper(languages, macroInterpreter, new RuleDescriptionFormatter());
    private final SearchAction underTest = new SearchAction(ruleIndex, activeRuleCompleter, ruleQueryFactory, db.getDbClient(), ruleMapper,
            new RuleWsSupport(db.getDbClient(), userSession));
    private final TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
    private final WsActionTester ws = new WsActionTester(underTest);

    @Before
    public void before() {
        doReturn("interpreted").when(macroInterpreter).interpret(anyString());
    }

    @Test
    public void return_subset_contain_the_updated_at_field() {
        RuleDto rule = db.rules().insert(r -> r.setLanguage("java"));
        indexRules();

        Rules.SearchResponse response = ws.newRequest()
                .setParam(WebService.Param.FIELDS, "createdAt, updatedAt")
                .executeProtobuf(Rules.SearchResponse.class);
        Rules.Rule result = response.getRules(0);

        // mandatory fields
        assertThat(result.getKey()).isEqualTo(rule.getKey().toString());

        // selected fields
        assertThat(result.getCreatedAt()).isNotEmpty();
        assertThat(result.getUpdatedAt()).isNotEmpty();

        // not returned fields
        assertThat(result.getLangName()).isEmpty();
    }

    private void indexRules() {
        ruleIndexer.indexAll();
    }

}

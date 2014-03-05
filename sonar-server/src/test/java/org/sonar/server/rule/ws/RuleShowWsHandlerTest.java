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

package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ws.WsTester;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleNote;
import org.sonar.server.rule.Rules;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleShowWsHandlerTest {

  @Mock
  Rules rules;

  @Mock
  Languages languages;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  I18n i18n;

  Rule.Builder ruleBuilder = new Rule.Builder()
    .setKey("AvoidCycle")
    .setRepositoryKey("squid")
    .setName("Avoid cycle")
    .setLanguage("xoo")
    .setDescription("Avoid cycle between packages");

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new RulesWs(mock(RuleSearchWsHandler.class), new RuleShowWsHandler(rules, ruleFinder, i18n, languages), mock(AddTagsWsHandler.class), mock(RemoveTagsWsHandler.class)));
  }

  @Test
  public void show_rule() throws Exception {
    String ruleKey = "squid:AvoidCycle";
    Rule rule = ruleBuilder.build();

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule);
    Language language = mock(Language.class);
    when(language.getName()).thenReturn("Xoo");
    when(languages.get("xoo")).thenReturn(language);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", ruleKey);
    request.execute().assertJson(getClass(), "show_rule.json");
  }

  @Test
  public void return_not_found_on_unknown_rule() throws Exception {
    String ruleKey = "squid:AvoidCycle";

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(null);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", ruleKey);

    try {
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void show_rule_with_dates() throws Exception {
    Date date1 = DateUtils.parseDateTime("2014-01-22T19:10:03+0100");
    Date date2 = DateUtils.parseDateTime("2014-01-23T19:10:03+0100");
    Rule rule = ruleBuilder
      .setCreatedAt(date1)
      .setUpdatedAt(date2)
      .build();

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule);
    when(i18n.formatDateTime(any(Locale.class), eq(date1))).thenReturn("Jan 22, 2014 10:03 AM");
    when(i18n.formatDateTime(any(Locale.class), eq(date2))).thenReturn("Jan 23, 2014 10:03 AM");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", rule.ruleKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_dates.json");
  }

  @Test
  public void show_rule_with_note() throws Exception {
    RuleNote note = mock(RuleNote.class);
    when(note.data()).thenReturn("*Extended rule description*");
    Rule rule = ruleBuilder
      .setRuleNote(note)
      .build();

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", rule.ruleKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_note.json");
  }

  @Test
  public void show_rule_with_tags() throws Exception {
    Rule rule = ruleBuilder
      .setAdminTags(ImmutableList.of("complexity"))
      .setSystemTags(ImmutableList.of("security"))
      .build();

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(rule);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", rule.ruleKey().toString());
    request.execute().assertJson(getClass(), "show_rule_with_tags.json");
  }

  @Test
  public void show_manual_rule() throws Exception {
    String ruleKey = "manual:api";
    when(ruleFinder.findByKey(RuleKey.of("manual", "api"))).thenReturn(
      org.sonar.api.rules.Rule.create("manual", "api", "API").setDescription("API rule description"));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", ruleKey);
    request.execute();
    request.execute().assertJson(getClass(), "show_manuel_rule.json");
  }

  @Test
  public void show_manual_rule_without_severity() throws Exception {
    String ruleKey = "manual:api";
    org.sonar.api.rules.Rule rule = mock(org.sonar.api.rules.Rule.class);
    when(rule.getKey()).thenReturn("api");
    when(rule.getRepositoryKey()).thenReturn("manual");
    when(rule.getName()).thenReturn("API");
    when(rule.getDescription()).thenReturn("API rule description");
    when(rule.getSeverity()).thenReturn(null);
    when(ruleFinder.findByKey(RuleKey.of("manual", "api"))).thenReturn(rule);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", ruleKey);
    request.execute();
    request.execute().assertJson(getClass(), "show_manuel_rule.json");
  }

  @Test
  public void return_not_found_on_unknown_manual_rule() throws Exception {
    String ruleKey = "manual:api";

    when(rules.findByKey(RuleKey.of("squid", "AvoidCycle"))).thenReturn(null);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", ruleKey);

    try {
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

}

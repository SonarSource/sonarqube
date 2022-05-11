/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.qualityprofile;

import java.util.Set;
import java.util.function.Predicate;
import javax.servlet.AsyncContext;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.rule.RuleChange;
import org.sonar.core.util.rule.RuleSetChangedEvent;
import org.sonar.server.pushapi.sonarlint.SonarLintClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleSetChangeBroadcastUtilsTest {

  private final static String JAVA_KEY = "java";
  private final static String PROJECT_KEY_1 = "projectKey1";
  private final static String PROJECT_KEY_2 = "projectKey2";
  private final static String USER_UUID = "userUUID";
  private final static String[] DEACTIVATED_RULES = {"repo2:rule-key2"};

  private final static Set<String> EXAMPLE_KEYS = Set.of(PROJECT_KEY_1, PROJECT_KEY_2);

  private final AsyncContext asyncContext = mock(AsyncContext.class);

  @Test
  public void getsFilterForEvent() {
    RuleChange javaRule = new RuleChange();
    javaRule.setLanguage(JAVA_KEY);
    javaRule.setParams(new ParamChange[]{new ParamChange("param-key", "param-value")});
    javaRule.setTemplateKey("repo:template-key");
    javaRule.setSeverity(Severity.CRITICAL);
    javaRule.setKey("repo:rule-key");

    RuleChange[] activatedRules = {javaRule};
    RuleSetChangedEvent ruleSetChangedEvent = new RuleSetChangedEvent(EXAMPLE_KEYS.toArray(String[]::new), activatedRules,
      DEACTIVATED_RULES, JAVA_KEY);
    Predicate<SonarLintClient> predicate = RuleSetChangeBroadcastUtils.getFilterForEvent(ruleSetChangedEvent);
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of(PROJECT_KEY_1), Set.of(JAVA_KEY), USER_UUID))).isTrue();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of(PROJECT_KEY_2), Set.of(JAVA_KEY), USER_UUID))).isTrue();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of(PROJECT_KEY_1), Set.of(), USER_UUID))).isFalse();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of(), Set.of(JAVA_KEY), USER_UUID))).isFalse();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of("another-project"), Set.of(), USER_UUID))).isFalse();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of(""), Set.of("another-language"), USER_UUID))).isFalse();
    assertThat(predicate.test(new SonarLintClient(asyncContext, Set.of("another-project"), Set.of("another-language"), USER_UUID))).isFalse();
  }

  @Test
  public void getsMessageForEvent() {
    RuleSetChangedEvent ruleSetChangedEvent = new RuleSetChangedEvent(new String[]{PROJECT_KEY_1}, new RuleChange[0],
      DEACTIVATED_RULES, JAVA_KEY);

    String message = RuleSetChangeBroadcastUtils.getMessage(ruleSetChangedEvent);

    assertThat(message).isEqualTo("event: RuleSetChanged\n" +
      "data: {\"activatedRules\":[]," +
      "\"projects\":[\"" + PROJECT_KEY_1 + "\"]," +
      "\"deactivatedRules\":[\"repo2:rule-key2\"]}");
  }
}

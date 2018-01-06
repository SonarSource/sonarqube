/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.rule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleFinderCompatibilityTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Rules rules;
  private RuleFinderCompatibility ruleFinder;

  @Before
  public void prepare() {
    RulesBuilder builder = new RulesBuilder();
    builder.add(RuleKey.of("repo1", "rule1"));
    builder.add(RuleKey.of("repo1", "rule2")).setInternalKey("rule2_internal");
    builder.add(RuleKey.of("repo2", "rule1"));
    rules = builder.build();
    
    ruleFinder = new RuleFinderCompatibility(rules);
  }

  @Test
  public void testByInternalKey() {
    assertThat(ruleFinder.find(RuleQuery.create().withRepositoryKey("repo1").withConfigKey("rule2_internal")).getKey()).isEqualTo("rule2");
    assertThat(ruleFinder.find(RuleQuery.create().withRepositoryKey("repo1").withConfigKey("rule2_internal2"))).isNull();
  }

  @Test
  public void testByKey() {
    assertThat(ruleFinder.find(RuleQuery.create().withRepositoryKey("repo1").withKey("rule2")).getKey()).isEqualTo("rule2");
    assertThat(ruleFinder.find(RuleQuery.create().withRepositoryKey("repo1").withKey("rule3"))).isNull();
    assertThat(ruleFinder.findByKey("repo1", "rule2").getKey()).isEqualTo("rule2");
  }

  @Test
  public void duplicateResult() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Non unique result for rule query: RuleQuery[repositoryKey=repo1,key=<null>,configKey=<null>]");
    ruleFinder.find(RuleQuery.create().withRepositoryKey("repo1"));
  }

  @Test
  public void unsupportedById() {
    thrown.expect(UnsupportedOperationException.class);
    ruleFinder.findById(1);
  }

  @Test
  public void unsupportedByInternalKeyWithoutRepo() {
    thrown.expect(UnsupportedOperationException.class);
    ruleFinder.find(RuleQuery.create().withConfigKey("config"));
  }

  @Test
  public void unsupportedByKeyWithoutRepo() {
    thrown.expect(UnsupportedOperationException.class);
    ruleFinder.find(RuleQuery.create().withKey("key"));
  }

  @Test
  public void unsupportedByKeyAndInternalKey() {
    thrown.expect(UnsupportedOperationException.class);
    ruleFinder.find(RuleQuery.create().withRepositoryKey("repo").withKey("key").withConfigKey("config"));
  }

  @Test
  public void unsupportedByKeyAndInternalKeyWithoutRepo() {
    thrown.expect(UnsupportedOperationException.class);
    ruleFinder.find(RuleQuery.create().withKey("key").withConfigKey("config"));
  }
}

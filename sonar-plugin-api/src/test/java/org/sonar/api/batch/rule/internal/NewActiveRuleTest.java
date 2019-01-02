/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;

public class NewActiveRuleTest {

  private NewActiveRule.Builder builder;

  @Before
  public void setBuilder() {
    builder = new NewActiveRule.Builder();
  }

  @Test
  public void builder_should_set_every_param() {
    NewActiveRule rule = builder
      .setRuleKey(RuleKey.of("foo", "bar"))
      .setName("name")
      .setSeverity(Severity.CRITICAL)
      .setParam("key", "value")
      .setCreatedAt(1_000L)
      .setUpdatedAt(1_000L)
      .setInternalKey("internal_key")
      .setLanguage("language")
      .setTemplateRuleKey("templateRuleKey")
      .setQProfileKey("qProfileKey")
      .build();

    assertThat(rule.ruleKey).isEqualTo(RuleKey.of("foo", "bar"));
    assertThat(rule.name).isEqualTo("name");
    assertThat(rule.severity).isEqualTo(Severity.CRITICAL);
    assertThat(rule.params).isEqualTo(ImmutableMap.of("key", "value"));
    assertThat(rule.createdAt).isEqualTo(1_000L);
    assertThat(rule.updatedAt).isEqualTo(1_000L);
    assertThat(rule.internalKey).isEqualTo("internal_key");
    assertThat(rule.language).isEqualTo("language");
    assertThat(rule.templateRuleKey).isEqualTo("templateRuleKey");
    assertThat(rule.qProfileKey).isEqualTo("qProfileKey");
  }

  @Test
  public void severity_should_have_default_value() {
    NewActiveRule rule = builder.build();
    assertThat(rule.severity).isEqualTo(Severity.defaultSeverity());
  }

  @Test
  public void params_should_be_empty_map_if_no_params() {
    NewActiveRule rule = builder.build();
    assertThat(rule.params).isEqualTo(ImmutableMap.of());
  }

  @Test
  public void set_param_remove_param_if_value_is_null() {
    NewActiveRule rule = builder
      .setParam("foo", "bar")
      .setParam("removed", "value")
      .setParam("removed", null)
      .build();
    assertThat(rule.params).isEqualTo(ImmutableMap.of("foo", "bar"));
  }
}

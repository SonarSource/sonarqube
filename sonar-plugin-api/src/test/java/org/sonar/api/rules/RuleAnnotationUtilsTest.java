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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RuleAnnotationUtilsTest {

  @Test
  public void readAnnotatedClassWithoutParameters() {
    Rule rule = RuleAnnotationUtils.readAnnotatedClass(AnnotatedCheck.class);
    assertNotNull(rule);
    assertThat(rule.getKey(), is(AnnotatedCheck.class.getName()));
    assertThat(rule.getName(), is("Annotated Check"));
    assertThat(rule.getConfigKey(), nullValue());
    assertThat(rule.getParams().size(), is(0));
    assertThat(rule.getDescription(), is("Description"));
    assertThat(rule.getCardinality(), is(Rule.Cardinality.SINGLE));
    assertThat(rule.getRulesCategory().getName(), is(Iso9126RulesCategories.RELIABILITY.getName()));
  }

  @Test
  public void ruleKeyCanBeOverridden() {
    Rule rule = RuleAnnotationUtils.readAnnotatedClass(AnnotatedCheckWithParameters.class);
    assertNotNull(rule);
    assertThat(rule.getKey(), is("overriden_key"));
  }
  @Test
  public void readAnnotatedClassWithParameters() {
    Rule rule = RuleAnnotationUtils.readAnnotatedClass(AnnotatedCheckWithParameters.class);
    assertNotNull(rule);
    assertThat(rule.getParams().size(), is(2));
    assertThat(rule.getParam("max"), not(nullValue()));
    assertThat(rule.getParam("max").getDescription(), is("Maximum value"));

    assertThat(rule.getParam("min"), nullValue());
    assertThat(rule.getParam("overidden_min"), not(nullValue()));
    assertThat(rule.getParam("overidden_min").getDescription(), is("Minimum value"));
  }
}

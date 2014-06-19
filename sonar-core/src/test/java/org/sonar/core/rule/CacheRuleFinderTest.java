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
package org.sonar.core.rule;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class CacheRuleFinderTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldCacheFindById() {
    setupData("shared");
    RuleFinder finder = new CacheRuleFinder(getSessionFactory());
    assertThat(finder.findById(3).getConfigKey(), is("Checker/Treewalker/AnnotationUseStyleCheck"));

    deleteRules();

    assertThat(finder.findById(3), notNullValue());
  }

  @Test
  public void shouldNotFailIfUnknownRuleId() {
    setupData("shared");
    RuleFinder finder = new CacheRuleFinder(getSessionFactory());
    assertThat(finder.findById(33456816), nullValue());
  }

  @Test
  public void shouldCacheFindByKey() {
    setupData("shared");
    RuleFinder finder = new CacheRuleFinder(getSessionFactory());
    Rule rule = finder.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck");
    assertThat(rule.getConfigKey(), is("Checker/Treewalker/AnnotationUseStyleCheck"));

    deleteRules();

    rule = finder.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.annotation.AnnotationUseStyleCheck");
    assertThat(rule, notNullValue());
  }

  private void deleteRules() {
    getSession().createQuery("delete " + Rule.class.getSimpleName()).executeUpdate();
    getSession().commit();
  }
}

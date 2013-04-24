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
package org.sonar.api.workflow.condition;

import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefs;
import org.sonar.api.config.Settings;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;

import static org.fest.assertions.Assertions.assertThat;

public class HasProjectPropertyConditionTest {
  @Test
  public void doVerify() {
    HasProjectPropertyCondition condition = new HasProjectPropertyCondition("jira.url");
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setSettings(new Settings().setProperty("jira.url", "http://jira"));
    assertThat(condition.doVerify(new DefaultReview(), context)).isTrue();
  }

  @Test
  public void missingProperty() {
    HasProjectPropertyCondition condition = new HasProjectPropertyCondition("jira.url");
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setSettings(new Settings());
    assertThat(condition.doVerify(new DefaultReview(), context)).isFalse();
  }

  @Test
  public void returnTrueIfDefaultValue() {
    HasProjectPropertyCondition condition = new HasProjectPropertyCondition("jira.url");
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setSettings(new Settings(new PropertyDefs().addComponent(WithDefaultValue.class)));
    assertThat(condition.doVerify(new DefaultReview(), context)).isTrue();
  }

  @Properties({
      @Property(key = "jira.url", name = "JIRA URL", defaultValue = "http://jira.com")
  })
  private static class WithDefaultValue {

  }
}

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
package org.sonar.plugins.core.issue;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.api.JavaClass;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CountFalsePositivesDecoratorTest {

  ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
  CountFalsePositivesDecorator decorator = new CountFalsePositivesDecorator(perspectives);

  @Test
  public void should_count_false_positives() {
    DefaultIssue falsePositive = new DefaultIssue().setRuleKey(RuleKey.parse("squid:AvoidCycles"))
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE).setStatus(Issue.STATUS_RESOLVED);
    DefaultIssue fixed = new DefaultIssue().setRuleKey(RuleKey.parse("squid:AvoidCycles"))
      .setResolution(Issue.RESOLUTION_FIXED).setStatus(Issue.STATUS_RESOLVED);

    File file = File.create("foo.c");
    Issuable issuable = mock(Issuable.class);
    when(perspectives.as(Issuable.class, file)).thenReturn(issuable);
    when(issuable.resolvedIssues()).thenReturn(Arrays.<Issue>asList(falsePositive, fixed));

    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(file, context);

    verify(context).saveMeasure(CoreMetrics.FALSE_POSITIVE_ISSUES, 1.0);
  }

  @Test
  public void should_declare_metadata() {
    assertThat(decorator.shouldExecuteOnProject(new Project("foo"))).isTrue();
    assertThat(decorator.generatesFalsePositiveMeasure()).isEqualTo(CoreMetrics.FALSE_POSITIVE_ISSUES);
    assertThat(decorator.toString()).isEqualTo("CountFalsePositivesDecorator");
  }

  @Test
  public void should_ignore_classes_and_methods() {
    JavaClass javaClass = JavaClass.create("Foo.java");
    when(perspectives.as(Issuable.class, javaClass)).thenReturn(null);

    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(javaClass, context);

    verifyZeroInteractions(context);
  }
}

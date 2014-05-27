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
package org.sonar.batch.rule;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QProfileEventsDecoratorTest {

  Project project = new Project("myProject");
  DecoratorContext decoratorContext = mock(DecoratorContext.class);
  TimeMachine timeMachine = mock(TimeMachine.class);
  private Languages languages = mock(Languages.class);
  private QualityProfileDao qualityProfileDao = mock(QualityProfileDao.class);
  QProfileEventsDecorator decorator = new QProfileEventsDecorator(timeMachine, qualityProfileDao, languages);

  @Test
  public void shouldExecuteOnProjects() {
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldDoNothingIfNoProfileChange() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]");
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext, never()).createEvent(anyString(), anyString(), anyString(), any(Date.class));
  }

  @Test
  public void shouldDoNothingIfNoProfileChange_fallbackOldProfileMeasure() {
    mockTMWithDeprecatedProfileMeasures(2, "Java Two", 20);
    when(qualityProfileDao.selectById(20)).thenReturn(new QualityProfileDto().setLanguage("java"));
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext, never()).createEvent(anyString(), anyString(), anyString(), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileChange() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]");
    // Different profile
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":3,\"name\":\"Java Other\",\"version\":1,\"language\":\"java\"}]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).createEvent(
      eq("Use Java Other version 1 (Java)"),
      eq("Java Other version 1 used for Java"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileChange_fallbackOldProfileMeasure() {
    mockTMWithDeprecatedProfileMeasures(2, "Java Two", 20);
    when(qualityProfileDao.selectById(20)).thenReturn(new QualityProfileDto().setLanguage("java"));
    // Different profile
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":3,\"name\":\"Java Other\",\"version\":1,\"language\":\"java\"}]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).createEvent(
      eq("Use Java Other version 1 (Java)"),
      eq("Java Other version 1 used for Java"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileVersionChange() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]");
    // Same profile, different version
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":21,\"language\":\"java\"}]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).createEvent(
      eq("Use Java Two version 21 (Java)"),
      eq("Java Two version 21 used for Java"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileVersionChange_fallbackOldProfileMeasure() {
    mockTMWithDeprecatedProfileMeasures(2, "Java Two", 20);
    when(qualityProfileDao.selectById(20)).thenReturn(new QualityProfileDto().setLanguage("java"));
    // Same profile, different version
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":21,\"language\":\"java\"}]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).createEvent(
      eq("Use Java Two version 21 (Java)"),
      eq("Java Two version 21 used for Java"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldCreateEventIfProfileVersionChange_fallbackOldProfileMeasure_noVersion() {
    mockTMWithDeprecatedProfileMeasures(2, "Java Two", null);
    when(qualityProfileDao.selectById(20)).thenReturn(new QualityProfileDto().setLanguage("java"));
    // Same profile, different version
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":21,\"language\":\"java\"}]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext).createEvent(
      eq("Use Java Two version 21 (Java)"),
      eq("Java Two version 21 used for Java"),
      same(Event.CATEGORY_PROFILE), any(Date.class));
  }

  @Test
  public void shouldNotCreateEventIfFirstAnalysis() {
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":21,\"language\":\"java\"}]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(decoratorContext, never()).createEvent(anyString(), anyString(), anyString(), any(Date.class));
  }

  private void mockTMWithDeprecatedProfileMeasures(double profileId, String profileName, Integer versionValue) {
    mockTM(new Measure(CoreMetrics.PROFILE, profileId, profileName), versionValue == null ? null : new Measure(CoreMetrics.PROFILE_VERSION, Double.valueOf(versionValue)));
  }

  private void mockTM(Measure result1, Measure result2) {
    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Collections.<Measure>emptyList())
      .thenReturn(result1 == null ? Collections.<Measure>emptyList() : Arrays.asList(result1))
      .thenReturn(result2 == null ? Collections.<Measure>emptyList() : Arrays.asList(result2));

  }
}

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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.protocol.Constants.EventCategory;
import org.sonar.batch.report.EventCache;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QProfileEventsDecoratorTest {

  static final String JAVA_V1_JSON = "{\"key\":\"J1\",\"language\":\"java\",\"name\":\"Java One\",\"rulesUpdatedAt\":\"2014-01-15T12:00:00+0000\"}";
  static final String JAVA_V2_JSON = "{\"key\":\"J1\",\"language\":\"java\",\"name\":\"Java One\",\"rulesUpdatedAt\":\"2014-02-20T12:00:00+0000\"}";
  static final String JAVA_OTHER_JSON = "{\"key\":\"J2\",\"language\":\"java\",\"name\":\"Java Two\",\"rulesUpdatedAt\":\"2014-02-20T12:00:00+0000\"}";

  Project project = new Project("myProject");
  DecoratorContext decoratorContext = mock(DecoratorContext.class);
  TimeMachine timeMachine = mock(TimeMachine.class);
  Languages languages = mock(Languages.class);
  EventCache eventCache = mock(EventCache.class);
  QProfileEventsDecorator decorator = new QProfileEventsDecorator(timeMachine, languages, eventCache);

  @Before
  public void prepare() {
    when(decoratorContext.getResource()).thenReturn(project);
  }

  @Test
  public void basic_tests() {
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
    assertThat(decorator.toString()).isEqualTo("QProfileEventsDecorator");
    assertThat(decorator.dependsUpon()).isNotNull();
  }

  @Test
  public void do_not_generate_event_if_no_changes() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V1_JSON + "]");
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V1_JSON + "]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    decorator.decorate(project, decoratorContext);

    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
  }

  @Test
  public void generate_event_if_profile_change() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V1_JSON + "]");
    // Different profile
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V2_JSON + "]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    // "from" and "to" must have one second more because of lack of ms precision
    verify(eventCache).createEvent(project, "Changes in 'Java One' (Java)", null, EventCategory.PROFILE, "from=2014-01-15T12:00:01+0000;key=J1;to=2014-02-20T12:00:01+0000");
  }

  @Test
  public void generate_event_if_profile_not_used_anymore() {
    Measure previousMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V1_JSON + "]");
    // Different profile
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_OTHER_JSON + "]");

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
      .thenReturn(Arrays.asList(previousMeasure));
    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(eventCache).createEvent(project, "Stop using 'Java One' (Java)", null, EventCategory.PROFILE, null);
  }

  @Test
  public void do_not_generate_event_on_first_analysis() {
    Measure newMeasure = new Measure(CoreMetrics.QUALITY_PROFILES, "[" + JAVA_V1_JSON + "]");

    when(decoratorContext.getMeasure(CoreMetrics.QUALITY_PROFILES)).thenReturn(newMeasure);

    when(languages.get("java")).thenReturn(Java.INSTANCE);

    decorator.decorate(project, decoratorContext);

    verify(eventCache, never()).createEvent(any(Resource.class), anyString(), anyString(), any(EventCategory.class), anyString());
  }
}

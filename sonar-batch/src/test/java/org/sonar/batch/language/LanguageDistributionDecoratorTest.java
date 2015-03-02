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

package org.sonar.batch.language;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LanguageDistributionDecoratorTest {

  @Mock
  DecoratorContext context;

  @Mock
  Resource resource;

  @Captor
  ArgumentCaptor<Measure> measureCaptor;

  LanguageDistributionDecorator decorator;

  @Before
  public void setUp() throws Exception {
    decorator = new LanguageDistributionDecorator();
  }

  @Test
  public void depended_upon_metric() {
    assertThat(decorator.generatesMetric()).isEqualTo(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
  }

  @Test
  public void depens_upon_metric() {
    assertThat(decorator.dependsUponMetric()).isEqualTo(CoreMetrics.LINES);
  }

  @Test
  public void save_ncloc_language_distribution_on_file() {
    Language language = mock(Language.class);
    when(language.getKey()).thenReturn("xoo");

    when(resource.getScope()).thenReturn(Scopes.FILE);
    when(resource.getLanguage()).thenReturn(language);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(measureCaptor.capture());

    Measure result = measureCaptor.getValue();
    assertThat(result.getMetric()).isEqualTo(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
    assertThat(result.getData()).isEqualTo("xoo=200");
  }

  @Test
  public void save_ncloc_language_distribution_on_file_without_language() {

    when(resource.getScope()).thenReturn(Scopes.FILE);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(measureCaptor.capture());

    Measure result = measureCaptor.getValue();
    assertThat(result.getMetric()).isEqualTo(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
    assertThat(result.getData()).isEqualTo("<null>=200");
  }

  @Test
  public void save_ncloc_language_distribution_on_project() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(context.getChildrenMeasures(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)).thenReturn(newArrayList(
      new Measure(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION, KeyValueFormat.format(ImmutableMap.of("java", 20))),
      new Measure(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION, KeyValueFormat.format(ImmutableMap.of("xoo", 150))),
      new Measure(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION, KeyValueFormat.format(ImmutableMap.of("xoo", 50)))
      ));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(measureCaptor.capture());

    Measure result = measureCaptor.getValue();
    assertThat(result.getMetric()).isEqualTo(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION);
    assertThat(result.getData()).isEqualTo("java=20;xoo=200");
  }

  @Test
  public void not_save_language_distribution_on_file_if_no_measure() {
    Language language = mock(Language.class);
    when(language.getKey()).thenReturn("xoo");

    when(resource.getScope()).thenReturn(Scopes.FILE);
    when(resource.getLanguage()).thenReturn(language);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(null);

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(measureCaptor.capture());
  }

  @Test
  public void not_save_language_distribution_on_project_if_no_chidren_measures() {
    when(resource.getScope()).thenReturn(Scopes.PROJECT);
    when(context.getChildrenMeasures(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)).thenReturn(Collections.<Measure>emptyList());

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(measureCaptor.capture());
  }

}

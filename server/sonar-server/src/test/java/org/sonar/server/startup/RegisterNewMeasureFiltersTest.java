/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Criterion;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;
import org.sonar.db.measure.MeasureFilterDao;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RegisterNewMeasureFiltersTest {
  private RegisterNewMeasureFilters registration;
  private MeasureFilterDao filterDao;
  private LoadedTemplateDao loadedTemplateDao;
  private FilterTemplate filterTemplate;

  @Before
  public void init() {
    filterDao = mock(MeasureFilterDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    filterTemplate = mock(FilterTemplate.class);

    registration = new RegisterNewMeasureFilters(new FilterTemplate[]{filterTemplate}, filterDao, loadedTemplateDao);
  }

  @Test
  public void should_insert_filters_on_start() {
    when(loadedTemplateDao.countByTypeAndKey(eq(LoadedTemplateDto.FILTER_TYPE), anyString())).thenReturn(0);
    when(filterTemplate.createFilter()).thenReturn(Filter.create());

    registration.start();

    verify(filterDao).insert(any(MeasureFilterDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_insert_nothing_if_templates_are_alreday_loaded() {
    when(loadedTemplateDao.countByTypeAndKey(eq(LoadedTemplateDto.FILTER_TYPE), anyString())).thenReturn(1);

    registration.start();

    verify(filterDao, never()).insert(any(MeasureFilterDto.class));
    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_register_filter() {
    when(filterTemplate.createFilter()).thenReturn(Filter.create());

    MeasureFilterDto filterDto = registration.register("Fake", filterTemplate.createFilter());

    assertThat(filterDto).isNotNull();
    verify(filterDao).insert(filterDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.FILTER_TYPE)));
  }

  @Test
  public void should_not_recreate_filter() {
    when(filterDao.selectSystemFilterByName("Fake")).thenReturn(new MeasureFilterDto());

    MeasureFilterDto filterDto = registration.register("Fake", null);

    assertThat(filterDto).isNull();
    verify(filterDao, never()).insert(filterDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.FILTER_TYPE)));
  }

  @Test
  public void should_create_dto_from_extension() {
    when(filterTemplate.createFilter()).thenReturn(Filter.create()
      .setFavouritesOnly(false)
      .setDisplayAs("list")
      .add(Criterion.createForMetric("complexity", Criterion.LT, 12f, false))
      .add(Criterion.createForMetric("lcom4", Criterion.GTE, 5f, false))
      .add(FilterColumn.create("metric", "distance", "ASC", false))
    );

    MeasureFilterDto dto = registration.createDtoFromExtension("Fake", filterTemplate.createFilter());

    assertThat(dto.getName()).isEqualTo("Fake");
    assertThat(dto.isShared()).isTrue();
    assertThat(dto.getData()).doesNotContain("onFavourites=true");
    assertThat(dto.getData()).contains("display=list");
    assertThat(dto.getData()).contains("c1_metric=complexity");
    assertThat(dto.getData()).contains("c1_op=lt");
    assertThat(dto.getData()).contains("c1_val=12.0");
    assertThat(dto.getData()).contains("c2_metric=lcom4");
    assertThat(dto.getData()).contains("c2_op=gte");
    assertThat(dto.getData()).contains("c2_val=5.0");
    assertThat(dto.getData()).contains("cols=metric:distance");
  }
}

/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterTemplate;
import org.sonar.core.filter.FilterDao;
import org.sonar.core.filter.FilterDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterNewFiltersTest {
  private RegisterNewFilters register;
  private FilterDao filterDao;
  private LoadedTemplateDao loadedTemplateDao;
  private FilterTemplate fakeFilterTemplate;

  @Before
  public void init() {
    filterDao = Mockito.mock(FilterDao.class);
    loadedTemplateDao = Mockito.mock(LoadedTemplateDao.class);

    fakeFilterTemplate = new FakeFilter();

    register = new RegisterNewFilters(new FilterTemplate[] {fakeFilterTemplate}, filterDao, loadedTemplateDao);
  }

  @Test
  public void should_start() {
    register.start();

    verify(filterDao).insert(any(FilterDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_register_filter_if_not_already_loaded() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.FILTER_TYPE, "Fake")).thenReturn(0);

    boolean shouldRegister = register.shouldRegister("Fake");

    assertThat(shouldRegister).isTrue();
  }

  @Test
  public void should_not_register_if_already_loaded() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.FILTER_TYPE, "Fake")).thenReturn(1);

    boolean shouldRegister = register.shouldRegister("Fake");

    assertThat(shouldRegister).isFalse();
  }

  @Test
  public void should_register_filter() {
    FilterDto filterDto = register.register("Fake", fakeFilterTemplate.createFilter());

    assertThat(filterDto).isNotNull();
    verify(filterDao).insert(filterDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.FILTER_TYPE)));
  }

  @Test
  public void should_create_dto_from_extension() {
    FilterDto dto = register.createDtoFromExtension("Fake", fakeFilterTemplate.createFilter());

    assertThat(dto.getUserId()).isNull();
    assertThat(dto.getName()).isEqualTo("Fake");
  }

  @Test
  public void should_compare_filter() {
    FilterDto f1 = new FilterDto().setName("foo");
    FilterDto f2 = new FilterDto().setName("Bar");

    List<FilterDto> filterDtos = Arrays.asList(f1, f2);
    Collections.sort(filterDtos, new RegisterNewFilters.FilterDtoComparator());

    assertThat(filterDtos).onProperty("name").containsExactly("Bar", "foo");
  }

  public class FakeFilter extends FilterTemplate {
    @Override
    public String getName() {
      return "Fake";
    }

    @Override
    public Filter createFilter() {
      Filter filter = Filter.create();
      return filter;
    }
  }
}

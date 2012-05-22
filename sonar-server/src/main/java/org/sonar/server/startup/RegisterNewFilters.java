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

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.web.Criterion;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;
import org.sonar.core.filter.CriterionDto;
import org.sonar.core.filter.FilterColumnDto;
import org.sonar.core.filter.FilterDao;
import org.sonar.core.filter.FilterDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.List;

/**
 * @since 3.1
 */
public final class RegisterNewFilters {
  private static final Logger LOG = LoggerFactory.getLogger(RegisterNewFilters.class);

  private final List<FilterTemplate> filterTemplates;
  private final FilterDao filterDao;
  private final LoadedTemplateDao loadedTemplateDao;

  /**
   * Constructor used in case there is no {@link FilterTemplate}.
   */
  public RegisterNewFilters(FilterDao filterDao, LoadedTemplateDao loadedTemplateDao) {
    this(new FilterTemplate[0], filterDao, loadedTemplateDao);
  }

  public RegisterNewFilters(FilterTemplate[] filterTemplates, FilterDao filterDao, LoadedTemplateDao loadedTemplateDao) {
    this.filterTemplates = ImmutableList.copyOf(filterTemplates);
    this.filterDao = filterDao;
    this.loadedTemplateDao = loadedTemplateDao;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register filters");

    for (FilterTemplate template : filterTemplates) {
      if (shouldRegister(template.getName())) {
        Filter filter = template.createFilter();
        FilterDto dto = register(template.getName(), filter);
      }
    }

    profiler.stop();
  }

  private boolean shouldRegister(String filterName) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.FILTER_TYPE, filterName) == 0;
  }

  protected FilterDto register(String name, Filter filter) {
    FilterDto dto = null;
    if (filterDao.findFilter(name) == null) {
      dto = createDtoFromExtension(name, filter);
      filterDao.insert(dto);
    }
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplateDto(name, LoadedTemplateDto.FILTER_TYPE));
    return dto;
  }

  private static void addCriteria(FilterDto filterDto, String family, String operator, String textValue) {
    if (textValue != null) {
      filterDto.add(new CriterionDto().setFamily(family).setOperator(operator).setTextValue(textValue));
    }
  }

  protected FilterDto createDtoFromExtension(String name, Filter filter) {
    FilterDto filterDto = new FilterDto()
        .setName(name)
        .setPageSize((long) filter.getPageSize())
        .setShared(filter.isShared())
        .setFavourites(filter.isFavouritesOnly())
        .setDefaultView(filter.getDefaultPeriod());

    addCriteria(filterDto, "key", "=", filter.getResourceKeyLike());
    addCriteria(filterDto, "name", "=", filter.getResourceNameLike());
    addCriteria(filterDto, "language", "=", filter.getLanguage());
    addCriteria(filterDto, "qualifier", "=", filter.getSearchFor());
    
    for (Criterion criterion : filter.getCriteria()) {
      filterDto.add(new CriterionDto()
          .setFamily(criterion.getFamily())
          .setKey(criterion.getKey())
          .setOperator(criterion.getOperator())
          .setTextValue(criterion.getTextValue())
          .setValue(criterion.getValue())
          .setVariation(criterion.isVariation()));
    }

    for (FilterColumn column : filter.getColumns()) {
      filterDto.add(new FilterColumnDto()
          .setFamily(column.getFamily())
          .setKey(column.getKey())
          .setOrderIndex((long) column.getOrderIndex())
          .setSortDirection(column.getSortDirection())
          .setVariation(column.isVariation()));
    }

    return filterDto;
  }
}

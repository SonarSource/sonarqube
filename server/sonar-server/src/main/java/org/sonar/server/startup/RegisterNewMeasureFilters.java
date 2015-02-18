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
package org.sonar.server.startup;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.api.web.Criterion;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;
import org.sonar.core.measure.db.MeasureFilterDao;
import org.sonar.core.measure.db.MeasureFilterDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.Date;
import java.util.List;

/**
 * @since 3.1
 */
public final class RegisterNewMeasureFilters {
  private static final Logger LOG = Loggers.get(RegisterNewMeasureFilters.class);

  private final List<FilterTemplate> filterTemplates;
  private final MeasureFilterDao filterDao;
  private final LoadedTemplateDao loadedTemplateDao;

  public RegisterNewMeasureFilters(FilterTemplate[] filterTemplates, MeasureFilterDao filterDao, LoadedTemplateDao loadedTemplateDao) {
    this.filterTemplates = ImmutableList.copyOf(filterTemplates);
    this.filterDao = filterDao;
    this.loadedTemplateDao = loadedTemplateDao;
  }

  /**
   * Used when no plugin is defining some FilterTemplate
   */
  public RegisterNewMeasureFilters(MeasureFilterDao filterDao, LoadedTemplateDao loadedTemplateDao) {
    this(new FilterTemplate[] {}, filterDao, loadedTemplateDao);
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register measure filters");

    for (FilterTemplate template : filterTemplates) {
      if (shouldRegister(template.getName())) {
        Filter filter = template.createFilter();
        register(template.getName(), filter);
      }
    }

    profiler.stopDebug();
  }

  private boolean shouldRegister(String filterName) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.FILTER_TYPE, filterName) == 0;
  }

  protected MeasureFilterDto register(String name, Filter filter) {
    MeasureFilterDto dto = null;
    if (filterDao.findSystemFilterByName(name) == null) {
      LOG.info("Register measure filter: " + name);
      dto = createDtoFromExtension(name, filter);
      filterDao.insert(dto);
    }
    // and save the fact that is has now already been loaded
    loadedTemplateDao.insert(new LoadedTemplateDto(name, LoadedTemplateDto.FILTER_TYPE));
    return dto;
  }

  protected MeasureFilterDto createDtoFromExtension(String name, Filter filter) {
    Date now = new Date();
    String data = toData(filter);
    return new MeasureFilterDto()
      .setName(name)
      .setShared(true)
      .setUserId(null)
      .setCreatedAt(now)
      .setUpdatedAt(now)
      .setData(data);
  }

  static String toData(Filter filter) {
    List<String> fields = Lists.newArrayList();

    fields.add("display=" + filter.getDisplayAs());
    if (filter.isFavouritesOnly()) {
      fields.add("onFavourites=true");
    }
    if (filter.getPageSize() > 0) {
      fields.add("pageSize=" + filter.getPageSize());
    }
    appendCriteria(filter, fields);
    appendColumns(filter, fields);
    return Joiner.on("|").join(fields);
  }

  private static void appendCriteria(Filter filter, List<String> fields) {
    int metricCriterionId = 1;
    for (Criterion criterion : filter.getCriteria()) {
      if ("qualifier".equals(criterion.getFamily())) {
        fields.add("qualifiers=" + criterion.getTextValue());
      } else if ("name".equals(criterion.getFamily())) {
        fields.add("nameSearch=" + criterion.getTextValue());
      } else if ("key".equals(criterion.getFamily())) {
        fields.add("keySearch=" + criterion.getTextValue());
      } else if ("language".equals(criterion.getFamily())) {
        fields.add("languages=" + criterion.getTextValue());
      } else if ("date".equals(criterion.getFamily())) {
        if ("<".equals(criterion.getOperator())) {
          fields.add("ageMaxDays=" + criterion.getValue());
        } else if (">".equals(criterion.getOperator())) {
          fields.add("ageMinDays=" + criterion.getValue());
        }
      } else if ("direct-children".equals(criterion.getFamily()) && "true".equals(criterion.getTextValue())) {
        fields.add("onBaseComponents=true");
      } else if ("metric".equals(criterion.getFamily()) && StringUtils.isNotBlank(criterion.getKey())
        && StringUtils.isNotBlank(criterion.getOperator()) && criterion.getValue() != null) {
        fields.add("c" + metricCriterionId + "_metric=" + criterion.getKey());
        fields.add("c" + metricCriterionId + "_op=" + criterion.getOperator());
        fields.add("c" + metricCriterionId + "_val=" + criterion.getValue());
        metricCriterionId += 1;
      }
    }
  }

  private static void appendColumns(Filter filter, List<String> fields) {
    List<String> columnFields = Lists.newArrayList();
    for (FilterColumn column : filter.getColumns()) {
      StringBuilder columnKey = new StringBuilder().append(column.getFamily());
      if (StringUtils.isNotBlank(column.getKey()) && !column.isVariation()) {
        columnKey.append(":").append(column.getKey());
      }
      columnFields.add(columnKey.toString());
    }
    if (!columnFields.isEmpty()) {
      fields.add("cols=" + Joiner.on(",").join(columnFields));
    }
  }
}

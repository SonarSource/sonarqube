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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import FilterBar from './measures-filter-bar';
import BaseFilters from '../../components/navigator/filters/base-filters';
import CheckboxFilterView from '../../components/navigator/filters/checkbox-filters';
import ChoiceFilters from '../../components/navigator/filters/choice-filters';
import AjaxSelectFilters from '../../components/navigator/filters/ajax-select-filters';
import FavoriteFilters from '../../components/navigator/filters/favorite-filters';
import RangeFilters from '../../components/navigator/filters/range-filters';
import StringFilterView from '../../components/navigator/filters/string-filters';
import MetricFilterView from '../../components/navigator/filters/metric-filters';
import { translate } from '../../helpers/l10n';

const NavigatorApp = new Marionette.Application();
const newLastAnalysisFilter = function () {
  return new BaseFilters.Filter({
    name: translate('measure_filter.criteria.last_analysis'),
    propertyFrom: 'ageMinDays',
    propertyTo: 'ageMaxDays',
    type: RangeFilters.RangeFilterView,
    placeholder: translate('measure_filter.criteria.age.days'),
    enabled: false,
    optional: true
  });
};
const newMetricFilter = function (property) {
  return new BaseFilters.Filter({
    name: translate('measure_filter.criteria.metric'),
    property,
    type: MetricFilterView,
    metrics: window.SS.metrics,
    periods: window.SS.metricPeriods,
    operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
    enabled: false,
    optional: true
  });
};
const newNameFilter = function () {
  return new BaseFilters.Filter({
    name: translate('measure_filter.name_contains'),
    property: 'nameSearch',
    type: StringFilterView,
    enabled: false,
    optional: true
  });
};
const newAlertFilter = function () {
  return new BaseFilters.Filter({
    name: translate('measure_filter.criteria.alert'),
    property: 'alertLevels[]',
    type: ChoiceFilters.ChoiceFilterView,
    enabled: false,
    optional: true,
    choices: {
      'error': translate('measure_filter.criteria.alert.error'),
      'warn': translate('measure_filter.criteria.alert.warn'),
      'ok': translate('measure_filter.criteria.alert.ok')
    }
  });
};
const init = function () {
  NavigatorApp.addRegions({ filtersRegion: '.navigator-filters' });

  this.filters = new BaseFilters.Filters();

  if (_.isObject(window.SS.favorites)) {
    this.filters.add([
      new BaseFilters.Filter({
        type: FavoriteFilters.FavoriteFilterView,
        enabled: true,
        optional: false,
        choices: window.SS.favorites,
        favoriteUrl: '/measures/filter',
        manageUrl: '/measures/manage'
      })
    ]);
  }

  this.filters.add([
    new BaseFilters.Filter({
      name: translate('measure_filter.criteria.components'),
      property: 'qualifiers[]',
      type: ChoiceFilters.ChoiceFilterView,
      enabled: true,
      optional: false,
      choices: window.SS.qualifiers,
      defaultValue: translate('any')
    }),

    new BaseFilters.Filter({
      name: translate('measure_filter.criteria.components_of'),
      property: 'base',
      type: AjaxSelectFilters.ComponentFilterView,
      multiple: false,
      enabled: false,
      optional: true
    }),

    new BaseFilters.Filter({
      name: translate('measure_filter.criteria.only_favorites'),
      property: 'onFavourites',
      type: CheckboxFilterView,
      enabled: false,
      optional: true
    }),

    new BaseFilters.Filter({
      name: translate('measure_filter.criteria.date'),
      propertyFrom: 'fromDate',
      propertyTo: 'toDate',
      type: RangeFilters.DateRangeFilterView,
      enabled: false,
      optional: true
    }),

    new BaseFilters.Filter({
      name: translate('measure_filter.criteria.key_contains'),
      property: 'keySearch',
      type: StringFilterView,
      enabled: false,
      optional: true
    })
  ]);

  this.filters.add([
    newLastAnalysisFilter(),
    newMetricFilter('c3'),
    newMetricFilter('c2'),
    newMetricFilter('c1'),
    newNameFilter(),
    newAlertFilter()
  ]);

  this.filterBarView = new FilterBar({
    collection: this.filters,
    extra: {
      sort: '',
      asc: false
    }
  });

  this.filtersRegion.show(this.filterBarView);

  if (window.queryParams) {
    NavigatorApp.filterBarView.restoreFromQuery(window.queryParams);
  }
  key.setScope('list');
};

NavigatorApp.on('start', function () {
  init.call(NavigatorApp);
});

window.sonarqube.appStarted.then(options => NavigatorApp.start(options));

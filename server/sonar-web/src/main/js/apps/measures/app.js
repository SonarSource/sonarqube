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
define(
    [
      './measures-filter-bar',
      'components/navigator/filters/base-filters',
      'components/navigator/filters/checkbox-filters',
      'components/navigator/filters/choice-filters',
      'components/navigator/filters/ajax-select-filters',
      'components/navigator/filters/favorite-filters',
      'components/navigator/filters/range-filters',
      'components/navigator/filters/string-filters',
      'components/navigator/filters/metric-filters'
    ],
    function (FilterBar, BaseFilters, CheckboxFilterView, ChoiceFilters, AjaxSelectFilters,
              FavoriteFilters, RangeFilters, StringFilterView, MetricFilterView) {

      var NavigatorApp = new Marionette.Application();


      NavigatorApp.addRegions({
        filtersRegion: '.navigator-filters'
      });


      NavigatorApp.addInitializer(function () {
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
            })]);
        }

        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.components,
            property: 'qualifiers[]',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: true,
            optional: false,
            choices: window.SS.qualifiers,
            defaultValue: window.SS.phrases.any
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.componentsOf,
            property: 'base',
            type: AjaxSelectFilters.ComponentFilterView,
            multiple: false,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.favoritesOnly,
            property: 'onFavourites',
            type: CheckboxFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.date,
            propertyFrom: 'fromDate',
            propertyTo: 'toDate',
            type: RangeFilters.DateRangeFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.keyContains,
            property: 'keySearch',
            type: StringFilterView,
            enabled: false,
            optional: true
          })
        ]);
      });

      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.lastAnalysis,
            propertyFrom: 'ageMinDays',
            propertyTo: 'ageMaxDays',
            type: RangeFilters.RangeFilterView,
            placeholder: window.SS.phrases.days,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c3',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c2',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c1',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.nameContains,
            property: 'nameSearch',
            type: StringFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.alert,
            property: 'alertLevels[]',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: false,
            optional: true,
            choices: {
              'error': window.SS.phrases.error,
              'warn': window.SS.phrases.warning,
              'ok': window.SS.phrases.ok
            }
          })
        ]);


        this.filterBarView = new FilterBar({
          collection: this.filters,
          extra: {
            sort: '',
            asc: false
          }
        });


        this.filtersRegion.show(this.filterBarView);
      });


      NavigatorApp.start();
      if (window.queryParams) {
        NavigatorApp.filterBarView.restoreFromQuery(window.queryParams);
      }
      key.setScope('list');

    });

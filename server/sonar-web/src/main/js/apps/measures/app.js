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

var NavigatorApp = new Marionette.Application(),

    newLastAnalysisFilter = function () {
      return new BaseFilters.Filter({
        name: window.SS.phrases.lastAnalysis,
        propertyFrom: 'ageMinDays',
        propertyTo: 'ageMaxDays',
        type: RangeFilters.RangeFilterView,
        placeholder: window.SS.phrases.days,
        enabled: false,
        optional: true
      });
    },

    newMetricFilter = function (property) {
      return new BaseFilters.Filter({
        name: window.SS.phrases.metric,
        property: property,
        type: MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      });
    },

    newNameFilter = function () {
      return new BaseFilters.Filter({
        name: window.SS.phrases.nameContains,
        property: 'nameSearch',
        type: StringFilterView,
        enabled: false,
        optional: true
      });
    },

    newAlertFilter = function () {
      return new BaseFilters.Filter({
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
      });
    },

    init = function () {
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

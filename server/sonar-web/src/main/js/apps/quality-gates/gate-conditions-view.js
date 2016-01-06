/*
 * SonarQube :: Web
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
import Condition from './condition';
import ConditionView from './gate-condition-view';
import ConditionsEmptyView from './gate-conditions-empty-view';
import Template from './templates/quality-gate-detail-conditions.hbs';
import { translate } from '../../helpers/l10n';

export default Marionette.CompositeView.extend({
  template: Template,
  childView: ConditionView,
  emptyView: ConditionsEmptyView,
  childViewContainer: '.js-conditions',

  ui: {
    metricSelect: '#quality-gate-new-condition-metric'
  },

  events: {
    'click .js-show-more': 'showMoreIntroduction',
    'change @ui.metricSelect': 'addCondition'
  },

  childViewOptions: function () {
    return {
      canEdit: this.options.canEdit,
      gate: this.model,
      collectionView: this,
      metrics: this.options.metrics,
      periods: this.options.periods
    };
  },

  onRender: function () {
    this.ui.metricSelect.select2({
      allowClear: false,
      width: '250px',
      placeholder: translate('alerts.select_metric')
    });
  },

  showMoreIntroduction: function () {
    this.$('.js-show-more').addClass('hidden');
    this.$('.js-more').removeClass('hidden');
  },

  addCondition: function () {
    var metric = this.ui.metricSelect.val();
    this.ui.metricSelect.select2('val', '');
    var condition = new Condition({ metric: metric });
    this.collection.add(condition);
  },

  groupedMetrics: function () {
    var metrics = this.options.metrics.filter(function (metric) {
      return !metric.hidden;
    });
    metrics = _.groupBy(metrics, 'domain');
    metrics = _.map(metrics, function (list, domain) {
      return {
        domain: domain,
        metrics: _.sortBy(list, 'short_name')
      };
    });
    return _.sortBy(metrics, 'domain');
  },

  serializeData: function () {
    return _.extend(Marionette.CompositeView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit,
      metricGroups: this.groupedMetrics()
    });
  }
});

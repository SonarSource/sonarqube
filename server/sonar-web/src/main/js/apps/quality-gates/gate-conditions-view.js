import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Condition from './condition';
import ConditionView from './gate-condition-view';
import ConditionsEmptyView from './gate-conditions-empty-view';
import './templates';

export default Marionette.CompositeView.extend({
  template: Templates['quality-gate-detail-conditions'],
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
      placeholder: t('alerts.select_metric')
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



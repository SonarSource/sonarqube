import _ from 'underscore';
import ModalForm from 'components/common/modal-form';
import Metrics from 'apps/metrics/metrics';
import './templates';

export default ModalForm.extend({
  template: Templates['custom-measures-form'],

  initialize: function () {
    this.metrics = new Metrics();
    this.listenTo(this.metrics, 'reset', this.render);
    this.metrics.fetch({ reset: true });
  },

  onRender: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-custom-measure-metric').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onDestroy: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function (e) {
    this._super(e);
    this.sendRequest();
  },

  getAvailableMetrics: function () {
    var takenMetrics = this.collection.getTakenMetrics();
    return this.metrics.toJSON().filter(function (metric) {
      return takenMetrics.indexOf(metric.id) === -1;
    });
  },

  serializeData: function () {
    var metrics = this.getAvailableMetrics(),
        isNew = !this.model;
    return _.extend(this._super(), {
      metrics: metrics,
      canCreateMetric: !isNew || (isNew && metrics.length > 0)
    });
  }
});



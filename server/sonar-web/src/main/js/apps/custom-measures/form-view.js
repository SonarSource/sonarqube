import _ from 'underscore';
import ModalForm from '../../components/common/modal-form';
import Metrics from '../metrics/metrics';
import Template from './templates/custom-measures-form.hbs';

export default ModalForm.extend({
  template: Template,

  initialize: function () {
    this.metrics = new Metrics();
    this.listenTo(this.metrics, 'reset', this.render);
    this.metrics.fetch({ reset: true });
  },

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-custom-measure-metric').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onDestroy: function () {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
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
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      metrics: metrics,
      canCreateMetric: !isNew || (isNew && metrics.length > 0)
    });
  }
});



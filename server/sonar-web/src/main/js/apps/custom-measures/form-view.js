define([
  'components/common/modal-form',
  'apps/metrics/metrics',
  './templates'
], function (ModalForm, Metrics) {

  return ModalForm.extend({
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

    serializeData: function () {
      // TODO show only not taken metrics
      return _.extend(this._super(), { metrics: this.metrics.toJSON() });
    }
  });

});

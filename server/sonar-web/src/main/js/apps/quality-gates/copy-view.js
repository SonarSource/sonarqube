import FormView from './form-view';

export default FormView.extend({
  method: 'copy',

  prepareRequest: function () {
    var that = this;
    var url = baseUrl + '/api/qualitygates/copy',
        name = this.$('#quality-gate-form-name').val(),
        options = {
          url: url,
          data: { id: this.model.id, name: name }
        };
    return this.sendRequest(options)
        .done(function (r) {
          var gate = that.addGate(r);
          gate.trigger('select', gate);
        });
  }
});



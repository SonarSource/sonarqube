define([
  './form-view'
], function (FormView) {

  return FormView.extend({
    method: 'rename',

    prepareRequest: function () {
      var that = this;
      var url = baseUrl + '/api/qualitygates/rename',
          name = this.$('#quality-gate-form-name').val(),
          options = {
            url: url,
            data: { id: this.model.id, name: name }
          };
      return this.sendRequest(options)
          .done(function (r) {
            that.model.set(r);
          });
    }
  });

});

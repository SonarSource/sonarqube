define([
  'components/common/modal-form',
  './gate',
  './templates'
], function (ModalForm, Gate) {

  return ModalForm.extend({
    template: Templates['quality-gate-form'],

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.disableForm();
      this.prepareRequest();
    },

    sendRequest: function (options) {
      var that = this,
          opts = _.defaults(options || {}, {
            type: 'POST',
            statusCode: {
              // do not show global error
              400: null
            }
          });
      return Backbone.ajax(opts)
          .done(function () {
            that.close();
          }).fail(function (jqXHR) {
            that.enableForm();
            that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
          });
    },

    addGate: function (attrs) {
      var gate = new Gate(attrs);
      this.collection.add(gate, { merge: true });
      return gate;
    },

    serializeData: function () {
      return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
        method: this.method
      });
    }
  });

});

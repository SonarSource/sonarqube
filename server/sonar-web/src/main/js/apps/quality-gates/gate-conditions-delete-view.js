define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['quality-gates-condition-delete'],

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.disableForm();
      this.sendRequest();
    },

    sendRequest: function () {
      var that = this,
          options = {
            statusCode: {
              // do not show global error
              400: null
            }
          };
      return this.model.destroy(options)
          .done(function () {
            that.close();
          }).fail(function (jqXHR) {
            that.enableForm();
            that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
          });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        metric: this.options.metric
      });
    }
  });

});

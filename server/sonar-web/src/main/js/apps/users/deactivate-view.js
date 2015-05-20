define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['users-deactivate'],

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest();
    },

    sendRequest: function () {
      var that = this,
          collection = this.model.collection;
      return this.model.destroy({
        wait: true,
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        collection.total--;
        that.close();
      }).fail(function (jqXHR) {
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});

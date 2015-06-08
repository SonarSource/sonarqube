define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['provisioning-bulk-delete'],

    onFormSubmit: function (e) {
      this._super(e);
      this.sendRequest();
    },

    sendRequest: function () {
      var that = this,
          selected = _.pluck(this.collection.where({ selected: true }), 'id');
      return this.collection.bulkDelete(selected, {
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.collection.refresh();
        that.close();
      }).fail(function (jqXHR) {
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});

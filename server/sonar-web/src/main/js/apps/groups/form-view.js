define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['groups-form'],

    onRender: function () {
      this._super();
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onDestroy: function () {
      this._super();
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onFormSubmit: function (e) {
      this._super(e);
      this.sendRequest();
    }
  });

});

import ModalForm from '../../components/common/modal-form';
import Template from './templates/groups-delete.hbs';

export default ModalForm.extend({
  template: Template,

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
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  },

  showErrors: function (errors, warnings) {
    this.$('.js-modal-text').addClass('hidden');
    this.disableForm();
    ModalForm.prototype.showErrors.call(this, errors, warnings);
  }
});



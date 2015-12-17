import ModalForm from '../../components/common/modal-form';
import Template from './templates/metrics-delete.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function (e) {
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
      collection.refresh();
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});



import _ from 'underscore';
import ModalForm from 'components/common/modal-form';
import './templates';

export default ModalForm.extend({
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
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});



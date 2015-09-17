import $ from 'jquery';
import ModalFormView from 'components/common/modal-form';
import './templates';

export default ModalFormView.extend({
  template: Templates['quality-profiles-delete-profile'],

  modelEvents: {
    'destroy': 'destroy'
  },

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.sendRequest();
  },

  sendRequest: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/delete',
        options = { profileKey: this.model.get('key') };
    return $.ajax({
      type: 'POST',
      url: url,
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.model.collection.fetch();
      that.model.trigger('destroy', that.model, that.model.collection);
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      that.enableForm();
    });
  }
});



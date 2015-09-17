import $ from 'jquery';
import ModalFormView from 'components/common/modal-form';
import './templates';

export default ModalFormView.extend({
  template: Templates['quality-profiles-rename-profile'],

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/rename',
        name = this.$('#rename-profile-name').val(),
        options = {
          key: this.model.get('key'),
          name: name
        };
    return $.ajax({
      type: 'POST',
      url: url,
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.model.set({ name: name });
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});



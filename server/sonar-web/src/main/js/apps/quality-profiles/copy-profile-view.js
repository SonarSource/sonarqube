import $ from 'jquery';
import ModalFormView from 'components/common/modal-form';
import Profile from './profile';
import './templates';

export default ModalFormView.extend({
  template: Templates['quality-profiles-copy-profile'],

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.sendRequest();
  },

  sendRequest: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/copy',
        name = this.$('#copy-profile-name').val(),
        options = {
          fromKey: this.model.get('key'),
          toName: name
        };
    return $.ajax({
      type: 'POST',
      url: url,
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function (r) {
      that.addProfile(r);
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  },

  addProfile: function (profileData) {
    var profile = new Profile(profileData);
    this.model.collection.add([profile]);
    profile.trigger('select', profile);
  }
});



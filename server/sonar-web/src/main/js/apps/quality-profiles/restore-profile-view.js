import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from '../../components/common/modal-form';
import uploader from '../../components/common/file-upload';
import Profile from './profile';
import Template from './templates/quality-profiles-restore-profile.hbs';

export default ModalFormView.extend({
  template: Template,

  onFormSubmit: function (e) {
    var that = this;
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    uploader({ form: $(e.currentTarget) }).done(function (r) {
      if (_.isArray(r.errors) || _.isArray(r.warnings)) {
        that.showErrors(r.errors, r.warnings);
      } else {
        that.addProfile(r.profile);
        that.destroy();
      }
    });
  },

  addProfile: function (profileData) {
    var profile = new Profile(profileData);
    this.collection.add([profile], { merge: true });
    var addedProfile = this.collection.get(profile.id);
    if (addedProfile != null) {
      addedProfile.trigger('select', addedProfile);
    }
  }
});

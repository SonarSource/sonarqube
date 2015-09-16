define([
  'components/common/modal-form',
  'components/common/file-upload',
  './profile',
  './templates'
], function (ModalFormView, uploader, Profile) {

  var $ = jQuery;

  return ModalFormView.extend({
    template: Templates['quality-profiles-restore-profile'],

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

});

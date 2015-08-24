define([
  'backbone.marionette',
  './copy-profile-view',
  './rename-profile-view',
  './delete-profile-view',
  './templates'
], function (Marionette, ProfileCopyView, ProfileRenameView, ProfileDeleteView) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['quality-profiles-profile-header'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click #quality-profile-backup': 'onBackupClick',
      'click #quality-profile-copy': 'onCopyClick',
      'click #quality-profile-rename': 'onRenameClick',
      'click #quality-profile-set-as-default': 'onDefaultClick',
      'click #quality-profile-delete': 'onDeleteClick'
    },

    onBackupClick: function (e) {
      $(e.currentTarget).blur();
    },

    onCopyClick: function (e) {
      e.preventDefault();
      this.copy();
    },

    onRenameClick: function (e) {
      e.preventDefault();
      this.rename();
    },

    onDefaultClick: function (e) {
      e.preventDefault();
      this.setAsDefault();
    },

    onDeleteClick: function (e) {
      e.preventDefault();
      this.deleteProfile();
    },

    copy: function () {
      new ProfileCopyView({ model: this.model }).render();
    },

    rename: function () {
      new ProfileRenameView({ model: this.model }).render();
    },

    setAsDefault: function () {
      this.model.trigger('setAsDefault', this.model);
    },

    deleteProfile: function () {
      new ProfileDeleteView({ model: this.model }).render();
    },

    serializeData: function () {
      var key = this.model.get('key');
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        encodedKey: encodeURIComponent(key),
        canWrite: this.options.canWrite
      });
    }
  });

});

import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ProfileCopyView from './copy-profile-view';
import ProfileRenameView from './rename-profile-view';
import ProfileDeleteView from './delete-profile-view';
import './templates';

export default Marionette.ItemView.extend({
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
    this.delete();
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

  delete: function () {
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



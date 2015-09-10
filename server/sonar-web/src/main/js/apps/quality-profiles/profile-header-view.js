/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  './copy-profile-view',
  './rename-profile-view',
  './delete-profile-view',
  './templates'
], function (ProfileCopyView, ProfileRenameView, ProfileDeleteView) {

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

});

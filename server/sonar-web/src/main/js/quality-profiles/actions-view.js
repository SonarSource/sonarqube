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
  'quality-profiles/create-profile-view',
  'quality-profiles/restore-profile-view',
  'quality-profiles/restore-built-in-profiles-view',
  'templates/quality-profiles'
], function (CreateProfileView, RestoreProfileView, RestoreBuiltInProfilesView) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['quality-profiles-actions'],

    events: {
      'click #quality-profiles-create': 'onCreateClick',
      'click #quality-profiles-restore': 'onRestoreClick',
      'click #quality-profiles-restore-built-in': 'onRestoreBuiltInClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.create();
    },

    onRestoreClick: function (e) {
      e.preventDefault();
      this.restore();
    },

    onRestoreBuiltInClick: function (e) {
      e.preventDefault();
      this.restoreBuiltIn();
    },

    create: function () {
      var that = this;
      $.when(this.requestLanguages(), this.requestImporters()).done(function () {
        new CreateProfileView({
          collection: that.collection,
          languages: that.languages,
          importers: that.importers
        }).render();
      });
    },

    restore: function () {
      new RestoreProfileView({
        collection: this.collection
      }).render();
    },

    restoreBuiltIn: function () {
      var that = this;
      this.requestLanguages().done(function (r) {
        new RestoreBuiltInProfilesView({
          collection: that.collection,
          languages: r.languages
        }).render();
      });
    },

    requestLanguages: function () {
      var that = this,
          url = baseUrl + '/api/languages/list';
      return $.get(url).done(function (r) {
        that.languages = r.languages;
      });
    },

    requestImporters: function () {
      var that = this,
          url = baseUrl + '/api/qualityprofiles/importers';
      return $.get(url).done(function (r) {
        that.importers = r.importers;
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.canWrite
      });
    }
  });

});

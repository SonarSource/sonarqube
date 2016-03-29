/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import CreateProfileView from './create-profile-view';
import RestoreProfileView from './restore-profile-view';
import RestoreBuiltInProfilesView from './restore-built-in-profiles-view';
import Template from './templates/quality-profiles-actions.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #quality-profiles-create': 'onCreateClick',
    'click #quality-profiles-restore': 'onRestoreClick',
    'click #quality-profiles-restore-built-in': 'onRestoreBuiltInClick',

    'click .js-filter-by-language': 'onLanguageClick'
  },

  onCreateClick (e) {
    e.preventDefault();
    this.create();
  },

  onRestoreClick (e) {
    e.preventDefault();
    this.restore();
  },

  onRestoreBuiltInClick (e) {
    e.preventDefault();
    this.restoreBuiltIn();
  },

  onLanguageClick (e) {
    e.preventDefault();
    const language = $(e.currentTarget).data('language');
    this.filterByLanguage(language);
  },

  create () {
    const that = this;
    this.requestImporters().done(function () {
      new CreateProfileView({
        collection: that.collection,
        languages: that.languages,
        importers: that.importers
      }).render();
    });
  },

  restore () {
    new RestoreProfileView({
      collection: this.collection
    }).render();
  },

  restoreBuiltIn () {
    new RestoreBuiltInProfilesView({
      collection: this.collection,
      languages: this.languages
    }).render();
  },

  requestLanguages () {
    const that = this;
    const url = window.baseUrl + '/api/languages/list';
    return $.get(url).done(function (r) {
      that.languages = r.languages;
    });
  },

  requestImporters () {
    const that = this;
    const url = window.baseUrl + '/api/qualityprofiles/importers';
    return $.get(url).done(function (r) {
      that.importers = r.importers;
    });
  },

  filterByLanguage (language) {
    this.selectedLanguage = _.findWhere(this.languages, { key: language });
    this.render();
    this.collection.trigger('filter', language);
  },

  serializeData () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.canWrite,
      languages: this.languages,
      selectedLanguage: this.selectedLanguage
    });
  }
});



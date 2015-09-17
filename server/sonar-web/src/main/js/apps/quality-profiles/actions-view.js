import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import CreateProfileView from './create-profile-view';
import RestoreProfileView from './restore-profile-view';
import RestoreBuiltInProfilesView from './restore-built-in-profiles-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['quality-profiles-actions'],

  events: {
    'click #quality-profiles-create': 'onCreateClick',
    'click #quality-profiles-restore': 'onRestoreClick',
    'click #quality-profiles-restore-built-in': 'onRestoreBuiltInClick',

    'click .js-filter-by-language': 'onLanguageClick'
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

  onLanguageClick: function (e) {
    e.preventDefault();
    var language = $(e.currentTarget).data('language');
    this.filterByLanguage(language);
  },

  create: function () {
    var that = this;
    this.requestImporters().done(function () {
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
    new RestoreBuiltInProfilesView({
      collection: this.collection,
      languages: this.languages
    }).render();
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

  filterByLanguage: function (language) {
    this.selectedLanguage = _.findWhere(this.languages, { key: language });
    this.render();
    this.collection.trigger('filter', language);
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.canWrite,
      languages: this.languages,
      selectedLanguage: this.selectedLanguage
    });
  }
});



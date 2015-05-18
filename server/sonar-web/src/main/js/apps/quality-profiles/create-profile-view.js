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
  'components/common/modal-form',
  'components/common/file-upload',
  './profile',
  './templates'
], function (ModalFormView, uploader, Profile) {

  var $ = jQuery;

  return ModalFormView.extend({
    template: Templates['quality-profiles-create-profile'],

    events: function () {
      return _.extend(ModalFormView.prototype.events.apply(this, arguments), {
        'change #create-profile-language': 'onLanguageChange'
      });
    },

    onFormSubmit: function (e) {
      ModalFormView.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest(e);
    },

    onRender: function () {
      ModalFormView.prototype.onRender.apply(this, arguments);
      this.$('select').select2({
        width: '250px',
        minimumResultsForSearch: 50
      });
      this.onLanguageChange();
    },

    onLanguageChange: function () {
      var that = this;
      var language = this.$('#create-profile-language').val();
      var importers = this.getImportersForLanguages(language);
      this.$('.js-importer').each(function () {
        that.emptyInput($(this));
        $(this).addClass('hidden');
      });
      importers.forEach(function (importer) {
        that.$('.js-importer[data-key="' + importer.key + '"]').removeClass('hidden');
      });
    },

    emptyInput: function (e) {
      e.wrap('<form>').closest('form').get(0).reset();
      e.unwrap();
    },

    sendRequest: function (e) {
      var that = this;
      uploader({ form: $(e.currentTarget) }).done(function (r) {
        if (_.isArray(r.errors) || _.isArray(r.warnings)) {
          that.showErrors(r.errors, r.warnings);
        } else {
          that.addProfile(r.profile);
          that.close();
        }
      });
    },

    addProfile: function (profileData) {
      var profile = new Profile(profileData);
      this.collection.add([profile]);
      profile.trigger('select', profile);
    },

    getImportersForLanguages: function (language) {
      if (language != null) {
        return this.options.importers.filter(function (importer) {
          return importer.languages.indexOf(language) !== -1;
        });
      } else {
        return [];
      }
    },

    serializeData: function () {
      return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
        languages: this.options.languages,
        importers: this.options.importers
      });
    }
  });

});

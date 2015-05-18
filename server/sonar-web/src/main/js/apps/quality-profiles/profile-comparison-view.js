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
    './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['quality-profile-comparison'],

    events: {
      'submit #quality-profile-comparison-form': 'onFormSubmit'
    },

    onRender: function () {
      this.$('select').select2({
        width: '250px',
        minimumResultsForSearch: 50
      });
    },

    onFormSubmit: function (e) {
      e.preventDefault();
      var withKey = this.$('#quality-profile-comparison-with-key').val();
      this.model.compareWith(withKey);
    },

    getProfilesForComparison: function () {
      var profiles = this.model.collection.toJSON(),
          key = this.model.id,
          language = this.model.get('language');
      return profiles.filter(function (profile) {
        return profile.language === language && key !== profile.key;
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        profiles: this.getProfilesForComparison()
      });
    }
  });

});

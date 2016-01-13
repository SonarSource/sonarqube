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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/quality-profile-comparison.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'submit #quality-profile-comparison-form': 'onFormSubmit',
    'click .js-hide-comparison': 'onHideComparisonClick'
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

  onHideComparisonClick: function (e) {
    e.preventDefault();
    this.model.resetComparison();
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



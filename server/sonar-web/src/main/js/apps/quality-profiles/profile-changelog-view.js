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
import Marionette from 'backbone.marionette';
import Template from './templates/quality-profile-changelog.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'submit #quality-profile-changelog-form': 'onFormSubmit',
    'click .js-show-more-changelog': 'onShowMoreChangelogClick',
    'click .js-hide-changelog': 'onHideChangelogClick'
  },

  onFormSubmit: function (e) {
    e.preventDefault();
    this.model.fetchChangelog(this.getSearchParameters());
  },

  onShowMoreChangelogClick: function (e) {
    e.preventDefault();
    this.model.fetchMoreChangelog();
  },

  onHideChangelogClick: function (e) {
    e.preventDefault();
    this.model.resetChangelog();
  },

  getSearchParameters: function () {
    var form = this.$('#quality-profile-changelog-form');
    return {
      since: form.find('[name="since"]').val(),
      to: form.find('[name="to"]').val()
    };
  }
});



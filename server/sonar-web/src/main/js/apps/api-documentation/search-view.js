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
import Template from './templates/api-documentation-search.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  ui: {
    input: '.search-box-input'
  },

  events: {
    'keyup @ui.input': 'onChange',
    'search @ui.input': 'onChange'
  },

  initialize: function () {
    this.query = '';
    this.debouncedFilter = _.debounce(this.filter, 250);
  },

  onChange: function () {
    var query = this.ui.input.val();
    if (query === this.query) {
      return;
    }
    this.query = this.ui.input.val();
    this.debouncedFilter(query);
  },

  filter: function (query) {
    this.options.state.set({ query: query });
  }
});

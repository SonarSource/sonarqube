/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import { debounce } from 'lodash';
import Marionette from 'backbone.marionette';
import Template from './templates/groups-search.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  ui: {
    reset: '.js-reset'
  },

  events: {
    'submit #groups-search-form': 'onFormSubmit',
    'search #groups-search-query': 'initialOnKeyUp',
    'keyup #groups-search-query': 'initialOnKeyUp',
    'click .js-reset': 'onResetClick'
  },

  initialize() {
    this._bufferedValue = null;
    this.debouncedOnKeyUp = debounce(this.onKeyUp, 400);
  },

  onRender() {
    this.delegateEvents();
  },

  onFormSubmit(e) {
    e.preventDefault();
    this.debouncedOnKeyUp();
  },

  initialOnKeyUp() {
    const q = this.getQuery();
    this.ui.reset.toggleClass('hidden', q.length === 0);
    this.debouncedOnKeyUp();
  },

  onKeyUp() {
    const q = this.getQuery();
    if (q === this._bufferedValue) {
      return;
    }
    this._bufferedValue = this.getQuery();
    if (this.searchRequest != null) {
      this.searchRequest.abort();
    }
    this.searchRequest = this.search(q);
  },

  getQuery() {
    return this.$('#groups-search-query').val();
  },

  search(q) {
    return this.collection.fetch({ reset: true, data: { q } });
  },

  onResetClick(e) {
    e.preventDefault();
    e.currentTarget.blur();
    this.$('#groups-search-query')
      .val('')
      .focus();
    this.onKeyUp();
  }
});

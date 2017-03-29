/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Template from './templates/update-center-search.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'change [name="update-center-filter"]': 'onFilterChange',

    'submit #update-center-search-form': 'onFormSubmit',
    'search #update-center-search-query': 'onKeyUp',
    'keyup #update-center-search-query': 'onKeyUp',
    'change #update-center-search-query': 'onKeyUp'
  },

  collectionEvents: {
    filter: 'onFilter'
  },

  initialize() {
    this._bufferedValue = null;
    this.search = debounce(this.search, 50);
    this.listenTo(this.options.state, 'change', this.render);
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFilterChange() {
    const value = this.$('[name="update-center-filter"]:checked').val();
    this.filter(value);
  },

  filter(value) {
    this.options.router.navigate(value, { trigger: true });
  },

  onFormSubmit(e) {
    e.preventDefault();
    this.debouncedOnKeyUp();
  },

  onKeyUp() {
    const q = this.getQuery();
    if (q === this._bufferedValue) {
      return;
    }
    this._bufferedValue = this.getQuery();
    this.search(q);
  },

  getQuery() {
    return this.$('#update-center-search-query').val();
  },

  search(q) {
    this.collection.search(q);
  },

  focusSearch() {
    const that = this;
    setTimeout(
      () => {
        that.$('#update-center-search-query').focus();
      },
      0
    );
  },

  onFilter(model) {
    const q = model.get('category');
    this.$('#update-center-search-query').val(q);
    this.search(q);
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      state: this.options.state.toJSON()
    };
  }
});

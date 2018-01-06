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
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-query-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  events(...args) {
    return {
      ...BaseFacet.prototype.events.apply(this, args),
      'submit form': 'onFormSubmit',
      'keyup input': 'onKeyUp',
      'search input': 'onSearch',
      'click .js-reset': 'onResetClick'
    };
  },

  onRender() {
    this.$el.attr('data-property', this.model.get('property'));
    const query = this.options.app.state.get('query');
    const value = query.q;
    if (value != null) {
      this.$('input').val(value);
      this.$('.js-hint').toggleClass('hidden', value.length !== 1);
      this.$('.js-reset').toggleClass('hidden', value.length === 0);
    }
  },

  onFormSubmit(e) {
    e.preventDefault();
    this.applyFacet();
  },

  onKeyUp() {
    const q = this.$('input').val();
    this.$('.js-hint').toggleClass('hidden', q.length !== 1);
    this.$('.js-reset').toggleClass('hidden', q.length === 0);
  },

  onSearch() {
    const q = this.$('input').val();
    if (q.length !== 1) {
      this.applyFacet();
    }
  },

  onResetClick(e) {
    e.preventDefault();
    this.$('input')
      .val('')
      .focus();
  },

  applyFacet() {
    const obj = {};
    const property = this.model.get('property');
    const value = this.$('input').val();
    if (this.buffer !== value) {
      this.buffer = value;
      obj[property] = value;
      this.options.app.state.updateFilter(obj, { force: true });
    }
  }
});

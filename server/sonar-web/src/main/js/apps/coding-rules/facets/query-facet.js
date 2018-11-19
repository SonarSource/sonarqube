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
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-query-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  events() {
    return {
      ...BaseFacet.prototype.events.apply(this, arguments),
      'submit form': 'onFormSubmit',
      'search input': 'onInputSearch'
    };
  },

  onRender() {
    this.$el.attr('data-property', this.model.get('property'));
    const query = this.options.app.state.get('query');
    const value = query.q;
    if (value != null) {
      this.$('input').val(value);
    }
  },

  onFormSubmit(e) {
    e.preventDefault();
    this.applyFacet();
  },

  onInputSearch() {
    this.applyFacet();
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

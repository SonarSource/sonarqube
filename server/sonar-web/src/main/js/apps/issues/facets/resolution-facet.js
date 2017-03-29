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
import $ from 'jquery';
import { sortBy } from 'lodash';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-resolution-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    const value = this.options.app.state.get('query').resolved;
    if (value != null && (!value || value === 'false')) {
      this.$('.js-facet').filter('[data-unresolved]').addClass('active');
    }
  },

  toggleFacet(e) {
    const unresolved = $(e.currentTarget).is('[data-unresolved]');
    $(e.currentTarget).toggleClass('active');
    if (unresolved) {
      const checked = $(e.currentTarget).is('.active');
      const value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        resolved: value,
        resolutions: null
      });
    } else {
      return this.options.app.state.updateFilter({
        resolved: null,
        resolutions: this.getValue()
      });
    }
  },

  disable() {
    return this.options.app.state.updateFilter({
      resolved: null,
      resolutions: null
    });
  },

  sortValues(values) {
    const order = ['', 'FIXED', 'FALSE-POSITIVE', 'WONTFIX', 'REMOVED'];
    return sortBy(values, v => order.indexOf(v.val));
  }
});

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
import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-resolution-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').resolved;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unresolved]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unresolved = $(e.currentTarget).is('[data-unresolved]');
    $(e.currentTarget).toggleClass('active');
    if (unresolved) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
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

  disable: function () {
    return this.options.app.state.updateFilter({
      resolved: null,
      resolutions: null
    });
  },

  sortValues: function (values) {
    var order = ['', 'FIXED', 'FALSE-POSITIVE', 'WONTFIX', 'REMOVED'];
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});



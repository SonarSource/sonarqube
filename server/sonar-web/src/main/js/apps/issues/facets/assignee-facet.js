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
import CustomValuesFacet from './custom-values-facet';
import Template from '../templates/facets/issues-assignee-facet.hbs';

export default CustomValuesFacet.extend({
  template: Template,

  getUrl () {
    return window.baseUrl + '/api/users/search';
  },

  prepareAjaxSearch () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data (term, page) {
        return { q: term, p: page };
      },
      results: window.usersToSelect2
    };
  },

  onRender () {
    CustomValuesFacet.prototype.onRender.apply(this, arguments);
    const value = this.options.app.state.get('query').assigned;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unassigned]').addClass('active');
    }
  },

  toggleFacet (e) {
    const unassigned = $(e.currentTarget).is('[data-unassigned]');
    $(e.currentTarget).toggleClass('active');
    if (unassigned) {
      const checked = $(e.currentTarget).is('.active');
      const value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        assigned: value,
        assignees: null,
        assigned_to_me: null
      });
    } else {
      return this.options.app.state.updateFilter({
        assigned: null,
        assignees: this.getValue(),
        assigned_to_me: null
      });
    }
  },

  getValuesWithLabels () {
    const values = this.model.getValues();
    const users = this.options.app.facets.users;
    values.forEach(function (v) {
      const login = v.val;
      let name = '';
      if (login) {
        const user = _.findWhere(users, { login });
        if (user != null) {
          name = user.name;
        }
      }
      v.label = name;
    });
    return values;
  },

  disable () {
    return this.options.app.state.updateFilter({
      assigned: null,
      assignees: null
    });
  },

  addCustomValue () {
    const property = this.model.get('property');
    const customValue = this.$('.js-custom-value').select2('val');
    let value = this.getValue();
    if (value.length > 0) {
      value += ',';
    }
    value += customValue;
    const obj = {};
    obj[property] = value;
    obj.assigned = null;
    return this.options.app.state.updateFilter(obj);
  },

  sortValues (values) {
    return _.sortBy(values, function (v) {
      return v.val === '' ? -999999 : -v.count;
    });
  },

  serializeData () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


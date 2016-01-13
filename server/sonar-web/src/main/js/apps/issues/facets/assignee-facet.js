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

  getUrl: function () {
    return baseUrl + '/api/users/search';
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term, page) {
        return { q: term, p: page };
      },
      results: window.usersToSelect2
    };
  },

  onRender: function () {
    CustomValuesFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').assigned;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unassigned]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unassigned = $(e.currentTarget).is('[data-unassigned]');
    $(e.currentTarget).toggleClass('active');
    if (unassigned) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        assigned: value,
        assignees: null
      });
    } else {
      return this.options.app.state.updateFilter({
        assigned: null,
        assignees: this.getValue()
      });
    }
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        users = this.options.app.facets.users;
    values.forEach(function (v) {
      var login = v.val,
          name = '';
      if (login) {
        var user = _.findWhere(users, { login: login });
        if (user != null) {
          name = user.name;
        }
      }
      v.label = name;
    });
    return values;
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      assigned: null,
      assignees: null
    });
  },

  addCustomValue: function () {
    var property = this.model.get('property'),
        customValue = this.$('.js-custom-value').select2('val'),
        value = this.getValue();
    if (value.length > 0) {
      value += ',';
    }
    value += customValue;
    var obj = {};
    obj[property] = value;
    obj.assigned = null;
    return this.options.app.state.updateFilter(obj);
  },

  sortValues: function (values) {
    return _.sortBy(values, function (v) {
      return v.val === '' ? -999999 : -v.count;
    });
  },

  getNumberOfMyIssues: function () {
    return this.options.app.state.get('myIssues');
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      myIssues: this.getNumberOfMyIssues(),
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});



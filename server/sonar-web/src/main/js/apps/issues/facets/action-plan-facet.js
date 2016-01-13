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
import Template from '../templates/facets/issues-action-plan-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').planned;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unplanned]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unplanned = $(e.currentTarget).is('[data-unplanned]');
    $(e.currentTarget).toggleClass('active');
    if (unplanned) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        planned: value,
        actionPlans: null
      });
    } else {
      return this.options.app.state.updateFilter({
        planned: null,
        actionPlans: this.getValue()
      });
    }
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        actionPlans = this.options.app.facets.actionPlans;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var actionPlan = _.findWhere(actionPlans, { key: key });
        if (actionPlan != null) {
          label = actionPlan.name;
        }
      }
      v.label = label;
    });
    return values;
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      planned: null,
      actionPlans: null
    });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValuesWithLabels()
    });
  }
});



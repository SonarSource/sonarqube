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
import Template from '../templates/facets/coding-rules-characteristic-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').has_debt_characteristic;
    if (value != null && ('' + value === 'false')) {
      this.$('.js-facet').filter('[data-empty-characteristic]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var noneCharacteristic = $(e.currentTarget).is('[data-empty-characteristic]'),
        property = this.model.get('property'),
        obj = {};
    $(e.currentTarget).toggleClass('active');
    if (noneCharacteristic) {
      var checked = $(e.currentTarget).is('.active');
      obj.has_debt_characteristic = checked ? 'false' : null;
      obj[property] = null;
    } else {
      obj.has_debt_characteristic = null;
      obj[property] = this.getValue();
    }
    this.options.app.state.updateFilter(obj);
  },

  disable: function () {
    var property = this.model.get('property'),
        obj = {};
    obj.has_debt_characteristic = null;
    obj[property] = null;
    this.options.app.state.updateFilter(obj);
  },

  getValues: function () {
    var values = this.model.getValues(),
        characteristics = this.options.app.characteristics;
    return values.map(function (value) {
      var ch = _.findWhere(characteristics, { key: value.val });
      if (ch != null) {
        _.extend(value, ch, { label: ch.name });
      }
      return value;
    });
  },

  sortValues: function (values) {
    return _.sortBy(values, 'index');
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValues())
    });
  }
});

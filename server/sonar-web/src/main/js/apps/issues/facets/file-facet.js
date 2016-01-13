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
import Template from '../templates/facets/issues-file-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var maxValueWidth = _.max(this.$('.facet-stat').map(function () {
      return $(this).outerWidth();
    }).get());
    return this.$('.facet-name').css('padding-right', maxValueWidth);
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        source = this.options.app.facets.components;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var item = _.findWhere(source, { uuid: key });
        if (item != null) {
          label = item.longName;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});



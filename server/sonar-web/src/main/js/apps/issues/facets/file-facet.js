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
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-file-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    const widths = this.$('.facet-stat')
      .map(function() {
        return $(this).outerWidth();
      })
      .get();
    const maxValueWidth = Math.max(...widths);
    return this.$('.facet-name').css('padding-right', maxValueWidth);
  },

  getValuesWithLabels() {
    const values = this.model.getValues();
    const source = this.options.app.facets.components;
    values.forEach(v => {
      const key = v.val;
      let label = null;
      if (key) {
        const item = source.find(file => file.uuid === key);
        if (item != null) {
          label = item.longName;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData() {
    return {
      ...BaseFacet.prototype.serializeData.apply(this, arguments),
      values: this.sortValues(this.getValuesWithLabels())
    };
  }
});

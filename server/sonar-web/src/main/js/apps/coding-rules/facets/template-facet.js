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
import $ from 'jquery';
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-template-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    const value = this.options.app.state.get('query').is_template;
    if (value != null) {
      this.$('.js-facet')
        .filter(`[data-value="${value}"]`)
        .addClass('active');
    }
  },

  toggleFacet(e) {
    $(e.currentTarget).toggleClass('active');
    const property = this.model.get('property');
    const obj = {};
    if ($(e.currentTarget).hasClass('active')) {
      obj[property] = '' + $(e.currentTarget).data('value');
    } else {
      obj[property] = null;
    }
    this.options.app.state.updateFilter(obj);
  }
});

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
import Marionette from 'backbone.marionette';

export default Marionette.ItemView.extend({
  className: 'search-navigator-facet-box',
  forbiddenClassName: 'search-navigator-facet-box-forbidden',

  modelEvents() {
    return {
      change: 'render'
    };
  },

  events() {
    return {
      'click .js-facet-toggle': 'toggle',
      'click .js-facet': 'toggleFacet'
    };
  },

  onRender() {
    this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
    this.$el.attr('data-property', this.model.get('property'));
    const that = this;
    const property = this.model.get('property');
    const value = this.options.app.state.get('query')[property];
    if (typeof value === 'string') {
      value.split(',').forEach(s => {
        const facet = that.$('.js-facet').filter(`[data-value="${s}"]`);
        if (facet.length > 0) {
          facet.addClass('active');
        }
      });
    }
  },

  toggle() {
    if (!this.isForbidden()) {
      this.options.app.controller.toggleFacet(this.model.id);
    }
  },

  getValue() {
    return this.$('.js-facet.active')
      .map(function() {
        return $(this).data('value');
      })
      .get()
      .join();
  },

  toggleFacet(e) {
    $(e.currentTarget).toggleClass('active');
    const property = this.model.get('property');
    const obj = {};
    obj[property] = this.getValue();
    this.options.app.state.updateFilter(obj);
  },

  disable() {
    const property = this.model.get('property');
    const obj = {};
    obj[property] = null;
    this.options.app.state.updateFilter(obj);
  },

  forbid() {
    this.options.app.controller.disableFacet(this.model.id);
    this.$el.addClass(this.forbiddenClassName);
  },

  allow() {
    this.$el.removeClass(this.forbiddenClassName);
  },

  isForbidden() {
    return this.$el.hasClass(this.forbiddenClassName);
  },

  sortValues(values) {
    return values.slice().sort((left, right) => {
      if (left.count !== right.count) {
        return right.count - left.count;
      }
      if (left.val !== right.val) {
        if (left.val > right.val) {
          return 1;
        }
        if (left.val < right.val) {
          return -1;
        }
      }
      return 0;
    });
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      values: this.sortValues(this.model.getValues())
    };
  }
});

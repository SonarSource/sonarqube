/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define(function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'search-navigator-facet-box',
    forbiddenClassName: 'search-navigator-facet-box-forbidden',

    modelEvents: function () {
      return {
        'change': 'render'
      };
    },

    events: function () {
      return {
        'click .js-facet-toggle': 'toggle',
        'click .js-facet': 'toggleFacet'
      };
    },

    onRender: function () {
      this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
      this.$el.attr('data-property', this.model.get('property'));
      var that = this,
          property = this.model.get('property'),
          value = this.options.app.state.get('query')[property];
      if (typeof value === 'string') {
        value.split(',').forEach(function (s) {
          var facet = that.$('.js-facet').filter('[data-value="' + s + '"]');
          if (facet.length > 0) {
            facet.addClass('active');
          }
        });
      }
    },

    toggle: function () {
      if (!this.isForbidden()) {
        this.options.app.controller.toggleFacet(this.model.id);
      }
    },

    getValue: function () {
      return this.$('.js-facet.active').map(function () {
        return $(this).data('value');
      }).get().join();
    },

    toggleFacet: function (e) {
      $(e.currentTarget).toggleClass('active');
      var property = this.model.get('property'),
          obj = {};
      obj[property] = this.getValue();
      this.options.app.state.updateFilter(obj);
    },

    disable: function () {
      var property = this.model.get('property'),
          obj = {};
      obj[property] = null;
      this.options.app.state.updateFilter(obj);
    },

    forbid: function () {
      this.options.app.controller.disableFacet(this.model.id);
      this.$el.addClass(this.forbiddenClassName);
    },

    allow: function () {
      this.$el.removeClass(this.forbiddenClassName);
    },

    isForbidden: function () {
      return this.$el.hasClass(this.forbiddenClassName);
    },

    sortValues: function (values) {
      return values.slice().sort(function (left, right) {
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

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.model.getValues())
      });
    }
  });

});

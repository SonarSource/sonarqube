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

  return Backbone.View.extend({

    initialize: function() {
      this.model.view = this;
    },


    render: function() {
      this.$el.hide();
    },


    renderBase: function() {},
    renderInput: function() {},
    focus: function() {},
    showDetails: function() {},
    registerShowedDetails: function() {},
    hideDetails: function() {},
    isActive: function() {},
    renderValue: function() {},
    isDefaultValue: function() {},


    restoreFromQuery: function(q) {
      var param = _.findWhere(q, { key: this.model.get('property') });
      if (param && param.value) {
        this.restore(param.value);
      } else {
        this.clear();
      }
    },


    restore: function(value) {
      this.model.set({ value: value });
    },


    clear: function() {
      this.model.unset('value');
    },


    disable: function(e) {
      e.stopPropagation();
      this.hideDetails();
      this.options.filterBarView.hideDetails();
      this.model.set({
        enabled: false,
        value: null
      });
    },


    formatValue: function() {
      var q = {};
      if (this.model.has('property') && this.model.has('value') && this.model.get('value')) {
        q[this.model.get('property')] = this.model.get('value');
      }
      return q;
    }
  });

});

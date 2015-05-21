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
define([
    './base-facet',
    '../templates'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-query-facet'],

    events: function () {
      return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
        'submit form': 'onFormSubmit'
      });
    },

    onRender: function () {
      this.$el.attr('data-property', this.model.get('property'));
      var query = this.options.app.state.get('query'),
          value = query.q;
      if (value != null) {
        this.$('input').val(value);
      }
    },

    onFormSubmit: function (e) {
      e.preventDefault();
      this.applyFacet();
    },

    applyFacet: function() {
      var obj = {},
          property = this.model.get('property');
      obj[property] = this.$('input').val();
      this.options.app.state.updateFilter(obj, { force: true });
    }
  });

});

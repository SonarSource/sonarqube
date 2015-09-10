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
  './templates'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'a',
    className: 'list-group-item',
    template: Templates['api-documentation-web-service'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click': 'onClick'
    },

    initialize: function () {
      this.listenTo(this.options.state, 'change:internal', this.toggleInternal);
    },

    onRender: function () {
      this.$el.attr('data-path', this.model.get('path'));
      this.$el.toggleClass('active', this.options.highlighted);
      this.toggleInternal();
    },

    onClick: function (e) {
      e.preventDefault();
      this.model.trigger('select', this.model);
    },

    toggleInternal: function () {
      var showInternal = this.options.state.get('internal'),
          hideMe = this.model.get('internal') && !showInternal;
      this.$el.toggleClass('hidden', hideMe);
    }
  });

});

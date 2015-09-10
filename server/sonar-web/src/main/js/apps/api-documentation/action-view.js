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

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'panel panel-vertical',
    template: Templates['api-documentation-action'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-show-response-example': 'onShowResponseExampleClick'
    },

    onRender: function () {
      this.$el.attr('data-web-service', this.model.get('path'));
      this.$el.attr('data-action', this.model.get('key'));
    },

    onShowResponseExampleClick: function (e) {
      e.preventDefault();
      this.fetchResponse();
    },

    fetchResponse: function () {
      var that = this,
          url = baseUrl + '/api/webservices/response_example',
          options = { controller: this.model.get('path'), action: this.model.get('key') };
      return $.get(url, options).done(function (r) {
        that.model.set({ responseExample: r.example });
      });
    }
  });

});

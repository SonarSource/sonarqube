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
import Marionette from 'backbone.marionette';
import Template from './templates/api-documentation-action.hbs';

export default Marionette.ItemView.extend({
  className: 'panel panel-vertical',
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click .js-show-response-example': 'onShowResponseExampleClick',
    'click .js-hide-response-example': 'onHideResponseExampleClick'
  },

  initialize: function () {
    this.listenTo(this.options.state, 'change', this.toggleHidden);
  },

  onRender: function () {
    this.$el.attr('data-web-service', this.model.get('path'));
    this.$el.attr('data-action', this.model.get('key'));
    this.toggleHidden();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onShowResponseExampleClick: function (e) {
    e.preventDefault();
    this.fetchResponse();
  },

  onHideResponseExampleClick: function (e) {
    e.preventDefault();
    this.model.unset('responseExample');
  },

  fetchResponse: function () {
    var that = this,
        url = baseUrl + '/api/webservices/response_example',
        options = { controller: this.model.get('path'), action: this.model.get('key') };
    return $.get(url, options).done(function (r) {
      that.model.set({ responseExample: r.example });
    });
  },

  toggleHidden: function () {
    var test = this.model.get('path') + '/' + this.model.get('key');
    this.$el.toggleClass('hidden', !this.options.state.match(test, this.model.get('internal')));
  }
});

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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Template from './templates/maintenance-main.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #start-migration': 'startMigration'
  },

  initialize: function () {
    var that = this;
    this.requestOptions = {
      type: 'GET',
      url: '/api/system/' + (this.options.setup ? 'db_migration_status' : 'status')
    };
    this.pollingInternal = setInterval(function () {
      that.refresh();
    }, 5000);
  },

  refresh: function () {
    var that = this;
    return Backbone.ajax(this.requestOptions).done(function (r) {
      that.model.set(r);
      that.render();
      if (that.model.get('status') === 'UP' || that.model.get('state') === 'NO_MIGRATION') {
        that.stopPolling();
      }
      if (that.model.get('state') === 'MIGRATION_SUCCEEDED') {
        that.goHome();
      }
    });
  },

  stopPolling: function () {
    clearInterval(this.pollingInternal);
  },

  startMigration: function () {
    var that = this;
    Backbone.ajax({
      url: '/api/system/migrate_db',
      type: 'POST'
    }).done(function (r) {
      that.model.set(r);
      that.render();
    });
  },

  onRender: function () {
    $('.page-simple').toggleClass('panel-warning', this.model.get('state') === 'MIGRATION_REQUIRED');
  },

  goHome: function () {
    setInterval(function () {
      window.location = '/';
    }, 2500);
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      setup: this.options.setup
    });
  }
});



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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Template from './templates/maintenance-main.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #start-migration': 'startMigration'
  },

  initialize () {
    const that = this;
    this.requestOptions = {
      type: 'GET',
      url: window.baseUrl + '/api/system/' + (this.options.setup ? 'db_migration_status' : 'status')
    };
    this.pollingInternal = setInterval(() => {
      that.refresh();
    }, 5000);
  },

  refresh () {
    const that = this;
    return Backbone.ajax(this.requestOptions).done(r => {
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

  stopPolling () {
    clearInterval(this.pollingInternal);
  },

  startMigration () {
    const that = this;
    Backbone.ajax({
      url: window.baseUrl + '/api/system/migrate_db',
      type: 'POST'
    }).done(r => {
      that.model.set(r);
      that.render();
    });
  },

  onRender () {
    $('.page-simple').toggleClass('panel-warning', this.model.get('state') === 'MIGRATION_REQUIRED');
  },

  goHome () {
    setInterval(() => {
      window.location = window.baseUrl + '/';
    }, 2500);
  },

  serializeData () {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      setup: this.options.setup
    };
  }
});


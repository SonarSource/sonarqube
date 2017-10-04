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

  initialize() {
    this.requestOptions = {
      type: 'GET',
      url: window.baseUrl + '/api/system/' + (this.options.setup ? 'db_migration_status' : 'status')
    };
    this.pollingInternal = setInterval(() => {
      this.refresh();
    }, 5000);
    this.wasStarting = false;
  },

  refresh() {
    return Backbone.ajax(this.requestOptions).done(r => {
      if (r.status === 'STARTING') {
        this.wasStarting = true;
      }
      this.model.set(r);
      this.render();
      if (this.model.get('status') === 'UP' || this.model.get('state') === 'NO_MIGRATION') {
        this.stopPolling();
      }
      if (this.model.get('status') === 'UP' && this.wasStarting) {
        this.loadPreviousPage();
      }
      if (this.model.get('state') === 'MIGRATION_SUCCEEDED') {
        this.loadPreviousPage();
      }
    });
  },

  stopPolling() {
    clearInterval(this.pollingInternal);
  },

  startMigration() {
    Backbone.ajax({
      url: window.baseUrl + '/api/system/migrate_db',
      type: 'POST'
    }).done(r => {
      this.model.set(r);
      this.render();
    });
  },

  onRender() {
    $('.page-simple').toggleClass(
      'panel-warning',
      this.model.get('state') === 'MIGRATION_REQUIRED'
    );
  },

  loadPreviousPage() {
    setInterval(() => {
      window.location = this.options.returnTo || window.baseUrl;
    }, 2500);
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      setup: this.options.setup
    };
  }
});

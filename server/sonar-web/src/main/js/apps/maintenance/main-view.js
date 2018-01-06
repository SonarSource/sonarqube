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
import Marionette from 'backbone.marionette';
import Template from './templates/maintenance-main.hbs';
import { getSystemStatus, getMigrationStatus, migrateDatabase } from '../../api/system';
import { getBaseUrl } from '../../helpers/urls';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #start-migration': 'startMigration'
  },

  initialize() {
    this.pollingInternal = setInterval(() => {
      this.refresh();
    }, 5000);
    this.wasStarting = false;
  },

  getStatus() {
    return this.options.setup ? getMigrationStatus() : getSystemStatus();
  },

  refresh() {
    return this.getStatus().then(
      r => {
        if (r.status === 'STARTING') {
          this.wasStarting = true;
        }
        // unset `status` in case if was `OFFLINE` previously
        this.model.set({ status: undefined, ...r });
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
      },
      () => {
        this.model.set({ status: 'OFFLINE' });
        this.render();
      }
    );
  },

  stopPolling() {
    clearInterval(this.pollingInternal);
  },

  startMigration() {
    migrateDatabase().then(
      r => {
        this.model.set(r);
        this.render();
      },
      () => {}
    );
  },

  onRender() {
    document
      .querySelector('.page-simple')
      .classList.toggle('panel-warning', this.model.get('state') === 'MIGRATION_REQUIRED');
  },

  loadPreviousPage() {
    setInterval(() => {
      window.location = this.options.returnTo || getBaseUrl();
    }, 2500);
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      setup: this.options.setup
    };
  }
});

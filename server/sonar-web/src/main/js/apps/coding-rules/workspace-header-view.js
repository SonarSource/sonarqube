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
import $ from 'jquery';
import WorkspaceHeaderView from '../../components/navigator/workspace-header-view';
import BulkChangePopup from './bulk-change-popup-view';
import Template from './templates/coding-rules-workspace-header.hbs';

export default WorkspaceHeaderView.extend({
  template: Template,

  events() {
    return {
      ...WorkspaceHeaderView.prototype.events.apply(this, arguments),
      'click .js-back': 'onBackClick',
      'click .js-bulk-change': 'onBulkChangeClick',
      'click .js-reload': 'reload',
      'click .js-new-search': 'newSearch'
    };
  },

  onBackClick() {
    this.options.app.controller.hideDetails();
  },

  onBulkChangeClick(e) {
    e.stopPropagation();
    $('body').click();
    new BulkChangePopup({
      app: this.options.app,
      triggerEl: $(e.currentTarget),
      bottomRight: true
    }).render();
  },

  reload(event) {
    event.preventDefault();
    this.options.app.controller.fetchList(true);
  },

  newSearch() {
    this.options.app.controller.newSearch();
  },

  serializeData() {
    // show "Bulk Change" button only if user has at least one QP which he administates
    const canBulkChange = this.options.app.qualityProfiles.some(
      profile => profile.actions && profile.actions.edit
    );

    return {
      ...WorkspaceHeaderView.prototype.serializeData.apply(this, arguments),
      canBulkChange
    };
  }
});

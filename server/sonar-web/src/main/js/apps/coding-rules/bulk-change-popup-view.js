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
import PopupView from '../../components/common/popup';
import BulkChangeModalView from './bulk-change-modal-view';
import Template from './templates/coding-rules-bulk-change-popup.hbs';

export default PopupView.extend({
  template: Template,

  events: {
    'click .js-bulk-change': 'doAction'
  },

  doAction(e) {
    const action = $(e.currentTarget).data('action');
    const param = $(e.currentTarget).data('param');
    new BulkChangeModalView({
      app: this.options.app,
      action,
      param
    }).render();
  },

  serializeData() {
    const query = this.options.app.state.get('query');
    const profileKey = query.qprofile;
    const profile = this.options.app.qualityProfiles.find(p => p.key === profileKey);
    const activation = '' + query.activation;

    return {
      qualityProfile: profileKey,
      qualityProfileName: profile != null ? profile.name : null,
      allowActivateOnProfile: profile != null && activation === 'false' && !profile.isBuiltIn,
      allowDeactivateOnProfile: profileKey != null && activation === 'true'
    };
  }
});

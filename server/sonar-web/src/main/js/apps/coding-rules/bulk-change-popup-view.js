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
  'components/common/popup',
  './bulk-change-modal-view',
  './templates'
], function (PopupView, BulkChangeModalView) {

  var $ = jQuery;

  return PopupView.extend({
    template: Templates['coding-rules-bulk-change-popup'],

    events: {
      'click .js-bulk-change': 'doAction'
    },

    doAction: function (e) {
      var action = $(e.currentTarget).data('action'),
          param = $(e.currentTarget).data('param');
      new BulkChangeModalView({
        app: this.options.app,
        action: action,
        param: param
      }).render();
    },

    serializeData: function () {
      var query = this.options.app.state.get('query'),
          profileKey = query.qprofile,
          profile = _.findWhere(this.options.app.qualityProfiles, { key: profileKey }),
          activation = '' + query.activation;

      return {
        qualityProfile: profileKey,
        qualityProfileName: profile != null ? profile.name : null,
        allowActivateOnProfile: profileKey != null && activation === 'false',
        allowDeactivateOnProfile: profileKey != null && activation === 'true'
      };
    }
  });

});

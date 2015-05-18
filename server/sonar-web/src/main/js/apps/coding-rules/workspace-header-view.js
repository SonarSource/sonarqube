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
  'components/navigator/workspace-header-view',
  './bulk-change-popup-view',
  './rule/manual-rule-creation-view',
  './templates'
], function (WorkspaceHeaderView, BulkChangePopup, ManualRuleCreationView) {

  var $ = jQuery;

  return WorkspaceHeaderView.extend({
    template: Templates['coding-rules-workspace-header'],

    events: function () {
      return _.extend(WorkspaceHeaderView.prototype.events.apply(this, arguments), {
        'click .js-back': 'onBackClick',
        'click .js-bulk-change': 'onBulkChangeClick',
        'click .js-create-manual-rule': 'createManualRule',
        'click .js-reload': 'reload',
        'click .js-new-search': 'newSearch'
      });
    },

    onBackClick: function () {
      this.options.app.controller.hideDetails();
    },

    onBulkChangeClick: function (e) {
      e.stopPropagation();
      $('body').click();
      new BulkChangePopup({
        app: this.options.app,
        triggerEl: $(e.currentTarget),
        bottomRight: true
      }).render();
    },

    createManualRule: function() {
      new ManualRuleCreationView({
        app: this.options.app
      }).render();
    },

    reload: function () {
      this.options.app.controller.fetchList(true);
    },

    newSearch: function () {
      this.options.app.controller.newSearch();
    },

    serializeData: function () {
      return _.extend(WorkspaceHeaderView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});

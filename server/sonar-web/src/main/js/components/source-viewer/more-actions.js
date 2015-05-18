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
  'components/workspace/main',
  './templates'
], function (Workspace) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'source-viewer-header-more-actions',
    template: Templates['source-viewer-more-actions'],

    events: {
      'click .js-measures': 'showMeasures',
      'click .js-new-window': 'openNewWindow',
      'click .js-workspace': 'openInWorkspace',
      'click .js-raw-source': 'showRawSource'
    },

    onRender: function () {
      var that = this;
      $('body').on('click.component-viewer-more-actions', function () {
        $('body').off('click.component-viewer-more-actions');
        that.close();
      });
    },

    showMeasures: function () {
      this.options.parent.showMeasures();
    },

    openNewWindow: function () {
      this.options.parent.getPermalink();
    },

    openInWorkspace: function () {
      var uuid = this.options.parent.model.id;
      if (Workspace == null) {
        Workspace = require('components/workspace/main');
      }
      Workspace.openComponent({ uuid: uuid });
    },

    showRawSource: function () {
      this.options.parent.showRawSources();
    },

    serializeData: function () {
      var options = this.options.parent.options.viewer.options;
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        options: options
      });
    }
  });

});

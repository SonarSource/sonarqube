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
import Marionette from 'backbone.marionette';
import Workspace from '../workspace/main';
import Template from './templates/source-viewer-more-actions.hbs';

export default Marionette.ItemView.extend({
  className: 'source-viewer-header-more-actions',
  template: Template,

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
      that.destroy();
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



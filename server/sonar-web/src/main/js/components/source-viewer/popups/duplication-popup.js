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
import Popup from '../../common/popup';
import Workspace from '../../workspace/main';
import Template from '../templates/source-viewer-duplication-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-uuid]': 'goToFile'
  },

  goToFile: function (e) {
    e.stopPropagation();
    var uuid = $(e.currentTarget).data('uuid'),
        line = $(e.currentTarget).data('line');
    Workspace.openComponent({ uuid: uuid, line: line });
  },

  serializeData: function () {
    var that = this,
        files = this.model.get('duplicationFiles'),
        groupedBlocks = _.groupBy(this.collection.toJSON(), '_ref'),
        duplications = _.map(groupedBlocks, function (blocks, fileRef) {
          return {
            blocks: blocks,
            file: files[fileRef]
          };
        });
    duplications = _.sortBy(duplications, function (d) {
      var a = d.file.projectName !== that.model.get('projectName'),
          b = d.file.subProjectName !== that.model.get('subProjectName'),
          c = d.file.key !== that.model.get('key');
      return '' + a + b + c;
    });
    return {
      component: this.model.toJSON(),
      duplications: duplications,
      inRemovedComponent: this.options.inRemovedComponent
    };
  }
});


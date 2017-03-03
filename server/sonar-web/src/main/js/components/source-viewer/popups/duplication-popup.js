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
import groupBy from 'lodash/groupBy';
import sortBy from 'lodash/sortBy';
import Popup from '../../common/popup';
import Workspace from '../../workspace/main';
import Template from '../templates/source-viewer-duplication-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-uuid]': 'goToFile'
  },

  goToFile (e) {
    e.stopPropagation();
    const uuid = $(e.currentTarget).data('uuid');
    const line = $(e.currentTarget).data('line');
    Workspace.openComponent({ uuid, line });
  },

  serializeData () {
    const that = this;
    const files = this.model.get('duplicationFiles');
    const groupedBlocks = groupBy(this.collection.toJSON(), '_ref');
    let duplications = Object.keys(groupedBlocks).map(fileRef => {
      return {
        blocks: groupedBlocks[fileRef],
        file: files[fileRef]
      };
    });
    duplications = sortBy(duplications, d => {
      const a = d.file.projectName !== that.model.get('projectName');
      const b = d.file.subProjectName !== that.model.get('subProjectName');
      const c = d.file.key !== that.model.get('key');
      return '' + a + b + c;
    });
    return {
      duplications,
      component: this.model.toJSON(),
      inRemovedComponent: this.options.inRemovedComponent
    };
  }
});


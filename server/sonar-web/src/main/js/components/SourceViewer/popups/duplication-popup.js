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
import { groupBy, sortBy } from 'lodash';
import Template from './templates/source-viewer-duplication-popup.hbs';
import Popup from '../../common/popup';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-key]': 'goToFile'
  },

  goToFile(e) {
    e.stopPropagation();
    const key = $(e.currentTarget).data('key');
    const line = $(e.currentTarget).data('line');
    const Workspace = require('../../workspace/main').default;
    Workspace.openComponent({ key, line, branch: this.options.branch });
  },

  serializeData() {
    const that = this;
    const groupedBlocks = groupBy(this.options.blocks, '_ref');
    let duplications = Object.keys(groupedBlocks).map(fileRef => {
      return {
        blocks: groupedBlocks[fileRef],
        file: this.options.files[fileRef]
      };
    });
    duplications = sortBy(duplications, d => {
      const a = d.file.projectName !== that.options.component.projectName;
      const b = d.file.subProjectName !== that.options.component.subProjectName;
      const c = d.file.key !== that.options.component.key;
      return '' + a + b + c;
    });
    return {
      duplications,
      component: this.options.component,
      inRemovedComponent: this.options.inRemovedComponent
    };
  }
});

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
import Popup from '../../common/popup';
import Workspace from '../../workspace/main';
import Template from '../templates/source-viewer-coverage-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-id]': 'goToFile'
  },

  onRender () {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
  },

  goToFile (e) {
    e.stopPropagation();
    const id = $(e.currentTarget).data('id');
    Workspace.openComponent({ uuid: id });
  },

  serializeData () {
    const row = this.options.row || {};
    const tests = groupBy(this.collection.toJSON(), 'fileId');
    const testFiles = Object.keys(tests).map(fileId => {
      const testSet = tests[fileId];
      const test = testSet[0];
      return {
        file: {
          id: test.fileId,
          longName: test.fileName
        },
        tests: testSet
      };
    });
    return { testFiles, row };
  }
});


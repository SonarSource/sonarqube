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
import { groupBy } from 'lodash';
import Template from './templates/source-viewer-coverage-popup.hbs';
import Popup from '../../common/popup';

export default Popup.extend({
  template: Template,

  events: {
    'click a[data-key]': 'goToFile'
  },

  onRender() {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
  },

  goToFile(e) {
    e.stopPropagation();
    const key = $(e.currentTarget).data('key');
    const Workspace = require('../../workspace/main').default;
    Workspace.openComponent({ key, branch: this.options.branch });
  },

  serializeData() {
    const row = this.options.line || {};
    const tests = groupBy(this.options.tests, 'fileKey');
    const testFiles = Object.keys(tests).map(fileKey => {
      const testSet = tests[fileKey];
      const test = testSet[0];
      return {
        file: {
          key: test.fileKey,
          longName: test.fileName
        },
        tests: testSet
      };
    });
    return { testFiles, row };
  }
});

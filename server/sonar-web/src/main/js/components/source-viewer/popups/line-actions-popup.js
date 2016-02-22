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
import Popup from '../../common/popup';
import ManualIssueView from '../../issue/manual-issue-view';
import Template from '../templates/source-viewer-line-options-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click .js-get-permalink': 'getPermalink',
    'click .js-add-manual-issue': 'addManualIssue'
  },

  getPermalink (e) {
    e.preventDefault();
    const url = '/component/index?id=' +
        (encodeURIComponent(this.model.key())) + '&line=' + this.options.line;
    const windowParams = 'resizable=1,scrollbars=1,status=1';
    window.open(url, this.model.get('name'), windowParams);
  },

  addManualIssue (e) {
    e.preventDefault();
    const that = this;
    const line = this.options.line;
    const component = this.model.key();
    const manualIssueView = new ManualIssueView({
      line,
      component
    });
    manualIssueView.render().$el.appendTo(this.options.row.find('.source-line-code'));
    manualIssueView.on('add', function (issue) {
      that.trigger('onManualIssueAdded', issue);
    });
  }
});



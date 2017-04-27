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
import escapeHtml from 'escape-html';
import Modal from '../../components/common/modals';
import Template from './templates/global-permissions-groups.hbs';
import '../../components/SelectList';

function getSearchUrl (permission, project) {
  let url = window.baseUrl + '/api/permissions/groups?ps=100&permission=' + permission;
  if (project) {
    url = url + '&projectId=' + project;
  }
  return url;
}

function getExtra (permission, project) {
  const extra = { permission };
  if (project) {
    extra.projectId = project;
  }
  return extra;
}

export default Modal.extend({
  template: Template,

  onRender () {
    Modal.prototype.onRender.apply(this, arguments);
    new window.SelectList({
      el: this.$('#global-permissions-groups'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat(item) {
        return escapeHtml(item.name);
      },
      queryParam: 'q',
      searchUrl: getSearchUrl(this.options.permission, this.options.project),
      selectUrl: window.baseUrl + '/api/permissions/add_group',
      deselectUrl: window.baseUrl + '/api/permissions/remove_group',
      extra: getExtra(this.options.permission, this.options.project),
      selectParameter: 'groupName',
      selectParameterValue: 'name',
      parse (r) {
        this.more = false;
        return r.groups;
      }
    });
  },

  onDestroy () {
    this.options.refresh();
    Modal.prototype.onDestroy.apply(this, arguments);
  }
});


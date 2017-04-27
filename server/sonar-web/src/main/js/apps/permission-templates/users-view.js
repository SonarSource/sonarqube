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
import _ from 'underscore';
import Modal from '../../components/common/modals';
import '../../components/SelectList';
import Template from './templates/permission-templates-users.hbs';

export default Modal.extend({
  template: Template,

  onRender () {
    Modal.prototype.onRender.apply(this, arguments);
    const searchUrl = window.baseUrl + '/api/permissions/template_users?ps=100&permission=' +
        this.options.permission.key + '&templateId=' + this.options.permissionTemplate.id;
    new window.SelectList({
      searchUrl,
      el: this.$('#permission-templates-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat (item) {
        return `${escapeHtml(item.name)}<br><span class="note">${escapeHtml(item.login)}</span>`;
      },
      queryParam: 'q',
      selectUrl: window.baseUrl + '/api/permissions/add_user_to_template',
      deselectUrl: window.baseUrl + '/api/permissions/remove_user_from_template',
      extra: {
        permission: this.options.permission.key,
        templateId: this.options.permissionTemplate.id
      },
      selectParameter: 'login',
      selectParameterValue: 'login',
      parse (r) {
        this.more = false;
        return r.users;
      }
    });
  },

  onDestroy () {
    if (this.options.refresh) {
      this.options.refresh();
    }
    Modal.prototype.onDestroy.apply(this, arguments);
  },

  serializeData () {
    return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
      permissionName: this.options.permission.name,
      permissionTemplateName: this.options.permissionTemplate.name
    });
  }
});

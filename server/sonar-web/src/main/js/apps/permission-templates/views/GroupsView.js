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
import escapeHtml from 'escape-html';
import Modal from '../../../components/common/modals';
import Template from '../templates/permission-templates-groups.hbs';
import '../../../components/SelectList';

function getSearchUrl(permission, permissionTemplate) {
  return (
    `${window.baseUrl}/api/permissions/template_groups?` +
    `ps=100&permission=${permission.key}&templateId=${permissionTemplate.id}`
  );
}

export default Modal.extend({
  template: Template,

  onRender() {
    Modal.prototype.onRender.apply(this, arguments);
    new window.SelectList({
      el: this.$('#permission-templates-groups'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat(item) {
        return escapeHtml(item.name);
      },
      queryParam: 'q',
      searchUrl: getSearchUrl(this.options.permission, this.options.permissionTemplate),
      selectUrl: window.baseUrl + '/api/permissions/add_group_to_template',
      deselectUrl: window.baseUrl + '/api/permissions/remove_group_from_template',
      extra: {
        permission: this.options.permission.key,
        templateId: this.options.permissionTemplate.id
      },
      selectParameter: 'groupName',
      selectParameterValue: 'name',
      parse(r) {
        this.more = false;
        return r.groups;
      }
    });
  },

  onDestroy() {
    if (this.options.refresh) {
      this.options.refresh();
    }
    Modal.prototype.onDestroy.apply(this, arguments);
  },

  serializeData() {
    return {
      ...Modal.prototype.serializeData.apply(this, arguments),
      permissionName: this.options.permission.name,
      permissionTemplateName: this.options.permissionTemplate.name
    };
  }
});

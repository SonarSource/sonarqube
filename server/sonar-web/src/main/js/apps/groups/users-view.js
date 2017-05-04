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
import Modal from '../../components/common/modals';
import '../../components/SelectList';
import Template from './templates/groups-users.hbs';

export default Modal.extend({
  template: Template,

  initialize(options) {
    this.organization = options.organization;
  },

  onRender() {
    Modal.prototype.onRender.apply(this, arguments);

    const extra = {
      name: this.model.get('name')
    };
    if (this.organization) {
      extra.organization = this.organization.key;
    }

    new window.SelectList({
      el: this.$('#groups-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat(item) {
        return `${escapeHtml(item.name)}<br><span class="note">${escapeHtml(item.login)}</span>`;
      },
      queryParam: 'q',
      searchUrl: window.baseUrl + '/api/user_groups/users?ps=100&id=' + this.model.id,
      selectUrl: window.baseUrl + '/api/user_groups/add_user',
      deselectUrl: window.baseUrl + '/api/user_groups/remove_user',
      extra,
      selectParameter: 'login',
      selectParameterValue: 'login',
      parse(r) {
        this.more = false;
        return r.users;
      }
    });
  },

  onDestroy() {
    this.model.collection.refresh();
    Modal.prototype.onDestroy.apply(this, arguments);
  }
});

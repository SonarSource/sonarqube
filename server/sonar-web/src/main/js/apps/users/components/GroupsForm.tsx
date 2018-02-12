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
import * as React from 'react';
import * as escapeHtml from 'escape-html';
import { User } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import SelectList from '../../../components/SelectList';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: User;
}

export default class GroupsForm extends React.PureComponent<Props> {
  container?: HTMLDivElement | null;

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.handleClose();
  };

  handleClose = () => {
    this.props.onUpdateUsers();
    this.props.onClose();
  };

  renderSelectList = () => {
    const searchUrl = `${getBaseUrl()}/api/users/groups?ps=100&login=${encodeURIComponent(
      this.props.user.login
    )}`;

    new (SelectList as any)({
      el: this.container,
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat: (item: { name: string; description: string }) =>
        `${escapeHtml(item.name)}<br><span class="note">${escapeHtml(item.description)}</span>`,
      queryParam: 'q',
      searchUrl,
      selectUrl: getBaseUrl() + '/api/user_groups/add_user',
      deselectUrl: getBaseUrl() + '/api/user_groups/remove_user',
      extra: { login: this.props.user.login },
      selectParameter: 'id',
      selectParameterValue: 'id',
      parse(r: any) {
        this.more = false;
        return r.groups;
      }
    });
  };

  render() {
    const header = translate('users.update_groups');

    return (
      <Modal
        contentLabel={header}
        onAfterOpen={this.renderSelectList}
        onRequestClose={this.handleClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          <div id="user-groups" ref={node => (this.container = node)} />
        </div>

        <footer className="modal-foot">
          <a className="js-modal-close" href="#" onClick={this.handleCloseClick}>
            {translate('Done')}
          </a>
        </footer>
      </Modal>
    );
  }
}

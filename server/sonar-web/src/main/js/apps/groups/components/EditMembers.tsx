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
import { Group } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import BulletListIcon from '../../../components/icons-components/BulletListIcon';
import SelectList from '../../../components/SelectList';
import { ButtonIcon } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  group: Group;
  onEdit: () => void;
  organization: string | undefined;
}

interface State {
  modal: boolean;
}

export default class EditMembers extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleMembersClick = () => {
    this.setState({ modal: true }, () => {
      // defer rendering of the SelectList to make sure we have `ref` assigned
      setTimeout(this.renderSelectList, 0);
    });
  };

  handleModalClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
      this.props.onEdit();
    }
  };

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.handleModalClose();
  };

  renderSelectList = () => {
    if (this.container) {
      const extra = { name: this.props.group.name, organization: this.props.organization };

      /* eslint-disable no-new */
      new SelectList({
        el: this.container,
        width: '100%',
        readOnly: false,
        focusSearch: false,
        dangerouslyUnescapedHtmlFormat: (item: { login: string; name: string }) =>
          `${escapeHtml(item.name)}<br><span class="note">${escapeHtml(item.login)}</span>`,
        queryParam: 'q',
        searchUrl: getBaseUrl() + '/api/user_groups/users?ps=100&id=' + this.props.group.id,
        selectUrl: getBaseUrl() + '/api/user_groups/add_user',
        deselectUrl: getBaseUrl() + '/api/user_groups/remove_user',
        extra,
        selectParameter: 'login',
        selectParameterValue: 'login',
        parse: (r: any) => r.users
      });
      /* eslint-enable no-new */
    }
  };

  render() {
    const modalHeader = translate('users.update');

    return (
      <>
        <ButtonIcon className="button-small" onClick={this.handleMembersClick}>
          <BulletListIcon />
        </ButtonIcon>
        {this.state.modal && (
          <Modal contentLabel={modalHeader} onRequestClose={this.handleModalClose}>
            <header className="modal-head">
              <h2>{modalHeader}</h2>
            </header>

            <div className="modal-body">
              <div id="groups-users" ref={node => (this.container = node)} />
            </div>

            <footer className="modal-foot">
              <button className="button-link" onClick={this.handleCloseClick} type="reset">
                {translate('Done')}
              </button>
            </footer>
          </Modal>
        )}
      </>
    );
  }
}

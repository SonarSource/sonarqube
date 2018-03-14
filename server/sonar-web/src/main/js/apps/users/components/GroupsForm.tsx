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
import { User } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import SelectList from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { getUserGroups } from '../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: User;
}

interface State {
  error: string;
  groups: Array<{ key: string; name: string; selected: boolean }>;
}

export default class GroupsForm extends React.PureComponent<Props> {
  container?: HTMLDivElement | null;
  state: State = { error: '', groups: [] };

  componentDidMount() {
    this.handleSearch('', 'selected');
  }

  handleSearch = (query: string, selected: string) => {
    return getUserGroups(this.props.user.login, undefined, query, selected).then(
      (data: any) => {
        this.setState({
          groups: data.groups.map((group: any) => {
            return { key: group.id, name: group.name, selected: group.selected };
          })
        });
      },
      () => {}
    );
  };

  handleSelect = (key: number | string) => {
    const requestData: any = {
      id: key,
      login: this.props.user.login
    };

    return addUserToGroup(requestData).then(
      () => {
        this.setState((state: State) => {
          return {
            groups: state.groups.map((group: any) => {
              return group.key === key ? { ...group, selected: true } : group;
            })
          };
        });
      },
      () => {}
    );
  };

  handleUnselect = (key: number | string) => {
    const requestData: any = {
      id: key,
      login: this.props.user.login
    };

    return removeUserFromGroup(requestData).then(() => {
      this.setState((state: State) => {
        return {
          groups: state.groups.map((group: any) => {
            return group.key === key ? { ...group, selected: false } : group;
          })
        };
      });
    });
  };

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.handleClose();
  };

  handleClose = () => {
    this.props.onUpdateUsers();
    this.props.onClose();
  };

  render() {
    const header = translate('users.update_groups');

    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          {this.state.error !== '' && (
            <div className="alert alert-danger">
              <p>{this.state.error}</p>
            </div>
          )}
          <SelectList
            elements={this.state.groups}
            onSearch={this.handleSearch}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
          />
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

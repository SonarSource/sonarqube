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
import { find, without } from 'lodash';
import { User, Group } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import SelectList from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { getUserGroups } from '../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';
import throwGlobalError from '../../../app/utils/throwGlobalError';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: User;
}

interface State {
  error: string;
  groups: Group[];
  selectedGroups: number[];
}

export default class GroupsForm extends React.PureComponent<Props> {
  container?: HTMLDivElement | null;
  state: State = { error: '', groups: [], selectedGroups: [] };

  componentDidMount() {
    this.handleSearch('', 'selected');
  }

  handleSearch = (query: string, selected: string) => {
    return getUserGroups(this.props.user.login, undefined, query, selected).then(
      (data: any) => {
        this.setState({
          groups: data.groups,
          selectedGroups: data.groups
            .filter((group: any) => group.selected)
            .map((group: any) => group.id)
        });
      },
      () => {}
    );
  };

  handleSelect = (key: number) => {
    const requestData: any = {
      id: key,
      login: this.props.user.login
    };

    return addUserToGroup(requestData).then(
      () => {
        this.setState((state: State) => {
          return {
            selectedGroups: [...state.selectedGroups, key]
          };
        });
      },
      e => {
        throwGlobalError(e);
      }
    );
  };

  handleUnselect = (key: number) => {
    const requestData: any = {
      id: key,
      login: this.props.user.login
    };

    return removeUserFromGroup(requestData).then(
      () => {
        this.setState((state: State) => {
          return {
            selectedGroups: without(state.selectedGroups, key)
          };
        });
      },
      e => {
        throwGlobalError(e);
      }
    );
  };

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.handleClose();
  };

  handleClose = () => {
    this.props.onUpdateUsers();
    this.props.onClose();
  };

  renderElement = (id: number): React.ReactNode => {
    const group = find(this.state.groups, { id });
    return group === undefined ? id : group.name;
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
            elements={this.state.groups.map(group => group.id)}
            onSearch={this.handleSearch}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedGroups}
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

/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { getUserGroups, UserGroup } from '../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: T.User;
}

interface State {
  groups: UserGroup[];
  selectedGroups: string[];
}

export default class GroupsForm extends React.PureComponent<Props> {
  container?: HTMLDivElement | null;
  state: State = { groups: [], selectedGroups: [] };

  componentDidMount() {
    this.handleSearch('', Filter.Selected);
  }

  handleSearch = (query: string, selected: Filter) => {
    return getUserGroups(this.props.user.login, undefined, query, selected).then(data => {
      this.setState({
        groups: data.groups,
        selectedGroups: data.groups.filter(group => group.selected).map(group => group.name)
      });
    });
  };

  handleSelect = (name: string) => {
    return addUserToGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      this.setState((state: State) => ({ selectedGroups: [...state.selectedGroups, name] }));
    });
  };

  handleUnselect = (name: string) => {
    return removeUserFromGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      this.setState((state: State) => ({
        selectedGroups: without(state.selectedGroups, name)
      }));
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

  renderElement = (name: string): React.ReactNode => {
    const group = find(this.state.groups, { name });
    return (
      <div className="select-list-list-item">
        {group === undefined ? (
          name
        ) : (
          <>
            {group.name}
            <br />
            <span className="note">{group.description}</span>
          </>
        )}
      </div>
    );
  };

  render() {
    const header = translate('users.update_groups');

    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          <SelectList
            elements={this.state.groups.map(group => group.name)}
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

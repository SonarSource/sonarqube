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
import { find, without } from 'lodash';
import * as React from 'react';
import Modal from 'sonar-ui-common/components/controls/Modal';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams
} from 'sonar-ui-common/components/controls/SelectList';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getUserGroups, UserGroup } from '../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: T.User;
}

interface State {
  needToReload: boolean;
  lastSearchParams?: SelectListSearchParams;
  groups: UserGroup[];
  groupsTotalCount?: number;
  selectedGroups: string[];
}

export default class GroupsForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      needToReload: false,
      groups: [],
      selectedGroups: []
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchUsers = (searchParams: SelectListSearchParams) =>
    getUserGroups({
      login: this.props.user.login,
      organization: undefined,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const more = searchParams.page != null && searchParams.page > 1;

          const groups = more ? [...prevState.groups, ...data.groups] : data.groups;
          const newSeletedGroups = data.groups.filter(gp => gp.selected).map(gp => gp.name);
          const selectedGroups = more
            ? [...prevState.selectedGroups, ...newSeletedGroups]
            : newSeletedGroups;

          return {
            lastSearchParams: searchParams,
            needToReload: false,
            groups,
            groupsTotalCount: data.paging.total,
            selectedGroups
          };
        });
      }
    });

  handleSelect = (name: string) =>
    addUserToGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedGroups: [...state.selectedGroups, name]
        }));
      }
    });

  handleUnselect = (name: string) =>
    removeUserFromGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedGroups: without(state.selectedGroups, name)
        }));
      }
    });

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

        <div className="modal-body modal-container">
          <SelectList
            elements={this.state.groups.map(group => group.name)}
            elementsTotalCount={this.state.groupsTotalCount}
            needToReload={
              this.state.needToReload &&
              this.state.lastSearchParams &&
              this.state.lastSearchParams.filter !== SelectListFilter.All
            }
            onSearch={this.fetchUsers}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedGroups}
            withPaging={true}
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

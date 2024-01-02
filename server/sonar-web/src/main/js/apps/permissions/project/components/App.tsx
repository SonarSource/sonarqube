/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import * as api from '../../../../api/permissions';
import withComponentContext from '../../../../app/components/componentContext/withComponentContext';
import VisibilitySelector from '../../../../components/common/VisibilitySelector';
import { translate } from '../../../../helpers/l10n';
import { Component, Paging, PermissionGroup, PermissionUser } from '../../../../types/types';
import AllHoldersList from '../../shared/components/AllHoldersList';
import { FilterOption } from '../../shared/components/SearchForm';
import '../../styles.css';
import { convertToPermissionDefinitions, PERMISSIONS_ORDER_BY_QUALIFIER } from '../../utils';
import PageHeader from './PageHeader';
import PublicProjectDisclaimer from './PublicProjectDisclaimer';

interface Props {
  component: Component;
  onComponentChange: (changes: Partial<Component>) => void;
}

interface State {
  disclaimer: boolean;
  filter: FilterOption;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  loading: boolean;
  query: string;
  selectedPermission?: string;
  users: PermissionUser[];
  usersPaging?: Paging;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      disclaimer: false,
      filter: 'all',
      groups: [],
      loading: true,
      query: '',
      users: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  loadUsersAndGroups = (userPage?: number, groupsPage?: number) => {
    const { component } = this.props;
    const { filter, query, selectedPermission } = this.state;

    const getUsers: Promise<{ paging?: Paging; users: PermissionUser[] }> =
      filter !== 'groups'
        ? api.getPermissionsUsersForComponent({
            projectKey: component.key,
            q: query || undefined,
            permission: selectedPermission,
            p: userPage,
          })
        : Promise.resolve({ paging: undefined, users: [] });

    const getGroups: Promise<{ paging?: Paging; groups: PermissionGroup[] }> =
      filter !== 'users'
        ? api.getPermissionsGroupsForComponent({
            projectKey: component.key,
            q: query || undefined,
            permission: selectedPermission,
            p: groupsPage,
          })
        : Promise.resolve({ paging: undefined, groups: [] });

    return Promise.all([getUsers, getGroups]);
  };

  loadHolders = () => {
    this.setState({ loading: true });
    return this.loadUsersAndGroups().then(([usersResponse, groupsResponse]) => {
      if (this.mounted) {
        this.setState({
          groups: groupsResponse.groups,
          groupsPaging: groupsResponse.paging,
          loading: false,
          users: usersResponse.users,
          usersPaging: usersResponse.paging,
        });
      }
    }, this.stopLoading);
  };

  onLoadMore = () => {
    const { usersPaging, groupsPaging } = this.state;
    this.setState({ loading: true });
    return this.loadUsersAndGroups(
      usersPaging ? usersPaging.pageIndex + 1 : 1,
      groupsPaging ? groupsPaging.pageIndex + 1 : 1
    ).then(([usersResponse, groupsResponse]) => {
      if (this.mounted) {
        this.setState(({ groups, users }) => ({
          groups: [...groups, ...groupsResponse.groups],
          groupsPaging: groupsResponse.paging,
          loading: false,
          users: [...users, ...usersResponse.users],
          usersPaging: usersResponse.paging,
        }));
      }
    }, this.stopLoading);
  };

  handleFilterChange = (filter: FilterOption) => {
    if (this.mounted) {
      this.setState({ filter }, this.loadHolders);
    }
  };

  handleQueryChange = (query: string) => {
    if (this.mounted) {
      this.setState({ query }, this.loadHolders);
    }
  };

  handlePermissionSelect = (selectedPermission?: string) => {
    if (this.mounted) {
      this.setState(
        (state: State) => ({
          selectedPermission:
            state.selectedPermission === selectedPermission ? undefined : selectedPermission,
        }),
        this.loadHolders
      );
    }
  };

  addPermissionToGroup = (group: string, permission: string) => {
    return this.state.groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  addPermissionToUser = (user: string, permission: string) => {
    return this.state.users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  removePermissionFromGroup = (group: string, permission: string) => {
    return this.state.groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  removePermissionFromUser = (user: string, permission: string) => {
    return this.state.users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  grantPermissionToGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState({
        loading: true,
        groups: this.addPermissionToGroup(group, permission),
      });
      return api
        .grantPermissionToGroup({
          projectKey: this.props.component.key,
          groupName: group,
          permission,
        })
        .then(this.stopLoading, () => {
          if (this.mounted) {
            this.setState({
              loading: false,
              groups: this.removePermissionFromGroup(group, permission),
            });
          }
        });
    }
    return Promise.resolve();
  };

  grantPermissionToUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState({
        loading: true,
        users: this.addPermissionToUser(user, permission),
      });
      return api
        .grantPermissionToUser({
          projectKey: this.props.component.key,
          login: user,
          permission,
        })
        .then(this.stopLoading, () => {
          if (this.mounted) {
            this.setState({
              loading: false,
              users: this.removePermissionFromUser(user, permission),
            });
          }
        });
    }
    return Promise.resolve();
  };

  revokePermissionFromGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState({
        loading: true,
        groups: this.removePermissionFromGroup(group, permission),
      });
      return api
        .revokePermissionFromGroup({
          projectKey: this.props.component.key,
          groupName: group,
          permission,
        })
        .then(this.stopLoading, () => {
          if (this.mounted) {
            this.setState({
              loading: false,
              groups: this.addPermissionToGroup(group, permission),
            });
          }
        });
    }
    return Promise.resolve();
  };

  revokePermissionFromUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState({
        loading: true,
        users: this.removePermissionFromUser(user, permission),
      });
      return api
        .revokePermissionFromUser({
          projectKey: this.props.component.key,
          login: user,
          permission,
        })
        .then(this.stopLoading, () => {
          if (this.mounted) {
            this.setState({
              loading: false,
              users: this.addPermissionToUser(user, permission),
            });
          }
        });
    }
    return Promise.resolve();
  };

  handleVisibilityChange = (visibility: string) => {
    if (visibility === 'public') {
      this.openDisclaimer();
    } else {
      this.turnProjectToPrivate();
    }
  };

  turnProjectToPublic = () => {
    this.props.onComponentChange({ visibility: 'public' });
    api.changeProjectVisibility(this.props.component.key, 'public').then(
      () => {
        this.loadHolders();
      },
      () => {
        this.props.onComponentChange({
          visibility: 'private',
        });
      }
    );
  };

  turnProjectToPrivate = () => {
    this.props.onComponentChange({ visibility: 'private' });
    api.changeProjectVisibility(this.props.component.key, 'private').then(
      () => {
        this.loadHolders();
      },
      () => {
        this.props.onComponentChange({
          visibility: 'public',
        });
      }
    );
  };

  openDisclaimer = () => {
    if (this.mounted) {
      this.setState({ disclaimer: true });
    }
  };

  closeDisclaimer = () => {
    if (this.mounted) {
      this.setState({ disclaimer: false });
    }
  };

  render() {
    const { component } = this.props;
    const {
      filter,
      groups,
      disclaimer,
      loading,
      selectedPermission,
      query,
      users,
      usersPaging,
      groupsPaging,
    } = this.state;
    const canTurnToPrivate =
      component.configuration && component.configuration.canUpdateProjectVisibilityToPrivate;

    let order = PERMISSIONS_ORDER_BY_QUALIFIER[component.qualifier];
    if (component.visibility === 'public') {
      order = without(order, 'user', 'codeviewer');
    }
    const permissions = convertToPermissionDefinitions(order, 'projects_role');

    return (
      <div className="page page-limited" id="project-permissions-page">
        <Helmet defer={false} title={translate('permissions.page')} />

        <PageHeader component={component} loadHolders={this.loadHolders} loading={loading} />
        <div>
          <VisibilitySelector
            canTurnToPrivate={canTurnToPrivate}
            className="big-spacer-top big-spacer-bottom"
            onChange={this.handleVisibilityChange}
            visibility={component.visibility}
          />
          {disclaimer && (
            <PublicProjectDisclaimer
              component={component}
              onClose={this.closeDisclaimer}
              onConfirm={this.turnProjectToPublic}
            />
          )}
        </div>
        <AllHoldersList
          filter={filter}
          grantPermissionToGroup={this.grantPermissionToGroup}
          grantPermissionToUser={this.grantPermissionToUser}
          groups={groups}
          groupsPaging={groupsPaging}
          onFilter={this.handleFilterChange}
          onLoadMore={this.onLoadMore}
          onSelectPermission={this.handlePermissionSelect}
          onQuery={this.handleQueryChange}
          query={query}
          revokePermissionFromGroup={this.revokePermissionFromGroup}
          revokePermissionFromUser={this.revokePermissionFromUser}
          selectedPermission={selectedPermission}
          users={users}
          usersPaging={usersPaging}
          permissions={permissions}
        />
      </div>
    );
  }
}

export default withComponentContext(App);

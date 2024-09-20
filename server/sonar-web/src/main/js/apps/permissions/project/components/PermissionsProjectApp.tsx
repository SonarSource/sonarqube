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
import { LargeCenteredLayout, PageContentFontWrapper } from 'design-system';
import { noop, without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import * as api from '../../../../api/permissions';
import { getComponents } from '../../../../api/project-management';
import withComponentContext from '../../../../app/components/componentContext/withComponentContext';
import AllHoldersList from '../../../../components/permissions/AllHoldersList';
import { FilterOption } from '../../../../components/permissions/SearchForm';
import { translate } from '../../../../helpers/l10n';
import {
  PERMISSIONS_ORDER_BY_QUALIFIER,
  convertToPermissionDefinitions,
} from '../../../../helpers/permissions';
import { ComponentContextShape } from '../../../../types/component';
import { Permissions } from '../../../../types/permissions';
import { Component, Paging, PermissionGroup, PermissionUser } from '../../../../types/types';
import '../../styles.css';
import PageHeader from './PageHeader';
import PermissionsProjectVisibility from './PermissionsProjectVisibility';
import PublicProjectDisclaimer from './PublicProjectDisclaimer';

interface Props extends ComponentContextShape {
  component: Component;
}

interface State {
  disclaimer: boolean;
  filter: FilterOption;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  isProjectManaged: boolean;
  loading: boolean;
  query: string;
  selectedPermission?: string;
  users: PermissionUser[];
  usersPaging?: Paging;
}

class PermissionsProjectApp extends React.PureComponent<Props, State> {
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
      isProjectManaged: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadHolders();
    this.getIsProjectManaged();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

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

    const getGroups: Promise<{ groups: PermissionGroup[]; paging?: Paging }> =
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

    this.loadUsersAndGroups().then(([usersResponse, groupsResponse]) => {
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

  getIsProjectManaged = () => {
    if (this.props.component.qualifier === ComponentQualifier.Project) {
      getComponents({ projects: this.props.component.key })
        .then((response) => {
          if (this.mounted) {
            const { managed } = response.components[0];
            this.setState({
              isProjectManaged: !!managed,
            });
          }
        })
        .catch(noop);
    }
  };

  handleLoadMore = () => {
    const { usersPaging, groupsPaging } = this.state;
    this.setState({ loading: true });

    this.loadUsersAndGroups(
      usersPaging ? usersPaging.pageIndex + 1 : 1,
      groupsPaging ? groupsPaging.pageIndex + 1 : 1,
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
        this.loadHolders,
      );
    }
  };

  addPermissionToGroup = (group: string, permission: string) => {
    return this.state.groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate,
    );
  };

  addPermissionToUser = (user: string, permission: string) => {
    return this.state.users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate,
    );
  };

  removePermissionFromGroup = (group: string, permission: string) => {
    return this.state.groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate,
    );
  };

  removePermissionFromUser = (user: string, permission: string) => {
    return this.state.users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate,
    );
  };

  handleGrantPermissionToGroup = (group: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .grantPermissionToGroup({
        projectKey: this.props.component.key,
        groupName: group,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState({
            loading: false,
            groups: this.addPermissionToGroup(group, permission),
          });
        }
      }, this.stopLoading);
  };

  handleGrantPermissionToUser = (user: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .grantPermissionToUser({
        projectKey: this.props.component.key,
        login: user,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState({
            loading: false,
            users: this.addPermissionToUser(user, permission),
          });
        }
      }, this.stopLoading);
  };

  handleRevokePermissionFromGroup = (group: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .revokePermissionFromGroup({
        projectKey: this.props.component.key,
        groupName: group,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState({
            loading: false,
            groups: this.removePermissionFromGroup(group, permission),
          });
        }
      }, this.stopLoading);
  };

  handleRevokePermissionFromUser = (user: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .revokePermissionFromUser({
        projectKey: this.props.component.key,
        login: user,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState({
            loading: false,
            users: this.removePermissionFromUser(user, permission),
          });
        }
      }, this.stopLoading);
  };

  handleVisibilityChange = (visibility: string) => {
    if (visibility === Visibility.Public) {
      this.openDisclaimer();
    } else {
      this.turnProjectToPrivate();
    }
  };

  handleTurnProjectToPublic = () => {
    this.setState({ loading: true });

    api
      .changeProjectVisibility(this.props.component.key, Visibility.Public)
      .then(() => {
        this.props.onComponentChange({ visibility: Visibility.Public });
        this.loadHolders();
      })
      .catch(noop);
  };

  turnProjectToPrivate = () => {
    this.setState({ loading: true });

    api
      .changeProjectVisibility(this.props.component.key, Visibility.Private)
      .then(() => {
        this.props.onComponentChange({ visibility: Visibility.Private });
        this.loadHolders();
      })
      .catch(noop);
  };

  openDisclaimer = () => {
    if (this.mounted) {
      this.setState({ disclaimer: true });
    }
  };

  handleCloseDisclaimer = () => {
    if (this.mounted) {
      this.setState({ disclaimer: false });
    }
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
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
      isProjectManaged,
    } = this.state;

    let order = PERMISSIONS_ORDER_BY_QUALIFIER[component.qualifier];
    if (component.visibility === Visibility.Public) {
      order = without(order, Permissions.Browse, Permissions.CodeViewer);
    }
    const permissions = convertToPermissionDefinitions(order, 'projects_role');

    return (
      <LargeCenteredLayout id="project-permissions-page">
        <PageContentFontWrapper className="sw-my-8 sw-typo-default">
          <Helmet defer={false} title={translate('permissions.page')} />

          <PageHeader
            isProjectManaged={isProjectManaged}
            component={component}
            loadHolders={this.loadHolders}
          />
          <div>
            <PermissionsProjectVisibility
              isProjectManaged={isProjectManaged}
              component={component}
              handleVisibilityChange={this.handleVisibilityChange}
              isLoading={loading}
            />

            <PublicProjectDisclaimer
              component={component}
              onClose={this.handleCloseDisclaimer}
              onConfirm={this.handleTurnProjectToPublic}
              isOpen={disclaimer}
            />
          </div>

          <AllHoldersList
            loading={loading}
            filter={filter}
            onGrantPermissionToGroup={this.handleGrantPermissionToGroup}
            onGrantPermissionToUser={this.handleGrantPermissionToUser}
            groups={groups}
            groupsPaging={groupsPaging}
            onFilter={this.handleFilterChange}
            onLoadMore={this.handleLoadMore}
            onSelectPermission={this.handlePermissionSelect}
            onQuery={this.handleQueryChange}
            query={query}
            onRevokePermissionFromGroup={this.handleRevokePermissionFromGroup}
            onRevokePermissionFromUser={this.handleRevokePermissionFromUser}
            selectedPermission={selectedPermission}
            users={users}
            usersPaging={usersPaging}
            permissions={permissions}
            isProjectManaged={isProjectManaged}
          />
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withComponentContext(PermissionsProjectApp);

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
import Helmet from 'react-helmet';
import { translate } from 'sonar-ui-common/helpers/l10n';
import * as api from '../../../api/permissions';
import HoldersList from '../../permissions/shared/components/HoldersList';
import SearchForm from '../../permissions/shared/components/SearchForm';
import {
  convertToPermissionDefinitions,
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE
} from '../../permissions/utils';
import TemplateDetails from './TemplateDetails';
import TemplateHeader from './TemplateHeader';

interface Props {
  organization: T.Organization | undefined;
  refresh: () => void;
  template: T.PermissionTemplate;
  topQualifiers: string[];
}

interface State {
  filter: string;
  groups: T.PermissionGroup[];
  loading: boolean;
  query: string;
  selectedPermission?: string;
  users: T.PermissionUser[];
}

export default class Template extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    filter: 'all',
    groups: [],
    loading: false,
    query: '',
    users: []
  };

  componentDidMount() {
    this.mounted = true;
    this.requestHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  requestHolders = (realQuery?: string) => {
    this.setState({ loading: true });

    const { template } = this.props;
    const { query, filter, selectedPermission } = this.state;
    const requests = [];

    const finalQuery = realQuery != null ? realQuery : query;

    if (filter !== 'groups') {
      requests.push(api.getPermissionTemplateUsers(template.id, finalQuery, selectedPermission));
    } else {
      requests.push(Promise.resolve([]));
    }

    if (filter !== 'users') {
      requests.push(api.getPermissionTemplateGroups(template.id, finalQuery, selectedPermission));
    } else {
      requests.push(Promise.resolve([]));
    }

    return Promise.all(requests).then(responses => {
      if (this.mounted) {
        this.setState({
          users: responses[0],
          groups: responses[1],
          loading: false
        });
      }
    });
  };

  handleToggleUser = (user: T.PermissionUser, permission: string) => {
    if (user.login === '<creator>') {
      return this.handleToggleProjectCreator(user, permission);
    }
    const { template, organization } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const data: { templateId: string; login: string; permission: string; organization?: string } = {
      templateId: template.id,
      login: user.login,
      permission
    };
    if (organization) {
      data.organization = organization.key;
    }
    const request = hasPermission
      ? api.revokeTemplatePermissionFromUser(data)
      : api.grantTemplatePermissionToUser(data);
    return request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleToggleProjectCreator = (user: T.PermissionUser, permission: string) => {
    const { template } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const request = hasPermission
      ? api.removeProjectCreatorFromTemplate(template.id, permission)
      : api.addProjectCreatorToTemplate(template.id, permission);
    return request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleToggleGroup = (group: T.PermissionGroup, permission: string) => {
    const { template, organization } = this.props;
    const hasPermission = group.permissions.includes(permission);
    const data = {
      templateId: template.id,
      groupName: group.name,
      permission
    };
    if (organization) {
      Object.assign(data, { organization: organization.key });
    }
    const request = hasPermission
      ? api.revokeTemplatePermissionFromGroup(data)
      : api.grantTemplatePermissionToGroup(data);
    return request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleSearch = (query: string) => {
    this.setState({ query });
    this.requestHolders(query);
  };

  handleFilter = (filter: string) => {
    this.setState({ filter }, this.requestHolders);
  };

  handleSelectPermission = (selectedPermission: string) => {
    if (selectedPermission === this.state.selectedPermission) {
      this.setState({ selectedPermission: undefined }, this.requestHolders);
    } else {
      this.setState({ selectedPermission }, this.requestHolders);
    }
  };

  shouldDisplayCreator = (creatorPermissions: string[]) => {
    const { filter, query, selectedPermission } = this.state;
    const CREATOR_NAME = translate('permission_templates.project_creators');

    const isFiltered = filter !== 'all';

    const matchQuery = !query || CREATOR_NAME.toLocaleLowerCase().includes(query.toLowerCase());

    const matchPermission =
      selectedPermission === undefined || creatorPermissions.includes(selectedPermission);

    return !isFiltered && matchQuery && matchPermission;
  };

  render() {
    const permissions = convertToPermissionDefinitions(
      PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
      'projects_role'
    );

    const allUsers = [...this.state.users];

    const creatorPermissions = this.props.template.permissions
      .filter(p => p.withProjectCreator)
      .map(p => p.key);

    if (this.shouldDisplayCreator(creatorPermissions)) {
      const creator = {
        login: '<creator>',
        name: translate('permission_templates.project_creators'),
        permissions: creatorPermissions
      };

      allUsers.unshift(creator);
    }

    return (
      <div className="page page-limited">
        <Helmet title={this.props.template.name} />

        <TemplateHeader
          loading={this.state.loading}
          organization={this.props.organization}
          refresh={this.props.refresh}
          template={this.props.template}
          topQualifiers={this.props.topQualifiers}
        />

        <TemplateDetails organization={this.props.organization} template={this.props.template} />

        <HoldersList
          groups={this.state.groups}
          onSelectPermission={this.handleSelectPermission}
          onToggleGroup={this.handleToggleGroup}
          onToggleUser={this.handleToggleUser}
          permissions={permissions}
          selectedPermission={this.state.selectedPermission}
          showPublicProjectsWarning={true}
          users={allUsers}>
          <SearchForm
            filter={this.state.filter}
            onFilter={this.handleFilter}
            onSearch={this.handleSearch}
            query={this.state.query}
          />
        </HoldersList>
      </div>
    );
  }
}

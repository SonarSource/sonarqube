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
import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { debounce } from 'lodash';
import TemplateHeader from './TemplateHeader';
import TemplateDetails from './TemplateDetails';
import HoldersList from '../../permissions/shared/components/HoldersList';
import SearchForm from '../../permissions/shared/components/SearchForm';
import { PERMISSIONS_ORDER_FOR_PROJECT } from '../../permissions/project/constants';
import * as api from '../../../api/permissions';
import { translate } from '../../../helpers/l10n';

export default class Template extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    template: PropTypes.object.isRequired,
    refresh: PropTypes.func.isRequired,
    topQualifiers: PropTypes.array.isRequired
  };

  state = {
    loading: false,
    users: [],
    groups: [],
    query: '',
    filter: 'all',
    selectedPermission: null
  };

  componentDidMount() {
    this.mounted = true;
    this.requestHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  requestHolders = realQuery => {
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

  handleToggleUser = (user, permission) => {
    if (user.login === '<creator>') {
      this.handleToggleProjectCreator(user, permission);
      return;
    }
    const { template, organization } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const data = {
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
    request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleToggleProjectCreator = (user, permission) => {
    const { template } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const request = hasPermission
      ? api.removeProjectCreatorFromTemplate(template.id, permission)
      : api.addProjectCreatorToTemplate(template.id, permission);
    request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleToggleGroup = (group, permission) => {
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
    request.then(() => this.requestHolders()).then(this.props.refresh);
  };

  handleSearch = query => {
    this.setState({ query });
    this.requestHolders(query);
  };

  handleFilter = filter => {
    this.setState({ filter }, this.requestHolders);
  };

  handleSelectPermission = selectedPermission => {
    if (selectedPermission === this.state.selectedPermission) {
      this.setState({ selectedPermission: null }, this.requestHolders);
    } else {
      this.setState({ selectedPermission }, this.requestHolders);
    }
  };

  shouldDisplayCreator = creatorPermissions => {
    const { filter, query, selectedPermission } = this.state;
    const CREATOR_NAME = translate('permission_templates.project_creators');

    const isFiltered = filter !== 'all';

    const matchQuery = !query || CREATOR_NAME.toLocaleLowerCase().includes(query.toLowerCase());

    const matchPermission =
      selectedPermission == null || creatorPermissions.includes(selectedPermission);

    return !isFiltered && matchQuery && matchPermission;
  };

  render() {
    const permissions = PERMISSIONS_ORDER_FOR_PROJECT.map(p => ({
      key: p,
      name: translate('projects_role', p),
      description: translate('projects_role', p, 'desc')
    }));

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
          organization={this.props.organization}
          template={this.props.template}
          loading={this.state.loading}
          refresh={this.props.refresh}
          topQualifiers={this.props.topQualifiers}
        />

        <TemplateDetails organization={this.props.organization} template={this.props.template} />

        <HoldersList
          permissions={permissions}
          selectedPermission={this.state.selectedPermission}
          users={allUsers}
          groups={this.state.groups}
          showPublicProjectsWarning={true}
          onSelectPermission={this.handleSelectPermission}
          onToggleUser={this.handleToggleUser}
          onToggleGroup={this.handleToggleGroup}>
          <SearchForm
            query={this.state.query}
            filter={this.state.filter}
            onSearch={this.handleSearch}
            onFilter={this.handleFilter}
          />
        </HoldersList>
      </div>
    );
  }
}

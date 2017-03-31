/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Helmet from 'react-helmet';
import { debounce } from 'lodash';
import TemplateHeader from './TemplateHeader';
import TemplateDetails from './TemplateDetails';
import HoldersList from '../../permissions/shared/components/HoldersList';
import SearchForm from '../../permissions/shared/components/SearchForm';
import { PERMISSIONS_ORDER_FOR_PROJECT } from '../../permissions/project/constants';
import * as api from '../../../api/permissions';
import { translate } from '../../../helpers/l10n';
import withStore from '../../../store/utils/withStore';

class Template extends React.Component {
  static propTypes = {
    organization: React.PropTypes.object,
    template: React.PropTypes.object.isRequired,
    refresh: React.PropTypes.func.isRequired,
    topQualifiers: React.PropTypes.array.isRequired
  };

  componentWillMount() {
    this.requestHolders = this.requestHolders.bind(this);
    this.requestHoldersDebounced = debounce(this.requestHolders, 250);
    this.handleSelectPermission = this.handleSelectPermission.bind(this);
    this.handleToggleUser = this.handleToggleUser.bind(this);
    this.handleToggleGroup = this.handleToggleGroup.bind(this);
    this.handleSearch = this.handleSearch.bind(this);
    this.handleFilter = this.handleFilter.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.requestHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  requestHolders(realQuery) {
    this.props.updateStore({ loading: true });

    const { template } = this.props;
    const { query, filter, selectedPermission } = this.props.getStore();
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
      this.props.updateStore({
        users: responses[0],
        groups: responses[1],
        loading: false
      });
    });
  }

  handleToggleUser(user, permission) {
    if (user.login === '<creator>') {
      this.handleToggleProjectCreator(user, permission);
      return;
    }
    const { template } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const request = hasPermission
      ? api.revokeTemplatePermissionFromUser(template.id, user.login, permission)
      : api.grantTemplatePermissionToUser(template.id, user.login, permission);
    request.then(() => this.requestHolders()).then(this.props.refresh);
  }

  handleToggleProjectCreator(user, permission) {
    const { template } = this.props;
    const hasPermission = user.permissions.includes(permission);
    const request = hasPermission
      ? api.removeProjectCreatorFromTemplate(template.id, permission)
      : api.addProjectCreatorToTemplate(template.id, permission);
    request.then(() => this.requestHolders()).then(this.props.refresh);
  }

  handleToggleGroup(group, permission) {
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
  }

  handleSearch(query) {
    this.props.updateStore({ query });
    if (query.length === 0 || query.length > 2) {
      this.requestHoldersDebounced(query);
    }
  }

  handleFilter(filter) {
    this.props.updateStore({ filter });
    this.requestHolders();
  }

  handleSelectPermission(selectedPermission) {
    const store = this.props.getStore();
    if (selectedPermission === store.selectedPermission) {
      this.props.updateStore({ selectedPermission: null });
    } else {
      this.props.updateStore({ selectedPermission });
    }
    this.requestHolders();
  }

  shouldDisplayCreator(creatorPermissions) {
    const store = this.props.getStore();
    const CREATOR_NAME = translate('permission_templates.project_creators');

    const isFiltered = store.filter !== 'all';

    const matchQuery = !store.query ||
      CREATOR_NAME.toLocaleLowerCase().includes(store.query.toLowerCase());

    const matchPermission = store.selectedPermission == null ||
      creatorPermissions.includes(store.selectedPermission);

    return !isFiltered && matchQuery && matchPermission;
  }

  render() {
    const title = translate('permission_templates.page') + ' - ' + this.props.template.name;

    const permissions = PERMISSIONS_ORDER_FOR_PROJECT.map(p => ({
      key: p,
      name: translate('projects_role', p),
      description: translate('projects_role', p, 'desc')
    }));

    const store = this.props.getStore();
    const allUsers = [...store.users];

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
        <Helmet title={title} titleTemplate="SonarQube - %s" />

        <TemplateHeader
          organization={this.props.organization}
          template={this.props.template}
          loading={store.loading}
          refresh={this.props.refresh}
          topQualifiers={this.props.topQualifiers}
        />

        <TemplateDetails organization={this.props.organization} template={this.props.template} />

        <HoldersList
          permissions={permissions}
          selectedPermission={store.selectedPermission}
          users={allUsers}
          groups={store.groups}
          onSelectPermission={this.handleSelectPermission}
          onToggleUser={this.handleToggleUser}
          onToggleGroup={this.handleToggleGroup}>

          <SearchForm
            query={store.query}
            filter={store.filter}
            onSearch={this.handleSearch}
            onFilter={this.handleFilter}
          />

        </HoldersList>
      </div>
    );
  }
}

export default withStore(Template, {
  loading: false,
  users: [],
  groups: [],
  query: '',
  filter: 'all',
  selectedPermission: null
});

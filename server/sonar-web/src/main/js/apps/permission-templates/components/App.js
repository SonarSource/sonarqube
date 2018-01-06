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
import Home from './Home';
import Template from './Template';
import OrganizationHelmet from '../../../components/common/OrganizationHelmet';
import { getPermissionTemplates } from '../../../api/permissions';
import { sortPermissions, mergePermissionsToTemplates, mergeDefaultsToTemplates } from '../utils';
import { translate } from '../../../helpers/l10n';
import '../../permissions/styles.css';

export default class App extends React.PureComponent {
  static propTypes = {
    location: PropTypes.object.isRequired,
    organization: PropTypes.object,
    topQualifiers: PropTypes.array.isRequired
  };

  state = {
    ready: false,
    permissions: [],
    permissionTemplates: []
  };

  componentDidMount() {
    this.mounted = true;
    this.requestPermissions();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  requestPermissions = () => {
    const { organization } = this.props;
    const request = organization
      ? getPermissionTemplates(organization.key)
      : getPermissionTemplates();
    return request.then(r => {
      if (this.mounted) {
        const permissions = sortPermissions(r.permissions);
        const permissionTemplates = mergeDefaultsToTemplates(
          mergePermissionsToTemplates(r.permissionTemplates, permissions),
          r.defaultTemplates
        );
        this.setState({
          ready: true,
          permissionTemplates,
          permissions
        });
      }
    });
  };

  renderTemplate(id) {
    if (!this.state.ready) {
      return null;
    }

    const template = this.state.permissionTemplates.find(t => t.id === id);
    return (
      <Template
        organization={this.props.organization}
        template={template}
        refresh={this.requestPermissions}
        topQualifiers={this.props.topQualifiers}
      />
    );
  }

  renderHome() {
    return (
      <Home
        organization={this.props.organization}
        topQualifiers={this.props.topQualifiers}
        permissions={this.state.permissions}
        permissionTemplates={this.state.permissionTemplates}
        ready={this.state.ready}
        refresh={this.requestPermissions}
      />
    );
  }

  render() {
    const { id } = this.props.location.query;
    return (
      <div>
        <OrganizationHelmet
          title={translate('permission_templates.page')}
          organization={this.props.organization}
        />

        {id && this.renderTemplate(id)}
        {!id && this.renderHome()}
      </div>
    );
  }
}

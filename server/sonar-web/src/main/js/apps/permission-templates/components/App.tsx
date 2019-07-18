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
import { Location } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getPermissionTemplates } from '../../../api/permissions';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import OrganizationHelmet from '../../../components/common/OrganizationHelmet';
import { getAppState, Store } from '../../../store/rootReducer';
import '../../permissions/styles.css';
import { mergeDefaultsToTemplates, mergePermissionsToTemplates, sortPermissions } from '../utils';
import Home from './Home';
import Template from './Template';

interface Props {
  location: Location;
  organization: T.Organization | undefined;
  topQualifiers: string[];
}

interface State {
  ready: boolean;
  permissions: T.Permission[];
  permissionTemplates: T.PermissionTemplate[];
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
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
        this.setState({ ready: true, permissionTemplates, permissions });
      }
    });
  };

  renderTemplate(id: string) {
    if (!this.state.ready) {
      return null;
    }

    const template = this.state.permissionTemplates.find(t => t.id === id);
    if (!template) {
      return null;
    }

    return (
      <Template
        organization={this.props.organization}
        refresh={this.requestPermissions}
        template={template}
        topQualifiers={this.props.topQualifiers}
      />
    );
  }

  renderHome() {
    return (
      <Home
        organization={this.props.organization}
        permissionTemplates={this.state.permissionTemplates}
        permissions={this.state.permissions}
        ready={this.state.ready}
        refresh={this.requestPermissions}
        topQualifiers={this.props.topQualifiers}
      />
    );
  }

  render() {
    const { id } = this.props.location.query;
    return (
      <div>
        <Suggestions suggestions="permission_templates" />
        <OrganizationHelmet
          organization={this.props.organization}
          title={translate('permission_templates.page')}
        />

        {id && this.renderTemplate(id)}
        {!id && this.renderHome()}
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({ topQualifiers: getAppState(state).qualifiers });

export default connect(mapStateToProps)(App);

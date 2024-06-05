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

import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location } from '~sonar-aligned/types/router';
import { getPermissionTemplates } from '../../../api/permissions';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';
import { Permission, PermissionTemplate } from '../../../types/types';
import '../../permissions/styles.css';
import { mergeDefaultsToTemplates, mergePermissionsToTemplates, sortPermissions } from '../utils';
import Home from './Home';
import Template from './Template';

interface Props {
  appState: AppState;
  location: Location;
}

interface State {
  permissionTemplates: PermissionTemplate[];
  permissions: Permission[];
  ready: boolean;
}

class PermissionTemplatesApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    ready: false,
    permissions: [],
    permissionTemplates: [],
  };

  componentDidMount() {
    this.mounted = true;
    this.handleRefresh();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleRefresh = async () => {
    const { permissions, defaultTemplates, permissionTemplates } = await getPermissionTemplates();
    if (this.mounted) {
      const sortedPerm = sortPermissions(permissions);
      const permissionTemplatesMerged = mergeDefaultsToTemplates(
        mergePermissionsToTemplates(permissionTemplates, sortedPerm),
        defaultTemplates,
      );
      this.setState({
        ready: true,
        permissionTemplates: permissionTemplatesMerged,
        permissions: sortedPerm,
      });
    }
  };

  renderTemplate(id: string) {
    const { permissionTemplates, ready } = this.state;
    if (!ready) {
      return null;
    }

    const template = permissionTemplates.find((t) => t.id === id);
    if (!template) {
      return null;
    }

    return (
      <Template
        refresh={this.handleRefresh}
        template={template}
        topQualifiers={this.props.appState.qualifiers}
      />
    );
  }

  render() {
    const { appState, location } = this.props;
    const { id } = location.query;
    const { permissionTemplates, permissions, ready } = this.state;
    return (
      <>
        <Helmet defer={false} title={translate('permission_templates.page')} />

        {id === undefined ? (
          <Home
            permissionTemplates={permissionTemplates}
            permissions={permissions}
            ready={ready}
            refresh={this.handleRefresh}
            topQualifiers={appState.qualifiers}
          />
        ) : (
          this.renderTemplate(id)
        )}
      </>
    );
  }
}

export default withRouter(withAppStateContext(PermissionTemplatesApp));

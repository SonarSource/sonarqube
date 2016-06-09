/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Header from './Header';
import List from './List';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { getPermissionTemplates } from '../../../api/permissions';
import { sortPermissions, mergePermissionsToTemplates, mergeDefaultsToTemplates } from '../utils';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

export default class App extends React.Component {
  static propTypes = {
    topQualifiers: React.PropTypes.array.isRequired
  };

  state = {
    ready: false,
    permissions: [],
    permissionTemplates: []
  };

  componentWillMount () {
    this.requestPermissions = this.requestPermissions.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    this.requestPermissions();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  requestPermissions () {
    getPermissionTemplates().then(r => {
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
  }

  render () {
    return (
        <div className="page page-limited">
          <Helmet
              title={translate('permission_templates.page')}
              titleTemplate="SonarQube - %s"/>

          <Header
              ready={this.state.ready}
              refresh={this.requestPermissions}/>

          <TooltipsContainer>
            <List
                permissionTemplates={this.state.permissionTemplates}
                permissions={this.state.permissions}
                topQualifiers={this.props.topQualifiers}
                refresh={this.requestPermissions}/>
          </TooltipsContainer>
        </div>
    );
  }
}

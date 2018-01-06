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
import Header from './Header';
import List from './List';
import { translate } from '../../../helpers/l10n';

export default class Home extends React.PureComponent {
  static propTypes = {
    organization: PropTypes.object,
    topQualifiers: PropTypes.array.isRequired,
    permissions: PropTypes.array.isRequired,
    permissionTemplates: PropTypes.array.isRequired,
    ready: PropTypes.bool.isRequired,
    refresh: PropTypes.func.isRequired
  };

  render() {
    return (
      <div className="page page-limited">
        <Helmet title={translate('permission_templates.page')} />

        <Header
          organization={this.props.organization}
          ready={this.props.ready}
          refresh={this.props.refresh}
        />

        <List
          organization={this.props.organization}
          permissionTemplates={this.props.permissionTemplates}
          permissions={this.props.permissions}
          topQualifiers={this.props.topQualifiers}
          refresh={this.props.refresh}
        />
      </div>
    );
  }
}

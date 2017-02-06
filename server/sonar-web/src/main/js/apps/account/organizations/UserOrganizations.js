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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import { translate } from '../../../helpers/l10n';

export default class UserOrganizations extends React.Component {
  render () {
    const title = translate('my_account.organizations') + ' - ' + translate('my_account.page');

    return (
        <div className="account-body account-container">
          <Helmet title={title} titleTemplate="%s - SonarQube"/>

          <header className="page-header">
            <h2 className="page-title">{translate('my_account.organizations')}</h2>
            <div className="page-actions">
              <Link to="/account/organizations/create" className="button">{translate('create')}</Link>
            </div>
            <div className="page-description">
              {translate('my_account.organizations.description')}
            </div>
          </header>

          {this.props.children}
        </div>
    );
  }
}

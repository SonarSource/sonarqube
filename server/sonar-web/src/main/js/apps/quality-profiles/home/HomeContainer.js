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
import PageHeader from './PageHeader';
import Evolution from './Evolution';
import ProfilesList from './ProfilesList';
import { translate } from '../../../helpers/l10n';

export default class HomeContainer extends React.Component {
  render() {
    return (
      <div>
        <Helmet title={translate('quality_profiles.page')} titleTemplate="SonarQube - %s" />

        <PageHeader {...this.props} />

        <div className="page-with-sidebar">
          <div className="page-main">
            <ProfilesList {...this.props} />
          </div>
          <div className="page-sidebar">
            <Evolution {...this.props} />
          </div>
        </div>
      </div>
    );
  }
}

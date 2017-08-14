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
// @flow
import React from 'react';
import OAuthProviders from '../../sessions/components/OAuthProviders';
import { getIdentityProviders } from '../../../api/users';

/*::
type State = {
  identityProviders: Array<{
    backgroundColor: string,
    iconPath: string,
    key: string,
    name: string
  }>,
  loading: boolean
};
*/

export default class SonarCloudGetStarted extends React.PureComponent {
  /*:: mounted: boolean; */
  state /*: State */ = {
    identityProviders: [],
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders = () => {
    this.setState({ loading: true });
    getIdentityProviders().then(({ identityProviders }) => {
      if (this.mounted) {
        this.setState({ identityProviders, loading: false });
      }
    });
  };

  formatLabel = (name /*: string */) => `Start with ${name}`;

  render() {
    if (this.state.loading) {
      return null;
    }

    return (
      <div className="sqcom-get-started">
        <OAuthProviders
          formatLabel={this.formatLabel}
          identityProviders={this.state.identityProviders}
        />
      </div>
    );
  }
}

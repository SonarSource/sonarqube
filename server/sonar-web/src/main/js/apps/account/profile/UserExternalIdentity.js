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
import { getIdentityProviders } from '../../../api/users';
import * as theme from '../../../app/theme';
import { getTextColor } from '../../../helpers/colors';

export default class UserExternalIdentity extends React.PureComponent {
  state = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.user !== this.props.user) {
      this.this.fetchIdentityProviders();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders() {
    this.setState({ loading: true });
    getIdentityProviders()
      .then(r => r.identityProviders)
      .then(providers => {
        if (this.mounted) {
          const identityProvider = providers.find(
            provider => provider.key === this.props.user.externalProvider
          );
          this.setState({ loading: false, identityProvider });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  }

  render() {
    const { user } = this.props;
    const { loading, identityProvider } = this.state;

    if (loading) {
      return null;
    }

    if (!identityProvider) {
      return (
        <div>
          {user.externalProvider}
          {': '}
          {user.externalIdentity}
        </div>
      );
    }

    return (
      <div
        className="identity-provider"
        style={{
          backgroundColor: identityProvider.backgroundColor,
          color: getTextColor(identityProvider.backgroundColor, theme.secondFontColor)
        }}>
        <img
          className="little-spacer-right"
          src={window.baseUrl + identityProvider.iconPath}
          width="14"
          height="14"
          alt={identityProvider.name}
        />{' '}
        {user.externalIdentity}
      </div>
    );
  }
}

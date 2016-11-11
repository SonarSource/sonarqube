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
import './OAuthProvider.css';

export default class OAuthProvider extends React.Component {
  static propTypes = {
    provider: React.PropTypes.shape({
      key: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired,
      iconPath: React.PropTypes.string.isRequired,
      backgroundColor: React.PropTypes.string.isRequired
    }).isRequired
  };

  render () {
    const { key, name, iconPath, backgroundColor } = this.props.provider;

    const url = window.baseUrl + '/sessions/init/' + key;
    const label = 'Log in with ' + name;

    return (
        <a className="oauth-provider" href={url} style={{ backgroundColor }} title={label}>
          <img alt={name} width="20" height="20" src={window.baseUrl + iconPath}/>
          <span>{label}</span>
        </a>
    );
  }
}

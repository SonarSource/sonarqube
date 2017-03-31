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
import { Link } from 'react-router';

export default class ProfileLink extends React.Component {
  static propTypes = {
    profileKey: React.PropTypes.string.isRequired
  };

  render() {
    const { profileKey, children, ...other } = this.props;
    const query = { key: profileKey };
    return (
      <Link
        to={{ pathname: '/profiles/show', query }}
        activeClassName="link-no-underline"
        {...other}>
        {children}
      </Link>
    );
  }
}

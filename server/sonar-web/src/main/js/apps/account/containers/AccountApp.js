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
import React, { Component, cloneElement } from 'react';

import Nav from '../components/Nav';
import { getIssueFilters } from '../../../api/issues';

export default class AccountApp extends Component {
  state = {};

  componentDidMount () {
    this.fetchFavoriteIssueFilters();
  }

  fetchFavoriteIssueFilters () {
    getIssueFilters().then(issueFilters => {
      const favoriteIssueFilters = issueFilters.filter(f => f.favorite);
      this.setState({ issueFilters: favoriteIssueFilters });
    });
  }

  render () {
    const { user } = window.sonarqube;
    const { favorites } = user;
    const { issueFilters } = this.state;
    const children = cloneElement(this.props.children, {
      measureFilters: user.favoriteMeasureFilters,
      user,
      favorites,
      issueFilters
    });

    return (
        <div>
          <Nav user={user}/>
          {children}
        </div>
    );
  }
}

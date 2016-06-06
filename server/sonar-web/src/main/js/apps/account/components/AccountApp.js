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
import Nav from './Nav';
import { getCurrentUser } from '../../../api/users';
import { getIssueFilters } from '../../../api/issues';
import '../account.css';

export default class AccountApp extends Component {
  state = {
    loading: true
  };

  componentDidMount () {
    this.mounted = true;
    Promise.all([
      this.loadUser(),
      this.loadFavoriteIssueFilters()
    ]).then(() => this.finishLoading());
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadUser () {
    return getCurrentUser().then(user => {
      if (this.mounted) {
        this.setState({ user });
      }
    });
  }

  loadFavoriteIssueFilters () {
    return getIssueFilters().then(issueFilters => {
      const favoriteIssueFilters = issueFilters.filter(f => f.favorite);
      this.setState({ issueFilters: favoriteIssueFilters });
    });
  }

  finishLoading () {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  }

  render () {
    const { user, issueFilters, loading } = this.state;

    if (loading) {
      return (
          <div>
            <i className="spinner spinner-margin"/>
          </div>
      );
    }

    const { favorites } = window.sonarqube.user;
    const measureFilters = window.sonarqube.user.favoriteMeasureFilters;
    const children = cloneElement(this.props.children, {
      measureFilters,
      user,
      favorites,
      issueFilters
    });

    return (
        <div className="account-page">
          <Nav user={user}/>
          {children}
        </div>
    );
  }
}

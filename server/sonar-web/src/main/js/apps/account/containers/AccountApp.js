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
import { connect } from 'react-redux';

import Nav from '../components/Nav';
import { fetchUser } from '../store/actions';
import { getIssueFilters } from '../../../api/issues';

class AccountApp extends Component {
  state = {};

  componentDidMount () {
    this.props.fetchUser();
    this.fetchFavoriteIssueFilters();
  }

  fetchFavoriteIssueFilters () {
    getIssueFilters().then(issueFilters => {
      const favoriteIssueFilters = issueFilters.filter(f => f.favorite);
      this.setState({ issueFilters: favoriteIssueFilters });
    });
  }

  render () {
    const { user } = this.props;

    if (!user) {
      return null;
    }

    const { favorites } = window.sonarqube.user;
    const { issueFilters } = this.state;
    const children = cloneElement(this.props.children, {
      measureFilters: user.favoriteMeasureFilters,
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

function mapStateToProps (state) {
  return { user: state.user };
}

function mapDispatchToProps (dispatch) {
  return { fetchUser: () => dispatch(fetchUser()) };
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(AccountApp);

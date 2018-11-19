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
import * as React from 'react';
import { connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  getCurrentUser,
  getLanguages,
  areThereCustomOrganizations
} from '../../../store/rootReducer';

interface Props {
  currentUser: { isLoggedIn: boolean };
  languages: { [key: string]: { key: string; name: string } };
  organizationsEnabled: boolean;
}

class App extends React.PureComponent<Props> {
  static childContextTypes = {
    currentUser: PropTypes.object.isRequired,
    languages: PropTypes.object.isRequired,
    organizationsEnabled: PropTypes.bool
  };

  getChildContext() {
    return {
      currentUser: this.props.currentUser,
      languages: this.props.languages,
      organizationsEnabled: this.props.organizationsEnabled
    };
  }

  componentDidMount() {
    const elem = document.querySelector('html');
    if (elem) {
      elem.classList.add('dashboard-page');
    }
  }

  componentWillUnmount() {
    const elem = document.querySelector('html');
    if (elem) {
      elem.classList.remove('dashboard-page');
    }
  }

  render() {
    return <div id="projects-page">{this.props.children}</div>;
  }
}

const mapStateToProps = (state: any) => ({
  currentUser: getCurrentUser(state),
  languages: getLanguages(state),
  organizationsEnabled: areThereCustomOrganizations(state)
});

export default connect<any, any, any>(mapStateToProps)(App);

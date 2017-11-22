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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { CurrentUser } from '../types';
import { getCurrentUser, getGlobalSettingValue } from '../../store/rootReducer';

interface Props {
  currentUser: CurrentUser;
  onSonarCloud?: { value: any };
}

class Landing extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object
  };

  componentDidMount() {
    const { currentUser, onSonarCloud } = this.props;
    if (currentUser.isLoggedIn) {
      this.context.router.replace(`/projects`);
    } else if (onSonarCloud && onSonarCloud.value === 'true') {
      window.location.href = 'https://about.sonarcloud.io';
    } else {
      this.context.router.replace('/about');
    }
  }

  render() {
    return null;
  }
}

const mapStateToProps = (state: any) => ({
  currentUser: getCurrentUser(state),
  onSonarCloud: getGlobalSettingValue(state, 'sonar.sonarcloud.enabled')
});

export default connect<Props, {}, Props>(mapStateToProps)(Landing);

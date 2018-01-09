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
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { CurrentUser, isLoggedIn } from '../types';
import { getCurrentUser, getGlobalSettingValue } from '../../store/rootReducer';
import { getHomePageUrl } from '../../helpers/urls';

interface Props {
  currentUser: CurrentUser;
  onSonarCloud: boolean;
}

class Landing extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  componentDidMount() {
    const { currentUser, onSonarCloud } = this.props;
    if (isLoggedIn(currentUser)) {
      if (onSonarCloud && currentUser.homepage) {
        const homepage = getHomePageUrl(currentUser.homepage);
        this.context.router.replace(homepage);
      } else {
        this.context.router.replace('/projects');
      }
    } else if (onSonarCloud) {
      window.location.href = 'https://about.sonarcloud.io';
    } else {
      this.context.router.replace('/about');
    }
  }

  render() {
    return null;
  }
}

const mapStateToProps = (state: any) => {
  const onSonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');
  return {
    currentUser: getCurrentUser(state),
    onSonarCloud: Boolean(onSonarCloudSetting && onSonarCloudSetting.value === 'true')
  };
};

export default connect<Props>(mapStateToProps)(Landing);

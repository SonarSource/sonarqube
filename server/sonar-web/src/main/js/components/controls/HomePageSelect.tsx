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
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import Tooltip from './Tooltip';
import HomeIcon from '../icons-components/HomeIcon';
import { CurrentUser, isLoggedIn, HomePage, isSameHomePage } from '../../app/types';
import { translate } from '../../helpers/l10n';
import { getCurrentUser, getGlobalSettingValue } from '../../store/rootReducer';
import { setHomePage } from '../../store/users/actions';

interface StateProps {
  currentUser: CurrentUser;
  onSonarCloud: boolean;
}

interface DispatchProps {
  setHomePage: (homepage: HomePage) => void;
}

interface Props extends StateProps, DispatchProps {
  className?: string;
  currentPage: HomePage;
}

class HomePageSelect extends React.PureComponent<Props> {
  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.setHomePage(this.props.currentPage);
  };

  render() {
    const { currentPage, currentUser, onSonarCloud } = this.props;

    if (!isLoggedIn(currentUser) || !onSonarCloud) {
      return null;
    }

    const { homepage } = currentUser;
    const checked = homepage !== undefined && isSameHomePage(homepage, currentPage);
    const tooltip = checked ? translate('homepage.current') : translate('homepage.check');

    return (
      <Tooltip overlay={tooltip} placement="left">
        {checked ? (
          <span className={classNames('display-inline-block', this.props.className)}>
            <HomeIcon filled={checked} />
          </span>
        ) : (
          <a
            className={classNames(
              'link-no-underline',
              'display-inline-block',
              this.props.className
            )}
            href="#"
            onClick={this.handleClick}>
            <HomeIcon filled={checked} />
          </a>
        )}
      </Tooltip>
    );
  }
}

const mapStateToProps = (state: any): StateProps => {
  const sonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');

  return {
    currentUser: getCurrentUser(state),
    onSonarCloud: Boolean(sonarCloudSetting && sonarCloudSetting.value === 'true')
  };
};

const mapDispatchToProps: DispatchProps = { setHomePage };

export default connect(mapStateToProps, mapDispatchToProps)(HomePageSelect);

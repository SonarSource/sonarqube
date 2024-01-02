/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { DiscreetInteractiveIcon, HomeFillIcon, HomeIcon } from 'design-system';
import * as React from 'react';
import { setHomePage } from '../../api/users';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../helpers/l10n';
import { isSameHomePage } from '../../helpers/users';
import { HomePage, isLoggedIn } from '../../types/users';
import Tooltip from './Tooltip';

interface Props
  extends Pick<CurrentUserContextInterface, 'currentUser' | 'updateCurrentUserHomepage'> {
  className?: string;
  currentPage: HomePage;
}

export const DEFAULT_HOMEPAGE: HomePage = { type: 'PROJECTS' };

export class HomePageSelect extends React.PureComponent<Props> {
  async setCurrentUserHomepage(homepage: HomePage) {
    const { currentUser } = this.props;

    if (isLoggedIn(currentUser)) {
      await setHomePage(homepage);

      this.props.updateCurrentUserHomepage(homepage);
    }
  }

  handleClick = () => {
    this.setCurrentUserHomepage(this.props.currentPage);
  };

  handleReset = () => {
    this.setCurrentUserHomepage(DEFAULT_HOMEPAGE);
  };

  render() {
    const { className, currentPage, currentUser } = this.props;

    if (!isLoggedIn(currentUser)) {
      return null;
    }

    const { homepage } = currentUser;
    const isChecked = homepage !== undefined && isSameHomePage(homepage, currentPage);
    const isDefault = isChecked && isSameHomePage(currentPage, DEFAULT_HOMEPAGE);
    const tooltip = isChecked
      ? translate(isDefault ? 'homepage.current.is_default' : 'homepage.current')
      : translate('homepage.check');

    return (
      <Tooltip overlay={tooltip}>
        <DiscreetInteractiveIcon
          aria-label={tooltip}
          className={className}
          disabled={isDefault}
          Icon={isChecked ? HomeFillIcon : HomeIcon}
          onClick={isChecked ? this.handleReset : this.handleClick}
        />
      </Tooltip>
    );
  }
}

export default withCurrentUserContext(HomePageSelect);

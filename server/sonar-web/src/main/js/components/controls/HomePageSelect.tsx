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
import classNames from 'classnames';
import * as React from 'react';
import { setHomePage } from '../../api/users';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { ButtonLink } from '../../components/controls/buttons';
import Tooltip from '../../components/controls/Tooltip';
import HomeIcon from '../../components/icons/HomeIcon';
import { translate } from '../../helpers/l10n';
import { isSameHomePage } from '../../helpers/users';
import { HomePage, isLoggedIn } from '../../types/users';

interface Props
  extends Pick<CurrentUserContextInterface, 'currentUser' | 'updateCurrentUserHomepage'> {
  className?: string;
  currentPage: HomePage;
}

export const DEFAULT_HOMEPAGE: HomePage = { type: 'PROJECTS' };

export class HomePageSelect extends React.PureComponent<Props> {
  buttonNode?: HTMLElement | null;

  async setCurrentUserHomepage(homepage: HomePage) {
    const { currentUser } = this.props;

    if (currentUser && isLoggedIn(currentUser)) {
      await setHomePage(homepage);

      this.props.updateCurrentUserHomepage(homepage);

      if (this.buttonNode) {
        this.buttonNode.focus();
      }
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
        {isDefault ? (
          <span
            aria-label={tooltip}
            className={classNames('display-inline-block', className)}
            role="img"
          >
            <HomeIcon filled={isChecked} />
          </span>
        ) : (
          <ButtonLink
            aria-label={tooltip}
            className={classNames('link-no-underline', 'set-homepage-link', className)}
            onClick={isChecked ? this.handleReset : this.handleClick}
            innerRef={(node) => (this.buttonNode = node)}
          >
            <HomeIcon filled={isChecked} />
          </ButtonLink>
        )}
      </Tooltip>
    );
  }
}

export default withCurrentUserContext(HomePageSelect);

/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { connect } from 'react-redux';
import { ButtonLink } from '../../components/controls/buttons';
import Tooltip from '../../components/controls/Tooltip';
import HomeIcon from '../../components/icons/HomeIcon';
import { translate } from '../../helpers/l10n';
import { isLoggedIn } from '../../helpers/users';
import { getCurrentUser, Store } from '../../store/rootReducer';
import { setHomePage } from '../../store/users';
import { CurrentUser, HomePage } from '../../types/types';

interface StateProps {
  currentUser: CurrentUser;
}

interface DispatchProps {
  setHomePage: (homepage: HomePage) => void;
}

interface Props extends StateProps, DispatchProps {
  className?: string;
  currentPage: HomePage;
}

export const DEFAULT_HOMEPAGE: HomePage = { type: 'PROJECTS' };

export class HomePageSelect extends React.PureComponent<Props> {
  handleClick = () => {
    this.props.setHomePage(this.props.currentPage);
  };

  handleReset = () => {
    this.props.setHomePage(DEFAULT_HOMEPAGE);
  };

  render() {
    const { currentPage, currentUser } = this.props;

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
            className={classNames('display-inline-block', this.props.className)}>
            <HomeIcon filled={isChecked} />
          </span>
        ) : (
          <ButtonLink
            aria-label={tooltip}
            className={classNames('link-no-underline', 'set-homepage-link', this.props.className)}
            onClick={isChecked ? this.handleReset : this.handleClick}>
            <HomeIcon filled={isChecked} />
          </ButtonLink>
        )}
      </Tooltip>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  currentUser: getCurrentUser(state)
});

const mapDispatchToProps: DispatchProps = { setHomePage };

export default connect(mapStateToProps, mapDispatchToProps)(HomePageSelect);

function isSameHomePage(a: HomePage, b: HomePage) {
  return (
    a.type === b.type &&
    (a as any).branch === (b as any).branch &&
    (a as any).component === (b as any).component
  );
}

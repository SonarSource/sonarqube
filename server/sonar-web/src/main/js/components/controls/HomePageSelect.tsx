/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import HomeIcon from 'sonar-ui-common/components/icons/HomeIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isLoggedIn } from '../../helpers/users';
import { getCurrentUser, Store } from '../../store/rootReducer';
import { setHomePage } from '../../store/users';

interface StateProps {
  currentUser: T.CurrentUser;
}

interface DispatchProps {
  setHomePage: (homepage: T.HomePage) => void;
}

interface Props extends StateProps, DispatchProps {
  className?: string;
  currentPage: T.HomePage;
}

class HomePageSelect extends React.PureComponent<Props> {
  handleClick = () => {
    this.props.setHomePage(this.props.currentPage);
  };

  render() {
    const { currentPage, currentUser } = this.props;

    if (!isLoggedIn(currentUser)) {
      return null;
    }

    const { homepage } = currentUser;
    const checked = homepage !== undefined && isSameHomePage(homepage, currentPage);
    const tooltip = checked ? translate('homepage.current') : translate('homepage.check');

    return (
      <Tooltip overlay={tooltip}>
        {checked ? (
          <span
            aria-label={tooltip}
            className={classNames('display-inline-block', this.props.className)}>
            <HomeIcon filled={checked} />
          </span>
        ) : (
          <ButtonLink
            aria-label={tooltip}
            className={classNames('link-no-underline', 'set-homepage-link', this.props.className)}
            onClick={this.handleClick}>
            <HomeIcon filled={checked} />
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

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(HomePageSelect);

function isSameHomePage(a: T.HomePage, b: T.HomePage) {
  return (
    a.type === b.type &&
    (a as any).branch === (b as any).branch &&
    (a as any).component === (b as any).component &&
    (a as any).organization === (b as any).organization
  );
}

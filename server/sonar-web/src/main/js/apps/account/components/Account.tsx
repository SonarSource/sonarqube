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
import * as React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import handleRequiredAuthentication from 'sonar-ui-common/helpers/handleRequiredAuthentication';
import { translate } from 'sonar-ui-common/helpers/l10n';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { areThereCustomOrganizations, getCurrentUser, Store } from '../../../store/rootReducer';
import '../account.css';
import Nav from './Nav';
import UserCard from './UserCard';

interface Props {
  currentUser: T.CurrentUser;
  customOrganizations?: boolean;
}

export class Account extends React.PureComponent<Props> {
  componentDidMount() {
    if (!this.props.currentUser.isLoggedIn) {
      handleRequiredAuthentication();
    }
  }

  render() {
    const { currentUser, children } = this.props;

    if (!currentUser.isLoggedIn) {
      return null;
    }

    const title = translate('my_account.page');
    return (
      <div id="account-page">
        <Suggestions suggestions="account" />
        <Helmet defaultTitle={title} titleTemplate={'%s - ' + title} />
        <A11ySkipTarget anchor="account_main" />
        <header className="account-header">
          <div className="account-container clearfix">
            <UserCard user={currentUser as T.LoggedInUser} />
            <Nav customOrganizations={this.props.customOrganizations} />
          </div>
        </header>

        {children}
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state),
  customOrganizations: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(Account);

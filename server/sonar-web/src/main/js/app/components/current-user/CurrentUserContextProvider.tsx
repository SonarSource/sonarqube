/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { CurrentUser, HomePage, NoticeType } from '../../../types/users';
import { CurrentUserContext } from './CurrentUserContext';

interface Props {
  currentUser?: CurrentUser;
}

interface State {
  currentUser: CurrentUser;
}

export default class CurrentUserContextProvider extends React.PureComponent<
  React.PropsWithChildren<Props>,
  State
> {
  constructor(props: Props) {
    super(props);
    this.state = { currentUser: props.currentUser ?? { isLoggedIn: false, dismissedNotices: {} } };
  }

  updateCurrentUserHomepage = (homepage: HomePage) => {
    this.setState((prevState) => ({
      currentUser: { ...prevState.currentUser, homepage },
    }));
  };

  updateDismissedNotices = (key: NoticeType, value: boolean) => {
    this.setState((prevState) => ({
      currentUser: {
        ...prevState.currentUser,
        dismissedNotices: { ...prevState.currentUser.dismissedNotices, [key]: value },
      },
    }));
  };

  render() {
    return (
      <CurrentUserContext.Provider
        value={{
          currentUser: this.state.currentUser,
          updateCurrentUserHomepage: this.updateCurrentUserHomepage,
          updateDismissedNotices: this.updateDismissedNotices,
        }}
      >
        {this.props.children}
      </CurrentUserContext.Provider>
    );
  }
}

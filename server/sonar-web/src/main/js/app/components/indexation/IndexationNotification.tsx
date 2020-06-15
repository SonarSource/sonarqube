/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import withIndexationContext, {
  WithIndexationContextProps
} from '../../../components/hoc/withIndexationContext';
import { hasGlobalPermission, isLoggedIn } from '../../../helpers/users';
import './IndexationNotification.css';
import IndexationNotificationHelper from './IndexationNotificationHelper';
import IndexationNotificationRenderer from './IndexationNotificationRenderer';

interface Props extends WithIndexationContextProps {
  currentUser: T.CurrentUser;
}

interface State {
  progression?: IndexationProgression;
}

export enum IndexationProgression {
  InProgress,
  Completed
}

export class IndexationNotification extends React.PureComponent<Props, State> {
  state: State;
  isSystemAdmin = false;

  constructor(props: Props) {
    super(props);

    this.state = { progression: undefined };
    this.isSystemAdmin =
      isLoggedIn(this.props.currentUser) && hasGlobalPermission(this.props.currentUser, 'admin');
  }

  componentDidMount() {
    this.refreshNotification();
  }

  componentDidUpdate() {
    this.refreshNotification();
  }

  refreshNotification() {
    if (!this.props.indexationContext.status.isCompleted) {
      IndexationNotificationHelper.markInProgressNotificationAsDisplayed();
      this.setState({ progression: IndexationProgression.InProgress });
    } else if (IndexationNotificationHelper.shouldDisplayCompletedNotification()) {
      this.setState({ progression: IndexationProgression.Completed });
    }
  }

  handleDismissCompletedNotification = () => {
    IndexationNotificationHelper.markCompletedNotificationAsDisplayed();
    this.setState({ progression: undefined });
  };

  render() {
    const { progression } = this.state;
    const {
      indexationContext: {
        status: { percentCompleted }
      }
    } = this.props;

    if (progression === undefined) {
      return null;
    }

    return (
      <IndexationNotificationRenderer
        progression={progression}
        percentCompleted={percentCompleted ?? 0}
        onDismissCompletedNotification={this.handleDismissCompletedNotification}
        displayBackgroundTaskLink={this.isSystemAdmin}
      />
    );
  }
}

export default withCurrentUser(withIndexationContext(IndexationNotification));

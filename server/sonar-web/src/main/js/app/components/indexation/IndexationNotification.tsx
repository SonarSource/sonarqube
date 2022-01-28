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
import * as React from 'react';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import withIndexationContext, {
  WithIndexationContextProps
} from '../../../components/hoc/withIndexationContext';
import { hasGlobalPermission, isLoggedIn } from '../../../helpers/users';
import { IndexationNotificationType } from '../../../types/indexation';
import { Permissions } from '../../../types/permissions';
import { CurrentUser } from '../../../types/types';
import './IndexationNotification.css';
import IndexationNotificationHelper from './IndexationNotificationHelper';
import IndexationNotificationRenderer from './IndexationNotificationRenderer';

interface Props extends WithIndexationContextProps {
  currentUser: CurrentUser;
}

interface State {
  notificationType?: IndexationNotificationType;
}

export class IndexationNotification extends React.PureComponent<Props, State> {
  state: State = {};
  isSystemAdmin = false;

  constructor(props: Props) {
    super(props);

    this.isSystemAdmin =
      isLoggedIn(this.props.currentUser) &&
      hasGlobalPermission(this.props.currentUser, Permissions.Admin);
  }

  componentDidMount() {
    this.refreshNotification();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.indexationContext.status !== this.props.indexationContext.status) {
      this.refreshNotification();
    }
  }

  refreshNotification() {
    const { isCompleted, hasFailures } = this.props.indexationContext.status;

    if (!isCompleted) {
      IndexationNotificationHelper.markCompletedNotificationAsToDisplay();
      this.setState({
        notificationType: hasFailures
          ? IndexationNotificationType.InProgressWithFailure
          : IndexationNotificationType.InProgress
      });
    } else if (hasFailures) {
      this.setState({ notificationType: IndexationNotificationType.CompletedWithFailure });
    } else if (IndexationNotificationHelper.shouldDisplayCompletedNotification()) {
      this.setState({
        notificationType: IndexationNotificationType.Completed
      });
      IndexationNotificationHelper.markCompletedNotificationAsDisplayed();
    } else {
      this.setState({ notificationType: undefined });
    }
  }

  render() {
    const { notificationType } = this.state;
    const {
      indexationContext: {
        status: { percentCompleted }
      }
    } = this.props;

    if (notificationType === undefined) {
      return null;
    }

    return (
      <IndexationNotificationRenderer
        type={notificationType}
        percentCompleted={percentCompleted}
        isSystemAdmin={this.isSystemAdmin}
      />
    );
  }
}

export default withCurrentUser(withIndexationContext(IndexationNotification));

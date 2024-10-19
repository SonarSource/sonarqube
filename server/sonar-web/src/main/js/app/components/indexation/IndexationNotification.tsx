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
import * as React from 'react';
import withIndexationContext, {
  WithIndexationContextProps,
} from '../../../components/hoc/withIndexationContext';
import { hasGlobalPermission } from '../../../helpers/users';
import { IndexationNotificationType } from '../../../types/indexation';
import { Permissions } from '../../../types/permissions';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import withCurrentUserContext from '../current-user/withCurrentUserContext';
import IndexationNotificationHelper from './IndexationNotificationHelper';
import IndexationNotificationRenderer from './IndexationNotificationRenderer';

interface Props extends WithIndexationContextProps {
  currentUser: CurrentUser;
}

interface State {
  notificationType?: IndexationNotificationType;
}

const COMPLETED_NOTIFICATION_DISPLAY_DURATION = 5000;

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
          : IndexationNotificationType.InProgress,
      });
    } else if (hasFailures) {
      this.setState({ notificationType: IndexationNotificationType.CompletedWithFailure });
    } else if (IndexationNotificationHelper.shouldDisplayCompletedNotification()) {
      this.setState({
        notificationType: IndexationNotificationType.Completed,
      });

      IndexationNotificationHelper.markCompletedNotificationAsDisplayed();

      // Hide after some time
      setTimeout(() => {
        this.refreshNotification();
      }, COMPLETED_NOTIFICATION_DISPLAY_DURATION);
    } else {
      this.setState({ notificationType: undefined });
    }
  }

  render() {
    const { notificationType } = this.state;

    const {
      indexationContext: {
        status: { completedCount, total },
      },
    } = this.props;

    return !this.isSystemAdmin ? null : (
      <IndexationNotificationRenderer
        completedCount={completedCount}
        total={total}
        type={notificationType}
      />
    );
  }
}

export default withCurrentUserContext(withIndexationContext(IndexationNotification));

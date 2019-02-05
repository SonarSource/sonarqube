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
import ClearIcon from '../../../components/icons-components/ClearIcon';
import NotificationIcon from '../../../components/icons-components/NotificationIcon';
import { sonarcloudBlack500 } from '../../theme';
import { PrismicFeatureNews } from '../../../api/news';
import { differenceInSeconds, parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import './notifications.css';

interface Props {
  lastNews: PrismicFeatureNews;
  notificationsLastReadDate?: Date;
  notificationsOptOut?: boolean;
  onClick: () => void;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
}

export default class NavLatestNotification extends React.PureComponent<Props> {
  mounted = false;

  checkHasUnread = () => {
    const { notificationsLastReadDate, lastNews } = this.props;
    return (
      !notificationsLastReadDate ||
      differenceInSeconds(parseDate(lastNews.publicationDate), notificationsLastReadDate) > 0
    );
  };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClick();
  };

  handleDismiss = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();

    this.props.setCurrentUserSetting({
      key: 'notifications.readDate',
      value: Date.now().toString()
    });
  };

  render() {
    const { notificationsOptOut, lastNews } = this.props;
    const hasUnread = this.checkHasUnread();
    const showNotifications = Boolean(!notificationsOptOut && lastNews && hasUnread);
    return (
      <>
        {showNotifications && (
          <>
            <li className="navbar-latest-notification" onClick={this.props.onClick}>
              <div className="navbar-latest-notification-wrapper">
                <span className="badge badge-new">{translate('new')}</span>
                <span className="label">{lastNews.notification}</span>
              </div>
            </li>
            <li className="navbar-latest-notification-dismiss">
              <a className="navbar-icon" href="#" onClick={this.handleDismiss}>
                <ClearIcon fill={sonarcloudBlack500} size={10} />
              </a>
            </li>
          </>
        )}
        <li>
          <a className="navbar-icon" href="#" onClick={this.handleClick}>
            <NotificationIcon hasUnread={hasUnread && !notificationsOptOut} />
          </a>
        </li>
      </>
    );
  }
}

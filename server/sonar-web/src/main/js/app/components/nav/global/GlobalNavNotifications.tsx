/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { connect } from 'react-redux';
import ClearIcon from '../../../../components/icons-components/ClearIcon';
import NotificationIcon from '../../../../components/icons-components/NotificationIcon';
import { sonarcloudBlack500 } from '../../../theme';
import {
  fetchPrismicRefs,
  fetchPrismicFeatureNews,
  PrismicFeatureNews
} from '../../../../api/news';
import { differenceInSeconds, parseDate } from '../../../../helpers/dates';
import { translate } from '../../../../helpers/l10n';
import { fetchCurrentUserSettings, setCurrentUserSetting } from '../../../../store/users';
import {
  getGlobalSettingValue,
  getCurrentUserSettings,
  Store
} from '../../../../store/rootReducer';

interface Props {
  accessToken?: string;
  fetchCurrentUserSettings: () => void;
  notificationsLastReadDate?: Date;
  notificationsOptOut?: boolean;
  setCurrentUserSetting: (setting: T.CurrentUserSettingData) => void;
}

interface State {
  news: PrismicFeatureNews[];
  ready: boolean;
}

export class GlobalNavNotifications extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { news: [], ready: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchPrismicFeatureNews();
    this.props.fetchCurrentUserSettings();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkHasUnread = () => {
    const lastNews = this.state.news[0];
    if (!lastNews) {
      return false;
    }

    const { notificationsLastReadDate } = this.props;
    return (
      !notificationsLastReadDate ||
      differenceInSeconds(parseDate(lastNews.publicationDate), notificationsLastReadDate) > 0
    );
  };

  fetchPrismicFeatureNews = () => {
    const { accessToken } = this.props;
    if (accessToken) {
      fetchPrismicRefs()
        .then(({ ref }) => fetchPrismicFeatureNews({ accessToken, ref, ps: 10 }))
        .then(
          news => {
            if (this.mounted && news) {
              this.setState({ ready: true, news });
            }
          },
          () => {}
        );
    }
  };

  handleDismiss = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.setCurrentUserSetting({
      key: 'notificationsReadDate',
      value: new Date().toISOString()
    });
  };

  render() {
    if (!this.state.ready) {
      return null;
    }

    const { notificationsOptOut } = this.props;
    const lastNews = this.state.news[0];
    const hasUnread = this.checkHasUnread();
    const showNotifications = Boolean(!notificationsOptOut && lastNews && hasUnread);
    return (
      <>
        {showNotifications && (
          <li className="navbar-latest-notification">
            <div className="navbar-latest-notification-wrapper">
              <span className="badge">{translate('new')}</span>
              <span className="label">{lastNews.notification}</span>
              <a className="navbar-icon" href="#" onClick={this.handleDismiss}>
                <ClearIcon fill={sonarcloudBlack500} size={10} />
              </a>
            </div>
          </li>
        )}
        <li>
          <a className="navbar-icon">
            <NotificationIcon hasUnread={hasUnread && !notificationsOptOut} />
          </a>
        </li>
      </>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const accessToken = getGlobalSettingValue(state, 'sonar.prismic.accessToken');
  const userSettings = getCurrentUserSettings(state);
  return {
    accessToken: accessToken && accessToken.value,
    notificationsLastReadDate: userSettings.notificationsReadDate
      ? parseDate(userSettings.notificationsReadDate)
      : undefined,
    notificationsOptOut: userSettings.notificationsReadDate === 'true'
  };
};

const mapDispatchToProps = { fetchCurrentUserSettings, setCurrentUserSetting };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(GlobalNavNotifications);

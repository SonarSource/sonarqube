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
import { connect } from 'react-redux';
import GlobalNavBranding, { SonarCloudNavBranding } from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavExplore from './GlobalNavExplore';
import GlobalNavUserContainer from './GlobalNavUserContainer';
import Search from '../../search/Search';
import EmbedDocsPopupHelper from '../../embed-docs-modal/EmbedDocsPopupHelper';
import * as theme from '../../../theme';
import NavBar from '../../../../components/nav/NavBar';
import { lazyLoad } from '../../../../components/lazyLoad';
import {
  fetchPrismicRefs,
  fetchPrismicFeatureNews,
  PrismicFeatureNews
} from '../../../../api/news';
import {
  getCurrentUser,
  getCurrentUserSetting,
  getAppState,
  getGlobalSettingValue,
  Store
} from '../../../../store/rootReducer';
import { isSonarCloud } from '../../../../helpers/system';
import { isLoggedIn } from '../../../../helpers/users';
import { OnboardingContext } from '../../OnboardingContext';
import { setCurrentUserSetting } from '../../../../store/users';
import { parseDate } from '../../../../helpers/dates';
import './GlobalNav.css';

const GlobalNavPlus = lazyLoad(() => import('./GlobalNavPlus'), 'GlobalNavPlus');
const NotificationsSidebar = lazyLoad(
  () => import('../../notifications/NotificationsSidebar'),
  'NotificationsSidebar'
);
const NavLatestNotification = lazyLoad(
  () => import('../../notifications/NavLatestNotification'),
  'NavLatestNotification'
);

interface Props {
  accessToken?: string;
  appState: Pick<T.AppState, 'canAdmin' | 'globalPages' | 'organizationsEnabled' | 'qualifiers'>;
  currentUser: T.CurrentUser;
  location: { pathname: string };
  notificationsLastReadDate?: Date;
  notificationsOptOut?: boolean;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
}

interface State {
  notificationSidebar?: boolean;
  loadingNews: boolean;
  loadingMoreNews: boolean;
  news: PrismicFeatureNews[];
  newsPaging?: T.Paging;
  newsRef?: string;
}

const PAGE_SIZE = 5;

export class GlobalNav extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loadingNews: false,
    loadingMoreNews: false,
    news: [],
    notificationSidebar: false
  };

  componentDidMount() {
    this.mounted = true;
    if (isSonarCloud()) {
      this.fetchFeatureNews();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchFeatureNews = () => {
    const { accessToken } = this.props;
    if (accessToken) {
      this.setState({ loadingNews: true });
      fetchPrismicRefs()
        .then(({ ref }) => {
          if (this.mounted) {
            this.setState({ newsRef: ref });
          }
          return ref;
        })
        .then(ref => fetchPrismicFeatureNews({ accessToken, ref, ps: PAGE_SIZE }))
        .then(
          ({ news, paging }) => {
            if (this.mounted) {
              this.setState({
                loadingNews: false,
                news,
                newsPaging: paging
              });
            }
          },
          () => {
            if (this.mounted) {
              this.setState({ loadingNews: false });
            }
          }
        );
    }
  };

  fetchMoreFeatureNews = () => {
    const { accessToken } = this.props;
    const { newsPaging, newsRef } = this.state;
    if (accessToken && newsPaging && newsRef) {
      this.setState({ loadingMoreNews: true });
      fetchPrismicFeatureNews({
        accessToken,
        ref: newsRef,
        p: newsPaging.pageIndex + 1,
        ps: PAGE_SIZE
      }).then(
        ({ news, paging }) => {
          if (this.mounted) {
            this.setState(state => ({
              loadingMoreNews: false,
              news: [...state.news, ...news],
              newsPaging: paging
            }));
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loadingMoreNews: false });
          }
        }
      );
    }
  };

  handleOpenNotificationSidebar = () => {
    this.setState({ notificationSidebar: true });
    this.fetchFeatureNews();
  };

  handleCloseNotificationSidebar = () => {
    this.setState({ notificationSidebar: false });
    const lastNews = this.state.news[0];
    const readDate = lastNews ? parseDate(lastNews.publicationDate).getTime() : Date.now();
    this.props.setCurrentUserSetting({ key: 'notifications.readDate', value: readDate.toString() });
  };

  render() {
    const { appState, currentUser } = this.props;
    const { news } = this.state;
    return (
      <NavBar className="navbar-global" height={theme.globalNavHeightRaw} id="global-navigation">
        {isSonarCloud() ? <SonarCloudNavBranding /> : <GlobalNavBranding />}

        <GlobalNavMenu {...this.props} />

        <ul className="global-navbar-menu global-navbar-menu-right">
          {isSonarCloud() && isLoggedIn(currentUser) && news.length > 0 && (
            <NavLatestNotification
              lastNews={news[0]}
              notificationsLastReadDate={this.props.notificationsLastReadDate}
              notificationsOptOut={this.props.notificationsOptOut}
              onClick={this.handleOpenNotificationSidebar}
              setCurrentUserSetting={this.props.setCurrentUserSetting}
            />
          )}
          {isSonarCloud() && <GlobalNavExplore location={this.props.location} />}
          <EmbedDocsPopupHelper />
          <Search appState={appState} currentUser={currentUser} />
          {isLoggedIn(currentUser) && (
            <OnboardingContext.Consumer data-test="global-nav-plus">
              {openProjectOnboarding => (
                <GlobalNavPlus
                  appState={appState}
                  currentUser={currentUser}
                  openProjectOnboarding={openProjectOnboarding}
                />
              )}
            </OnboardingContext.Consumer>
          )}
          <GlobalNavUserContainer appState={appState} currentUser={currentUser} />
        </ul>
        {isSonarCloud() && isLoggedIn(currentUser) && this.state.notificationSidebar && (
          <NotificationsSidebar
            fetchMoreFeatureNews={this.fetchMoreFeatureNews}
            loading={this.state.loadingNews}
            loadingMore={this.state.loadingMoreNews}
            news={news}
            notificationsLastReadDate={this.props.notificationsLastReadDate}
            onClose={this.handleCloseNotificationSidebar}
            paging={this.state.newsPaging}
          />
        )}
      </NavBar>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const accessToken = getGlobalSettingValue(state, 'sonar.prismic.accessToken');
  const notificationsLastReadDate = getCurrentUserSetting(state, 'notifications.readDate');
  const notificationsOptOut = getCurrentUserSetting(state, 'notifications.optOut') === 'true';

  return {
    currentUser: getCurrentUser(state),
    appState: getAppState(state),
    accessToken: accessToken && accessToken.value,
    notificationsLastReadDate: notificationsLastReadDate
      ? parseDate(Number(notificationsLastReadDate))
      : undefined,
    notificationsOptOut
  };
};

const mapDispatchToProps = {
  setCurrentUserSetting
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(GlobalNav);

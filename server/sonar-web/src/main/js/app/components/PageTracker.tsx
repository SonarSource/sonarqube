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
import { Location, withRouter } from '../../components/hoc/withRouter';
import { gtm } from '../../helpers/analytics';
import { installScript } from '../../helpers/extensions';
import { getWebAnalyticsPageHandlerFromCache } from '../../helpers/extensionsHandler';
import { getInstance } from '../../helpers/system';
import { getAppState, getGlobalSettingValue, Store } from '../../store/rootReducer';

interface Props {
  location: Location;
  trackingIdGTM?: string;
  webAnalytics?: string;
}

interface State {
  lastLocation?: string;
}

export class PageTracker extends React.Component<Props, State> {
  state: State = {};

  componentDidMount() {
    const { trackingIdGTM, webAnalytics } = this.props;

    if (webAnalytics && !getWebAnalyticsPageHandlerFromCache()) {
      installScript(webAnalytics, 'head');
    }

    if (trackingIdGTM) {
      gtm(trackingIdGTM);
    }
  }

  trackPage = () => {
    const { location, trackingIdGTM } = this.props;
    const { lastLocation } = this.state;
    const { dataLayer } = window as any;
    const locationChanged = location.pathname !== lastLocation;
    const webAnalyticsPageChange = getWebAnalyticsPageHandlerFromCache();

    if (webAnalyticsPageChange && locationChanged) {
      this.setState({ lastLocation: location.pathname });
      setTimeout(() => webAnalyticsPageChange(location.pathname), 500);
    } else if (dataLayer && dataLayer.push && trackingIdGTM && location.pathname !== '/') {
      this.setState({ lastLocation: location.pathname });
      setTimeout(() => dataLayer.push({ event: 'render-end' }), 500);
    }
  };

  render() {
    const { trackingIdGTM, webAnalytics } = this.props;

    return (
      <Helmet
        defaultTitle={getInstance()}
        onChangeClientState={trackingIdGTM || webAnalytics ? this.trackPage : undefined}>
        {this.props.children}
      </Helmet>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const trackingIdGTM = getGlobalSettingValue(state, 'sonar.analytics.gtm.trackingId');
  return {
    trackingIdGTM: trackingIdGTM && trackingIdGTM.value,
    webAnalytics: getAppState(state).webAnalyticsJsPath
  };
};

export default withRouter(connect(mapStateToProps)(PageTracker));

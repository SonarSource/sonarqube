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
import * as GoogleAnalytics from 'react-ga';
import { withRouter, WithRouterProps } from 'react-router';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import { getGlobalSettingValue, Store } from '../../store/rootReducer';
import { gtm } from '../../helpers/analytics';
import { getInstance } from '../../helpers/system';

interface StateProps {
  trackingIdGA?: string;
  trackingIdGTM?: string;
}

type Props = WithRouterProps & StateProps;

interface State {
  lastLocation?: string;
}

export class PageTracker extends React.Component<Props, State> {
  state: State = {
    lastLocation: undefined
  };

  componentDidMount() {
    const { trackingIdGA, trackingIdGTM } = this.props;

    if (trackingIdGA) {
      GoogleAnalytics.initialize(trackingIdGA);
    }

    if (trackingIdGTM) {
      gtm(trackingIdGTM);
    }
  }

  trackPage = () => {
    const { location, trackingIdGA, trackingIdGTM } = this.props;
    const { lastLocation } = this.state;

    if (location.pathname !== lastLocation) {
      if (trackingIdGA) {
        // More info on the "title and page not in sync" issue: https://github.com/nfl/react-helmet/issues/189
        setTimeout(() => GoogleAnalytics.pageview(location.pathname), 500);
      }

      if (trackingIdGTM && location.pathname !== '/') {
        setTimeout(() => {
          const { dataLayer } = window as any;
          if (dataLayer && dataLayer.push) {
            dataLayer.push({ event: 'render-end' });
          }
        }, 500);
      }

      this.setState({
        lastLocation: location.pathname
      });
    }
  };

  render() {
    const { trackingIdGA, trackingIdGTM } = this.props;
    const tracking = {
      ...((trackingIdGA || trackingIdGTM) && { onChangeClientState: this.trackPage })
    };

    return (
      <Helmet defaultTitle={getInstance()} {...tracking}>
        {this.props.children}
      </Helmet>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const trackingIdGA = getGlobalSettingValue(state, 'sonar.analytics.ga.trackingId');
  const trackingIdGTM = getGlobalSettingValue(state, 'sonar.analytics.gtm.trackingId');

  return {
    trackingIdGA: trackingIdGA && trackingIdGA.value,
    trackingIdGTM: trackingIdGTM && trackingIdGTM.value
  };
};

export default withRouter(connect(mapStateToProps)(PageTracker));

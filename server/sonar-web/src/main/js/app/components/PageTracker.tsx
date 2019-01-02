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
import { getGlobalSettingValue, Store } from '../../store/rootReducer';

interface StateProps {
  trackingId?: string;
}

type Props = WithRouterProps & StateProps;

export class PageTracker extends React.PureComponent<Props> {
  componentDidMount() {
    if (this.props.trackingId) {
      GoogleAnalytics.initialize(this.props.trackingId);
      this.trackPage();
    }
  }

  componentDidUpdate(prevProps: Props) {
    const currentPage = this.props.location.pathname;
    const prevPage = prevProps.location.pathname;

    if (currentPage !== prevPage) {
      this.trackPage();
    }
  }

  trackPage = () => {
    const { location, trackingId } = this.props;
    if (trackingId) {
      // More info on the "title and page not in sync" issue: https://github.com/nfl/react-helmet/issues/189
      setTimeout(() => GoogleAnalytics.pageview(location.pathname), 500);
    }
  };

  render() {
    return null;
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const trackingId = getGlobalSettingValue(state, 'sonar.analytics.trackingId');
  return {
    trackingId: trackingId && trackingId.value
  };
};

export default withRouter(connect(mapStateToProps)(PageTracker));

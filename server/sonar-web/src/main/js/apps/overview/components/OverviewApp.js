/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

import OverviewMain from './OverviewMain';
import { getMetrics } from '../../../api/metrics';

export default class OverviewApp extends React.Component {
  state = {};

  componentDidMount () {
    this.mounted = true;
    document.querySelector('html').classList.add('dashboard-page');
    this.requestMetrics();
  }

  componentWillUnmount () {
    this.mounted = false;
    document.querySelector('html').classList.delete('dashboard-page');
  }

  requestMetrics () {
    return getMetrics().then(metrics => {
      if (this.mounted) {
        this.setState({ metrics });
      }
    });
  }

  renderLoading () {
    return (
        <div className="text-center">
          <i className="spinner spinner-margin"/>
        </div>
    );
  }

  render () {
    if (!this.state.metrics) {
      return this.renderLoading();
    }

    return <OverviewMain {...this.props} metrics={this.state.metrics}/>;
  }
}

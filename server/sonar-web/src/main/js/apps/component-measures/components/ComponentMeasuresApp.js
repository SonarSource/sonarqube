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

import Spinner from './Spinner';
import { getMetrics } from '../../../api/metrics';

export default class ComponentMeasuresApp extends React.Component {
  state = {
    fetching: true,
    metrics: []
  };

  getChildContext () {
    return {
      component: this.props.component,
      metrics: this.state.metrics
    };
  }

  componentDidMount () {
    this.mounted = true;
    this.fetchMetrics();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchMetrics () {
    getMetrics().then(metrics => {
      if (this.mounted) {
        this.setState({ metrics, fetching: false });
      }
    });
  }

  render () {
    const { fetching, metrics } = this.state;

    if (fetching) {
      return <Spinner/>;
    }

    const child = React.cloneElement(this.props.children, { metrics });

    return (
        <div id="component-measures">
          {child}
        </div>
    );
  }
}

ComponentMeasuresApp.childContextTypes = {
  component: React.PropTypes.object,
  metrics: React.PropTypes.array
};

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
// @flow
import React from 'react';
import MeasureOverview from './MeasureOverview';
import { getComponentShow } from '../../../api/components';
import { getProjectUrl } from '../../../helpers/urls';
import { isViewType } from '../utils';
/*:: import type { Component, Period, Query } from '../types'; */
/*:: import type { RawQuery } from '../../../helpers/query'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {|
  branch?: string,
  className?: string,
  rootComponent: Component,
  currentUser: { isLoggedIn: boolean },
  domain: string,
  leakPeriod: Period,
  metrics: { [string]: Metric },
  router: {
    push: ({ pathname: string, query?: RawQuery }) => void
  },
  selected: ?string,
  updateQuery: Query => void
|}; */

/*:: type State = {
  component: ?Component,
  loading: {
    component: boolean,
    bubbles: boolean
  }
}; */

export default class MeasureOverviewContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    component: null,
    loading: {
      component: false,
      bubbles: false
    }
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent(this.props);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    const { component } = this.state;
    const componentChanged =
      !component ||
      nextProps.rootComponent.key !== component.key ||
      nextProps.selected !== component.key;
    if (componentChanged || nextProps.domain !== this.props.domain) {
      this.fetchComponent(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponent = ({ branch, rootComponent, selected } /*: Props */) => {
    if (!selected || rootComponent.key === selected) {
      this.setState({ component: rootComponent });
      this.updateLoading({ component: false });
      return;
    }
    this.updateLoading({ component: true });
    getComponentShow(selected, branch).then(
      ({ component }) => {
        if (this.mounted) {
          this.setState({ component });
          this.updateLoading({ component: false });
        }
      },
      () => this.updateLoading({ component: false })
    );
  };

  updateLoading = (loading /*: { [string]: boolean } */) => {
    if (this.mounted) {
      this.setState(state => ({ loading: { ...state.loading, ...loading } }));
    }
  };

  updateSelected = (component /*: string */) => {
    if (this.state.component && isViewType(this.state.component)) {
      this.props.router.push(getProjectUrl(component));
    } else {
      this.props.updateQuery({
        selected: component !== this.props.rootComponent.key ? component : null
      });
    }
  };

  render() {
    if (!this.state.component) {
      return null;
    }

    return (
      <MeasureOverview
        branch={this.props.branch}
        className={this.props.className}
        component={this.state.component}
        currentUser={this.props.currentUser}
        domain={this.props.domain}
        loading={this.state.loading.component || this.state.loading.bubbles}
        leakPeriod={this.props.leakPeriod}
        metrics={this.props.metrics}
        rootComponent={this.props.rootComponent}
        updateLoading={this.updateLoading}
        updateSelected={this.updateSelected}
      />
    );
  }
}

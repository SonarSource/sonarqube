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
import { InjectedRouter } from 'react-router';
import MeasureOverview from './MeasureOverview';
import { getComponentShow } from '../../../api/components';
import { getProjectUrl } from '../../../helpers/urls';
import { isViewType, Query } from '../utils';
import { getBranchLikeQuery } from '../../../helpers/branches';
import { BranchLike, ComponentMeasure, CurrentUser, Metric, Period } from '../../../app/types';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  currentUser: CurrentUser;
  domain: string;
  leakPeriod?: Period;
  metrics: { [metric: string]: Metric };
  rootComponent: ComponentMeasure;
  router: InjectedRouter;
  selected?: string;
  updateQuery: (query: Partial<Query>) => void;
}

interface LoadingState {
  bubbles: boolean;
  component: boolean;
}

interface State {
  component?: ComponentMeasure;
  loading: LoadingState;
}

export default class MeasureOverviewContainer extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: { bubbles: false, component: false }
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
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

  fetchComponent = ({ branchLike, rootComponent, selected }: Props) => {
    if (!selected || rootComponent.key === selected) {
      this.setState({ component: rootComponent });
      this.updateLoading({ component: false });
      return;
    }
    this.updateLoading({ component: true });
    getComponentShow({ component: selected, ...getBranchLikeQuery(branchLike) }).then(
      ({ component }) => {
        if (this.mounted) {
          this.setState({ component });
          this.updateLoading({ component: false });
        }
      },
      () => this.updateLoading({ component: false })
    );
  };

  updateLoading = (loading: Partial<LoadingState>) => {
    if (this.mounted) {
      this.setState(state => ({ loading: { ...state.loading, ...loading } }));
    }
  };

  updateSelected = (component: string) => {
    if (this.state.component && isViewType(this.state.component)) {
      this.props.router.push(getProjectUrl(component));
    } else {
      this.props.updateQuery({
        selected: component !== this.props.rootComponent.key ? component : undefined
      });
    }
  };

  render() {
    if (!this.state.component) {
      return null;
    }

    return (
      <MeasureOverview
        branchLike={this.props.branchLike}
        className={this.props.className}
        component={this.state.component}
        currentUser={this.props.currentUser}
        domain={this.props.domain}
        leakPeriod={this.props.leakPeriod}
        loading={this.state.loading.component || this.state.loading.bubbles}
        metrics={this.props.metrics}
        rootComponent={this.props.rootComponent}
        updateLoading={this.updateLoading}
        updateSelected={this.updateSelected}
      />
    );
  }
}

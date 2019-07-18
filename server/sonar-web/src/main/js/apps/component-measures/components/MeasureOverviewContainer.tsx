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
import { InjectedRouter } from 'react-router';
import { getComponentShow } from '../../../api/components';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';
import { getProjectUrl } from '../../../helpers/urls';
import { isViewType, Query } from '../utils';
import MeasureOverview from './MeasureOverview';

interface Props {
  branchLike?: T.BranchLike;
  className?: string;
  domain: string;
  leakPeriod?: T.Period;
  metrics: T.Dict<T.Metric>;
  onIssueChange?: (issue: T.Issue) => void;
  rootComponent: T.ComponentMeasure;
  router: InjectedRouter;
  selected?: string;
  updateQuery: (query: Partial<Query>) => void;
}

interface LoadingState {
  bubbles: boolean;
  component: boolean;
}

interface State {
  component?: T.ComponentMeasure;
  loading: LoadingState;
}

export default class MeasureOverviewContainer extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: { bubbles: false, component: false }
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentDidUpdate(prevProps: Props) {
    const prevComponentKey = prevProps.selected || prevProps.rootComponent.key;
    const componentKey = this.props.selected || this.props.rootComponent.key;
    if (
      prevComponentKey !== componentKey ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.domain !== this.props.domain
    ) {
      this.fetchComponent();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponent = () => {
    const { branchLike, rootComponent, selected } = this.props;
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
        domain={this.props.domain}
        leakPeriod={this.props.leakPeriod}
        loading={this.state.loading.component || this.state.loading.bubbles}
        metrics={this.props.metrics}
        onIssueChange={this.props.onIssueChange}
        rootComponent={this.props.rootComponent}
        updateLoading={this.updateLoading}
        updateSelected={this.updateSelected}
      />
    );
  }
}

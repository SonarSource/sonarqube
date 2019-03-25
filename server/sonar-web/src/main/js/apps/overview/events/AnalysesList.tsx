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
import { Link } from 'react-router';
import Analysis from './Analysis';
import { getProjectActivity } from '../../../api/projectActivity';
import PreviewGraph from '../../../components/preview-graph/PreviewGraph';
import { translate } from '../../../helpers/l10n';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';
import { getActivityUrl } from '../../../helpers/urls';

interface Props {
  branchLike?: T.BranchLike;
  component: T.Component;
  history?: {
    [metric: string]: Array<{ date: Date; value?: string }>;
  };
  metrics: T.Dict<T.Metric>;
  qualifier: string;
}

interface State {
  analyses: T.Analysis[];
  loading: boolean;
}

const PAGE_SIZE = 3;

export default class AnalysesList extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { analyses: [], loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.component.key !== this.props.component.key ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike)
    ) {
      this.fetchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getTopLevelComponent = () => {
    const { component } = this.props;
    let current = component.breadcrumbs.length - 1;
    while (
      current > 0 &&
      !['TRK', 'VW', 'APP'].includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }
    return component.breadcrumbs[current].key;
  };

  fetchData = () => {
    this.setState({ loading: true });

    getProjectActivity({
      ...getBranchLikeQuery(this.props.branchLike),
      project: this.getTopLevelComponent(),
      ps: PAGE_SIZE
    }).then(
      ({ analyses }) => {
        if (this.mounted) {
          this.setState({ analyses, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  renderList(analyses: T.Analysis[]) {
    if (!analyses.length) {
      return <p className="spacer-top note">{translate('no_results')}</p>;
    }

    return (
      <ul className="spacer-top">
        {analyses.map(analysis => (
          <Analysis analysis={analysis} key={analysis.key} qualifier={this.props.qualifier} />
        ))}
      </ul>
    );
  }

  render() {
    const { analyses, loading } = this.state;

    if (loading) {
      return null;
    }

    return (
      <div className="overview-meta-card">
        <h4 className="overview-meta-header">
          {translate('overview.project_activity', this.props.component.qualifier)}
        </h4>

        <PreviewGraph
          branchLike={this.props.branchLike}
          history={this.props.history}
          metrics={this.props.metrics}
          project={this.props.component.key}
        />

        {this.renderList(analyses)}

        <div className="spacer-top small">
          <Link to={getActivityUrl(this.props.component.key, this.props.branchLike)}>
            {translate('show_more')}
          </Link>
        </div>
      </div>
    );
  }
}

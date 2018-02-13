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
import { Link } from 'react-router';
import Analysis from './Analysis';
import { getProjectActivity, Analysis as IAnalysis } from '../../../api/projectActivity';
import PreviewGraph from '../../../components/preview-graph/PreviewGraph';
import { translate } from '../../../helpers/l10n';
import { Metric, Component } from '../../../app/types';
import { History } from '../../../api/time-machine';

interface Props {
  branch?: string;
  component: Component;
  history?: History;
  metrics: { [key: string]: Metric };
  qualifier: string;
}

interface State {
  analyses: IAnalysis[];
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
    if (prevProps.component !== this.props.component) {
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
      branch: this.props.branch,
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

  renderList(analyses: IAnalysis[]) {
    if (!analyses.length) {
      return <p className="spacer-top note">{translate('no_results')}</p>;
    }

    return (
      <ul className="spacer-top">
        {analyses.map(analysis => (
          <Analysis key={analysis.key} analysis={analysis} qualifier={this.props.qualifier} />
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
          branch={this.props.branch}
          history={this.props.history}
          project={this.props.component.key}
          metrics={this.props.metrics}
        />

        {this.renderList(analyses)}

        <div className="spacer-top small">
          <Link
            to={{
              pathname: '/project/activity',
              query: { id: this.props.component.key, branch: this.props.branch }
            }}>
            {translate('show_more')}
          </Link>
        </div>
      </div>
    );
  }
}

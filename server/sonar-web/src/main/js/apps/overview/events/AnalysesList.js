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
import { Link } from 'react-router';
import Analysis from './Analysis';
import { getAllMetrics } from '../../../api/metrics';
import { getProjectActivity } from '../../../api/projectActivity';
import PreviewGraph from '../../../components/preview-graph/PreviewGraph';
import { translate } from '../../../helpers/l10n';
/*:: import type { Analysis as AnalysisType } from '../../projectActivity/types'; */
/*:: import type { History, Metric } from '../types'; */

/*::
type Props = {
  branch?: string,
  component: Object,
  history: ?History,
  qualifier: string,
  router: { push: ({ pathname: string, query?: {} }) => void }
};
*/

/*::
type State = {
  analyses: Array<AnalysisType>,
  loading: boolean,
  metrics: Array<Metric>
};
*/

const PAGE_SIZE = 3;

export default class AnalysesList extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = { analyses: [], loading: true, metrics: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps /*: Props */) {
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

  fetchData() {
    this.setState({ loading: true });
    Promise.all([
      getProjectActivity({
        branch: this.props.branch,
        project: this.getTopLevelComponent(),
        ps: PAGE_SIZE
      }),
      getAllMetrics()
    ]).then(
      response => {
        if (this.mounted) {
          this.setState({
            analyses: response[0].analyses,
            metrics: response[1],
            loading: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  renderList(analyses /*: Array<AnalysisType> */) {
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
        <h4 className="overview-meta-header">{translate('project_activity.page')}</h4>

        <PreviewGraph
          branch={this.props.branch}
          history={this.props.history}
          project={this.props.component.key}
          metrics={this.state.metrics}
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

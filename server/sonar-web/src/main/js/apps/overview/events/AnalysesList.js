/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import PreviewGraph from './PreviewGraph';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { getMetrics } from '../../../api/metrics';
import { getProjectActivity } from '../../../api/projectActivity';
import { translate } from '../../../helpers/l10n';
import type { Analysis as AnalysisType } from '../../projectActivity/types';
import type { History, Metric } from '../types';

type Props = {
  history: History,
  project: string
};

type State = {
  analyses: Array<AnalysisType>,
  loading: boolean,
  metrics: Array<Metric>
};

const PAGE_SIZE = 5;

export default class AnalysesList extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = { analyses: [], loading: true, metrics: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.project !== this.props.project) {
      this.fetchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData() {
    this.setState({ loading: true });
    Promise.all([
      getProjectActivity({ project: this.props.project, ps: PAGE_SIZE }),
      getMetrics()
    ]).then(response => {
      if (this.mounted) {
        this.setState({ analyses: response[0].analyses, metrics: response[1], loading: false });
      }
    }, throwGlobalError);
  }

  renderList(analyses: Array<AnalysisType>) {
    if (!analyses.length) {
      return (
        <p className="spacer-top note">
          {translate('no_results')}
        </p>
      );
    }

    return (
      <ul className="spacer-top">
        {analyses.map(analysis => <Analysis key={analysis.key} analysis={analysis} />)}
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
          {translate('project_activity.page')}
        </h4>

        <PreviewGraph
          history={this.props.history}
          project={this.props.project}
          metrics={this.state.metrics}
        />

        {this.renderList(analyses)}

        <div className="spacer-top small">
          <Link to={{ pathname: '/project/activity', query: { id: this.props.project } }}>
            {translate('show_more')}
          </Link>
        </div>
      </div>
    );
  }
}

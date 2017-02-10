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
import { connect } from 'react-redux';
import Analysis from './Analysis';
import { translate } from '../../../helpers/l10n';
import { fetchRecentProjectActivity } from '../actions';
import { getProjectActivity } from '../../../store/rootReducer';
import { getAnalyses } from '../../../store/projectActivity/duck';

type Props = {
  analyses?: Array<*>,
  project: string,
  fetchRecentProjectActivity: (project: string) => Promise<*>
};

class AnalysesList extends React.Component {
  mounted: boolean;
  props: Props;

  state = {
    loading: true
  };

  componentDidMount () {
    this.mounted = true;
    this.fetchData();
  }

  componentDidUpdate (prevProps: Props) {
    if (prevProps.project !== this.props.project) {
      this.fetchData();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchData () {
    this.setState({ loading: true });
    this.props.fetchRecentProjectActivity(this.props.project).then(() => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    });
  }

  renderList (analyses) {
    if (!analyses.length) {
      return (
          <p className="spacer-top note">
            {translate('no_results')}
          </p>
      );
    }

    return (
        <ul className="spacer-top">
          {analyses.map(analysis => (
              <Analysis key={analysis.key} analysis={analysis}/>
          ))}
        </ul>
    );
  }

  render () {
    const { analyses } = this.props;
    const { loading } = this.state;

    if (loading || !analyses) {
      return null;
    }

    return (
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">
            {translate('project_activity.page')}
          </h4>

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

const mapStateToProps = (state, ownProps: Props) => ({
  analyses: getAnalyses(getProjectActivity(state), ownProps.project)
});

const mapDispatchToProps = { fetchRecentProjectActivity };

export default connect(mapStateToProps, mapDispatchToProps)(AnalysesList);

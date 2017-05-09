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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import ProjectActivityPageHeader from './ProjectActivityPageHeader';
import ProjectActivityAnalysesList from './ProjectActivityAnalysesList';
import ProjectActivityPageFooter from './ProjectActivityPageFooter';
import { fetchProjectActivity } from '../actions';
import { getComponent } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import './projectActivity.css';

type Props = {
  location: { query: { id: string } },
  fetchProjectActivity: (project: string) => void,
  project: { configuration?: { showHistory: boolean } }
};

type State = {
  filter: ?string
};

class ProjectActivityApp extends React.PureComponent {
  props: Props;

  state: State = {
    filter: null
  };

  componentDidMount() {
    const html = document.querySelector('html');
    if (html) {
      html.classList.add('dashboard-page');
    }
    this.props.fetchProjectActivity(this.props.location.query.id);
  }

  componentWillUnmount() {
    const html = document.querySelector('html');
    if (html) {
      html.classList.remove('dashboard-page');
    }
  }

  handleFilter = (filter: ?string) => {
    this.setState({ filter });
    this.props.fetchProjectActivity(this.props.location.query.id, filter);
  };

  render() {
    const project = this.props.location.query.id;
    const { configuration } = this.props.project;
    const canAdmin = configuration ? configuration.showHistory : false;

    return (
      <div id="project-activity" className="page page-limited">
        <Helmet title={translate('project_activity.page')} />

        <ProjectActivityPageHeader
          project={project}
          filter={this.state.filter}
          changeFilter={this.handleFilter}
        />

        <ProjectActivityAnalysesList project={project} canAdmin={canAdmin} />

        <ProjectActivityPageFooter project={project} />
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps: Props) => ({
  project: getComponent(state, ownProps.location.query.id)
});

const mapDispatchToProps = { fetchProjectActivity };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectActivityApp);

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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import ProjectActivityPageHeader from './ProjectActivityPageHeader';
import ProjectActivityAnalysesList from './ProjectActivityAnalysesList';
import ProjectActivityPageFooter from './ProjectActivityPageFooter';
import { fetchProjectActivity, changeFilter } from '../actions';
import { getFilter } from '../../../store/projectActivity/duck';
import { getProjectActivity } from '../../../store/rootReducer';
import './projectActivity.css';

type Props = {
  changeFilter: (project: string, filter: ?string) => void,
  location: { query: { id: string } },
  fetchProjectActivity: (project: string) => void,
  filter: ?string
};

class ProjectActivityApp extends React.Component {
  props: Props;

  componentDidMount () {
    // reset filter when opening the page
    if (this.props.filter) {
      this.props.changeFilter(this.props.location.query.id, null);
    } else {
      this.props.fetchProjectActivity(this.props.location.query.id);
    }
  }

  render () {
    const project = this.props.location.query.id;

    return (
        <div className="page page-limited">
          <ProjectActivityPageHeader project={project}/>
          <ProjectActivityAnalysesList project={project}/>
          <ProjectActivityPageFooter project={project}/>
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps: Props) => ({
  filter: getFilter(getProjectActivity(state), ownProps.location.query.id)
});

const mapDispatchToProps = { fetchProjectActivity, changeFilter };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectActivityApp);

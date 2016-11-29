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
import React from 'react';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListFooterContainer from './ProjectsListFooterContainer';
import PageSidebar from './PageSidebar';
import { parseUrlQuery } from '../store/utils';

export default class AllProjects extends React.Component {
  static propTypes = {
    user: React.PropTypes.oneOfType([React.PropTypes.object, React.PropTypes.bool]),
    isFavorite: React.PropTypes.bool.isRequired,
    fetchProjects: React.PropTypes.func.isRequired
  };

  state = {
    query: {}
  };

  componentDidMount () {
    this.handleQueryChange();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange();
    }
  }

  handleQueryChange () {
    const query = parseUrlQuery(this.props.location.query);
    this.setState({ query });
    this.props.fetchProjects(query, this.props.isFavorite);
  }

  render () {
    const isFiltered = Object.keys(this.state.query).some(key => this.state.query[key] != null);

    return (
        <div className="page-with-sidebar page-with-left-sidebar projects-page">
          <aside className="page-sidebar-fixed projects-sidebar">
            <PageSidebar query={this.state.query} isFavorite={this.props.isFavorite}/>
          </aside>
          <div className="page-main">
            <ProjectsListContainer isFavorite={this.props.isFavorite} isFiltered={isFiltered}/>
            <ProjectsListFooterContainer query={this.state.query} isFavorite={this.props.isFavorite}/>
          </div>
        </div>
    );
  }
}

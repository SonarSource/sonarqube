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
import _ from 'underscore';
import React from 'react';
import Header from './header';
import Search from './search';
import Projects from './projects';
import { PAGE_SIZE, TYPE } from './constants';
import {
    getComponents,
    getProvisioned,
    getGhosts,
    deleteComponents
} from '../../api/components';
import ListFooter from '../../components/controls/ListFooter';

export default React.createClass({
  propTypes: {
    hasProvisionPermission: React.PropTypes.bool.isRequired,
    topLevelQualifiers: React.PropTypes.array.isRequired
  },

  getInitialState() {
    return {
      ready: false,
      projects: [],
      total: 0,
      page: 1,
      query: '',
      qualifiers: 'TRK',
      type: TYPE.ALL,
      selection: []
    };
  },

  componentWillMount () {
    this.requestProjects = _.debounce(this.requestProjects, 250);
  },

  componentDidMount() {
    this.requestProjects();
  },

  getFilters() {
    const filters = { ps: PAGE_SIZE };
    if (this.state.page !== 1) {
      filters.p = this.state.page;
    }
    if (this.state.query) {
      filters.q = this.state.query;
    }
    return filters;
  },

  requestProjects() {
    switch (this.state.type) {
      case TYPE.ALL:
        this.requestAllProjects();
        break;
      case TYPE.PROVISIONED:
        this.requestProvisioned();
        break;
      case TYPE.GHOSTS:
        this.requestGhosts();
        break;
      default:

        // should never happen
    }
  },

  requestGhosts() {
    const data = this.getFilters();
    getGhosts(data).then(r => {
      let projects = r.projects.map(project => {
        return _.extend(project, { id: project.uuid, qualifier: 'TRK' });
      });
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.total });
    });
  },

  requestProvisioned() {
    const data = this.getFilters();
    getProvisioned(data).then(r => {
      let projects = r.projects.map(project => {
        return _.extend(project, { id: project.uuid, qualifier: 'TRK' });
      });
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.total });
    });
  },

  requestAllProjects() {
    const data = this.getFilters();
    data.qualifiers = this.state.qualifiers;
    getComponents(data).then(r => {
      let projects = r.components;
      if (this.state.page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({ ready: true, projects, total: r.paging.total });
    });
  },

  loadMore() {
    this.setState({ ready: false, page: this.state.page + 1 },
        this.requestProjects);
  },

  onSearch(query) {
    this.setState({
      ready: false,
      page: 1,
      query,
      selection: []
    }, this.requestProjects);
  },

  onTypeChanged(newType) {
    this.setState({
      ready: false,
      page: 1,
      query: '',
      type: newType,
      qualifiers: 'TRK',
      selection: []
    }, this.requestProjects);
  },

  onQualifierChanged(newQualifier) {
    this.setState({
      ready: false,
      page: 1,
      query: '',
      type: TYPE.ALL,
      qualifiers: newQualifier,
      selection: []
    }, this.requestProjects);
  },

  onProjectSelected(project) {
    const newSelection = _.uniq([].concat(this.state.selection, project.id));
    this.setState({ selection: newSelection });
  },

  onProjectDeselected(project) {
    const newSelection = _.without(this.state.selection, project.id);
    this.setState({ selection: newSelection });
  },

  onAllSelected() {
    const newSelection = this.state.projects.map(project => {
      return project.id;
    });
    this.setState({ selection: newSelection });
  },

  onAllDeselected() {
    this.setState({ selection: [] });
  },

  deleteProjects() {
    this.setState({ ready: false });
    const ids = this.state.selection.join(',');
    deleteComponents({ ids }).then(() => {
      this.setState({ page: 1, selection: [] }, this.requestProjects);
    });
  },

  render() {
    return (
        <div className="page page-limited">
          <Header
              hasProvisionPermission={this.props.hasProvisionPermission}
              selection={this.state.selection}
              total={this.state.total}
              query={this.state.query}
              qualifier={this.state.qualifiers}
              refresh={this.requestProjects}/>

          <Search {...this.props} {...this.state}
              onSearch={this.onSearch}
              onTypeChanged={this.onTypeChanged}
              onQualifierChanged={this.onQualifierChanged}
              onAllSelected={this.onAllSelected}
              onAllDeselected={this.onAllDeselected}
              deleteProjects={this.deleteProjects}/>

          <Projects
              ready={this.state.ready}
              projects={this.state.projects}
              refresh={this.requestProjects}
              selection={this.state.selection}
              onProjectSelected={this.onProjectSelected}
              onProjectDeselected={this.onProjectDeselected}/>

          <ListFooter
              ready={this.state.ready}
              count={this.state.projects.length}
              total={this.state.total}
              loadMore={this.loadMore}/>
        </div>
    );
  }
});

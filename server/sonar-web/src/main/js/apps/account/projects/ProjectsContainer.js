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
import moment from 'moment';

import Projects from './Projects';
import { getComponents } from '../../../api/components';

function getFakeDescription (project) {
  return `Description of ${project.name}.`;
}

function getFakeLinks () {
  return [
    {
      name: 'Bug Tracker',
      type: 'issue',
      href: 'http://jira.sonarsource.com/browse/SONAR'
    },
    {
      name: 'Continuous integration',
      type: 'ci',
      href: 'https://travis-ci.org/SonarSource/sonarqube'
    },
    {
      name: 'Developer connection',
      type: 'scm_dev',
      href: 'scm:git:git@github.com:SonarSource/sonarqube.git'
    },
    {
      name: 'Home',
      type: 'homepage',
      href: 'http://www.sonarqube.org/'
    },
    {
      name: 'Sources',
      type: 'scm',
      href: 'https://github.com/SonarSource/sonarqube'
    }
  ];
}

function getFakeDate (project) {
  return moment().subtract(project.name.length, 'days').format('YYYY-MM-DD');
}

function getFakeLevel (project) {
  const levels = ['OK', 'WARN', 'ERROR', null];
  return levels[project.name.length % 4];
}

function fillFakeData (projects) {
  return projects.map(project => ({
    ...project,
    description: getFakeDescription(project),
    links: getFakeLinks(),
    lastAnalysisDate: getFakeDate(project),
    qualityGateStatus: getFakeLevel(project)
  }));
}

export default class ProjectsContainer extends React.Component {
  state = { loading: true };

  componentWillMount () {
    this.loadMore = this.loadMore.bind(this);
    document.querySelector('html').classList.add('dashboard-page');
  }

  componentDidMount () {
    this.mounted = true;
    this.loadProjects();
  }

  componentWillUnmount () {
    this.mounted = false;
    document.querySelector('html').classList.remove('dashboard-page');
  }

  loadProjects (page = 1) {
    this.setState({ loading: true });
    const options = { qualifiers: 'TRK', p: page };
    return getComponents(options).then(r => {
      let projects = fillFakeData(r.components);
      if (page > 1) {
        projects = [].concat(this.state.projects, projects);
      }
      this.setState({
        projects,
        total: r.paging.total,
        page: r.paging.pageIndex,
        loading: false
      });
    });
  }

  loadMore () {
    return this.loadProjects(this.state.page + 1);
  }

  render () {
    if (this.state.projects == null) {
      return (
          <div className="text-center">
            <i className="spinner spinner-margin"/>
          </div>
      );
    }

    return (
        <Projects
            projects={this.state.projects}
            total={this.state.total}
            loading={this.state.loading}
            loadMore={this.loadMore}/>
    );
  }
}

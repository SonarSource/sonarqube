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
import React from 'react';
import Helmet from 'react-helmet';
import PageHeaderContainer from './PageHeaderContainer';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListFooterContainer from './ProjectsListFooterContainer';
import PageSidebar from './PageSidebar';
import VisualizationsContainer from '../visualizations/VisualizationsContainer';
import { parseUrlQuery } from '../store/utils';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

export default class AllProjects extends React.PureComponent {
  static propTypes = {
    isFavorite: React.PropTypes.bool.isRequired,
    location: React.PropTypes.object.isRequired,
    fetchProjects: React.PropTypes.func.isRequired,
    organization: React.PropTypes.object,
    router: React.PropTypes.object.isRequired
  };

  state = {
    query: {}
  };

  componentDidMount() {
    this.handleQueryChange();
    document.getElementById('footer').classList.add('search-navigator-footer');
  }

  componentDidUpdate(prevProps) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange();
    }
  }

  componentWillUnmount() {
    document.getElementById('footer').classList.remove('search-navigator-footer');
  }

  handleQueryChange() {
    const query = parseUrlQuery(this.props.location.query);
    this.setState({ query });
    this.props.fetchProjects(query, this.props.isFavorite, this.props.organization);
  }

  handleViewChange = view => {
    const query = {
      ...this.props.location.query,
      view: view === 'list' ? undefined : view
    };
    if (query.view !== 'visualizations') {
      Object.assign(query, { visualization: undefined });
    }
    this.props.router.push({
      pathname: this.props.location.pathname,
      query
    });
  };

  handleVisualizationChange = visualization => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...this.props.location.query,
        view: 'visualizations',
        visualization
      }
    });
  };

  render() {
    const { query } = this.state;
    const isFiltered = Object.keys(query).some(key => query[key] != null);

    const view = query.view || 'list';
    const visualization = query.visualization || 'risk';

    const top = this.props.organization ? 95 : 30;

    return (
      <div className="layout-page projects-page">
        <Helmet title={translate('projects.page')} />
        <div className="layout-page-side-outer">
          <div className="layout-page-side" style={{ top }}>
            <div className="layout-page-side-inner">
              <div className="layout-page-filters">
                <PageSidebar
                  query={query}
                  isFavorite={this.props.isFavorite}
                  organization={this.props.organization}
                />
              </div>
            </div>
          </div>
        </div>

        <div className="layout-page-main">
          <div className="layout-page-main-inner">
            <PageHeaderContainer onViewChange={this.handleViewChange} view={view} />
            {view === 'list' &&
              <ProjectsListContainer
                isFavorite={this.props.isFavorite}
                isFiltered={isFiltered}
                organization={this.props.organization}
              />}
            {view === 'list' &&
              <ProjectsListFooterContainer
                query={query}
                isFavorite={this.props.isFavorite}
                organization={this.props.organization}
              />}
            {view === 'visualizations' &&
              <VisualizationsContainer
                onVisualizationChange={this.handleVisualizationChange}
                sort={query.sort}
                visualization={visualization}
              />}
          </div>
        </div>
      </div>
    );
  }
}

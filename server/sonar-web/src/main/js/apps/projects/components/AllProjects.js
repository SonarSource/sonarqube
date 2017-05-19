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
//@flow
import React from 'react';
import Helmet from 'react-helmet';
import PageHeaderContainer from './PageHeaderContainer';
import ProjectOptionBar from './ProjectOptionBar';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListFooterContainer from './ProjectsListFooterContainer';
import PageSidebar from './PageSidebar';
import VisualizationsContainer from '../visualizations/VisualizationsContainer';
import { parseUrlQuery } from '../store/utils';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

type Props = {
  isFavorite: boolean,
  location: { pathname: string, query: { [string]: string } },
  fetchProjects: (query: string, isFavorite: boolean, organization?: {}) => Promise<*>,
  organization?: { key: string },
  router: { push: ({ pathname: string, query?: {} }) => void }
};

type State = {
  query: { [string]: string },
  optionBarOpen: boolean
};

export default class AllProjects extends React.PureComponent {
  props: Props;
  state: State = {
    query: {},
    optionBarOpen: false
  };

  componentDidMount() {
    this.handleQueryChange();
    const footer = document.getElementById('footer');
    footer && footer.classList.add('search-navigator-footer');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange();
    }
  }

  componentWillUnmount() {
    const footer = document.getElementById('footer');
    footer && footer.classList.remove('search-navigator-footer');
  }

  openOptionBar = (evt: Event & { currentTarget: HTMLElement }) => {
    evt.currentTarget.blur();
    evt.preventDefault();
    this.handleOptionBarToggle(true);
  };

  handleOptionBarToggle = (open: boolean) => this.setState({ optionBarOpen: open });

  handlePerspectiveChange = ({ view, visualization }: { view: string, visualization?: string }) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...this.props.location.query,
        view: view === 'overall' ? undefined : view,
        visualization
      }
    });
  };

  handleQueryChange() {
    const query = parseUrlQuery(this.props.location.query);
    this.setState({ query });
    this.props.fetchProjects(query, this.props.isFavorite, this.props.organization);
  }

  render() {
    const { query, optionBarOpen } = this.state;
    const isFiltered = Object.keys(query).some(key => query[key] != null);

    const view = query.view || 'overall';
    const visualization = query.visualization || 'risk';

    const top = (this.props.organization ? 95 : 30) + (optionBarOpen ? 45 : 0);

    return (
      <div>
        <Helmet title={translate('projects.page')} />

        <ProjectOptionBar
          onPerspectiveChange={this.handlePerspectiveChange}
          onToggleOptionBar={this.handleOptionBarToggle}
          open={optionBarOpen}
          view={view}
          visualization={visualization}
        />

        <div className="layout-page projects-page">
          <div className="layout-page-side-outer">
            <div className="layout-page-side projects-page-side" style={{ top }}>
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
              <PageHeaderContainer onOpenOptionBar={this.openOptionBar} />
              {view === 'overall' &&
                <ProjectsListContainer
                  isFavorite={this.props.isFavorite}
                  isFiltered={isFiltered}
                  organization={this.props.organization}
                />}
              {view === 'overall' &&
                <ProjectsListFooterContainer
                  query={query}
                  isFavorite={this.props.isFavorite}
                  organization={this.props.organization}
                />}
              {view === 'visualizations' &&
                <VisualizationsContainer sort={query.sort} visualization={visualization} />}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

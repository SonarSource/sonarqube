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
import ProjectsOptionBarContainer from './ProjectsOptionBarContainer';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListFooterContainer from './ProjectsListFooterContainer';
import PageSidebar from './PageSidebar';
import VisualizationsContainer from '../visualizations/VisualizationsContainer';
import { parseUrlQuery } from '../store/utils';
import { translate } from '../../../helpers/l10n';
import { SORTING_SWITCH, parseSorting } from '../utils';
import '../styles.css';

type Props = {
  isFavorite: boolean,
  location: { pathname: string, query: { [string]: string } },
  fetchProjects: (query: string, isFavorite: boolean, organization?: {}) => Promise<*>,
  optionBarOpen: boolean,
  optionBarToggle: (open: boolean) => void,
  organization?: { key: string },
  router: { push: ({ pathname: string, query?: {} }) => void },
  currentUser?: { isLoggedIn: boolean }
};

type State = {
  query: { [string]: string }
};

export default class AllProjects extends React.PureComponent {
  props: Props;
  state: State = { query: {} };

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
    this.props.optionBarToggle(true);
  };

  handlePerspectiveChange = ({ view, visualization }: { view: string, visualization?: string }) => {
    const query: { view: ?string, visualization: ?string, sort?: ?string } = {
      view: view === 'overall' ? undefined : view,
      visualization
    };

    if (this.state.query.view === 'leak' || view === 'leak') {
      if (this.state.query.sort) {
        const sort = parseSorting(this.state.query.sort);
        if (SORTING_SWITCH[sort.sortValue]) {
          query.sort = (sort.sortDesc ? '-' : '') + SORTING_SWITCH[sort.sortValue];
        }
      }
      this.props.router.push({ pathname: this.props.location.pathname, query });
    } else {
      this.updateLocationQuery(query);
    }
  };

  handleSortChange = (sort: string, desc: boolean) =>
    this.updateLocationQuery({ sort: (desc ? '-' : '') + sort });

  handleQueryChange() {
    const query = parseUrlQuery(this.props.location.query);
    this.setState({ query });
    this.props.fetchProjects(query, this.props.isFavorite, this.props.organization);
  }

  updateLocationQuery = (newQuery: { [string]: ?string }) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...this.props.location.query,
        ...newQuery
      }
    });
  };

  render() {
    const { isFavorite, organization, optionBarOpen } = this.props;
    const { query } = this.state;
    const isFiltered = Object.keys(query).some(key => query[key] != null);

    const view = query.view || 'overall';
    const visualization = query.visualization || 'risk';
    const selectedSort = query.sort || 'name';

    const sideBarTop = (organization ? 95 : 30) + (optionBarOpen ? 45 : 0);
    const contentTop = optionBarOpen ? 65 : 20;

    return (
      <div>
        <Helmet title={translate('projects.page')} />

        <ProjectsOptionBarContainer
          onPerspectiveChange={this.handlePerspectiveChange}
          onSortChange={this.handleSortChange}
          onToggleOptionBar={this.props.optionBarToggle}
          open={optionBarOpen}
          selectedSort={selectedSort}
          currentUser={this.props.currentUser}
          view={view}
          visualization={visualization}
        />

        <div className="layout-page projects-page">
          <div className="layout-page-side-outer">
            <div className="layout-page-side projects-page-side" style={{ top: sideBarTop }}>
              <div className="layout-page-side-inner">
                <div className="layout-page-filters">
                  <PageSidebar
                    isFavorite={isFavorite}
                    organization={organization}
                    query={query}
                    view={view}
                    visualization={visualization}
                  />
                </div>
              </div>
            </div>
          </div>

          <div
            className="layout-page-main projects-page-content"
            style={{ paddingTop: contentTop }}>
            <div className="layout-page-main-inner">
              <PageHeaderContainer
                query={query}
                isFavorite={isFavorite}
                organization={organization}
                onOpenOptionBar={this.openOptionBar}
                optionBarOpen={optionBarOpen}
              />
              {view !== 'visualizations' &&
                <ProjectsListContainer
                  isFavorite={isFavorite}
                  isFiltered={isFiltered}
                  organization={organization}
                  cardType={view}
                />}
              {view !== 'visualizations' &&
                <ProjectsListFooterContainer
                  query={query}
                  isFavorite={isFavorite}
                  organization={organization}
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

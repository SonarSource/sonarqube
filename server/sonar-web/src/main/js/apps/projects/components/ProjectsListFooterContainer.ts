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
import { connect } from 'react-redux';
import { getProjects, getProjectsAppState } from '../../../store/rootReducer';
import { fetchMoreProjects } from '../store/actions';
import ListFooter from '../../../components/controls/ListFooter';

const mapStateToProps = (state: any) => {
  const projects = getProjects(state);
  const appState = getProjectsAppState(state);
  return {
    count: projects != null ? projects.length : 0,
    total: appState.total != null ? appState.total : 0,
    ready: !appState.loading
  };
};

const mapDispatchToProps = (dispatch: any, ownProps: any) => ({
  loadMore: () =>
    dispatch(fetchMoreProjects(ownProps.query, ownProps.isFavorite, ownProps.organization))
});

export default connect(mapStateToProps, mapDispatchToProps)(ListFooter);

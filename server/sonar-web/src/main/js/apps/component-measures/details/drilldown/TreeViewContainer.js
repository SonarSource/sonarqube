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
import { connect } from 'react-redux';
import TreeView from './TreeView';
import {
  start,
  drilldown,
  useBreadcrumbs,
  fetchMore,
  selectComponent,
  selectNext,
  selectPrevious
} from '../../store/treeViewActions';
import {
  getMeasuresAppTreeComponents,
  getMeasuresAppTreeBreadcrumbs,
  getMeasuresAppTreeSelected,
  getMeasuresAppTreeTotal,
  getMeasuresAppTreePageIndex,
  getMeasuresAppAllMetrics,
  getMeasuresAppDetailsMetric,
  isMeasuresAppFetching
  , getMeasuresAppComponent
} from '../../../../app/store/rootReducer';

const mapStateToProps = state => {
  return {
    components: getMeasuresAppTreeComponents(state),
    breadcrumbs: getMeasuresAppTreeBreadcrumbs(state),
    selected: getMeasuresAppTreeSelected(state),
    total: getMeasuresAppTreeTotal(state),
    pageIndex: getMeasuresAppTreePageIndex(state),
    component: getMeasuresAppComponent(state),
    metrics: getMeasuresAppAllMetrics(state),
    metric: getMeasuresAppDetailsMetric(state),
    fetching: isMeasuresAppFetching(state)
  };
};

const mapDispatchToProps = dispatch => {
  return {
    onStart: (rootComponent, metric, periodIndex) => dispatch(start(rootComponent, metric, periodIndex)),
    onDrilldown: component => dispatch(drilldown(component)),
    onUseBreadcrumbs: component => dispatch(useBreadcrumbs(component)),
    onFetchMore: () => dispatch(fetchMore()),
    onSelect: component => dispatch(selectComponent(component)),
    onSelectNext: component => dispatch(selectNext(component)),
    onSelectPrevious: component => dispatch(selectPrevious(component))
  };
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(TreeView);

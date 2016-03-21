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
import pick from '../../../../../../../node_modules/lodash/pick';

import TreeView from './TreeView';
import {
    start,
    fetchMore,
    drilldown,
    useBreadcrumbs,
    selectComponent,
    selectNext,
    selectPrevious
} from '../../store/treeViewActions';

const mapStateToProps = state => {
  const drilldown = pick(state.tree, [
    'components',
    'breadcrumbs',
    'selected',
    'total',
    'pageIndex'
  ]);
  return {
    ...drilldown,
    component: state.app.component,
    metric: state.details.metric,
    fetching: state.status.fetching
  };
};

const mapDispatchToProps = dispatch => {
  return {
    onStart: (rootComponent, metric) => dispatch(start(rootComponent, metric)),
    onFetchMore: () => dispatch(fetchMore()),
    onDrilldown: component => dispatch(drilldown(component)),
    onUseBreadcrumbs: component => dispatch(useBreadcrumbs(component)),
    onSelect: component => dispatch(selectComponent(component)),
    onSelectNext: component => dispatch(selectNext(component)),
    onSelectPrevious: component => dispatch(selectPrevious(component))
  };
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(TreeView);

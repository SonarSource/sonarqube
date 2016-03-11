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

import Search from '../components/Search';
import { filterTasks, search } from '../store/actions';
import { STATUSES, CURRENTS, DEFAULT_FILTERS } from '../constants';

function mapStateToProps (state) {
  return {
    fetching: state.fetching,
    status: state.status,
    currents: state.currents,
    date: state.date,
    query: state.query,
    taskType: state.taskType,
    types: state.types,
    component: state.component
  };
}

function updateStatusQuery (status) {
  if (status === STATUSES.PENDING) {
    return { status, currents: CURRENTS.ALL };
  } else {
    return { status };
  }
}

function mapDispatchToProps (dispatch) {
  return {
    onRefresh: () => dispatch(filterTasks()),
    onReset: () => dispatch(filterTasks(DEFAULT_FILTERS)),
    onStatusChange: status => dispatch(filterTasks(updateStatusQuery(status))),
    onTypeChange: taskType => dispatch(filterTasks({ taskType })),
    onCurrentsChange: currents => dispatch(filterTasks({ currents, status: STATUSES.ALL_EXCEPT_PENDING })),
    onDateChange: date => dispatch(filterTasks({ date })),
    onSearch: query => dispatch(search(query))
  };
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Search);

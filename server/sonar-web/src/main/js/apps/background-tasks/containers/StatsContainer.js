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

import Stats from '../components/Stats';
import { filterTasks, cancelAllPending } from '../store/actions';
import { STATUSES, CURRENTS, DEFAULT_FILTERS } from '../constants';

function mapStateToProps (state) {
  return {
    pendingCount: state.pendingCount,
    failingCount: state.failingCount,
    inProgressDuration: state.inProgressDuration,
    component: state.component
  };
}

function mapDispatchToProps (dispatch) {
  return {
    onShowFailing: () => dispatch(filterTasks({
      ...DEFAULT_FILTERS,
      status: STATUSES.FAILED,
      currents: CURRENTS.ONLY_CURRENTS
    })),
    onCancelAllPending: () => dispatch(cancelAllPending())
  };
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Stats);

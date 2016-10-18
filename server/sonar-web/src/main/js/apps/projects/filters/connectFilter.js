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
import omitBy from 'lodash/omitBy';
import isNil from 'lodash/isNil';
import { getProjectsAppFilterStatus } from '../../../app/store/rootReducer';
import { toggleFilter } from '../store/filters/statuses/actions';
import { OPEN } from '../store/filters/statuses/reducer';

const connectFilter = (key, getValue) => Component => {
  const mapStateToProps = (state, ownProps) => ({
    isOpen: getProjectsAppFilterStatus(state, key) === OPEN,
    value: getValue(ownProps.query),
    getFilterUrl: part => {
      const query = omitBy({ ...ownProps.query, ...part }, isNil);
      return { pathname: '/projects', query };
    }
  });

  const mapDispatchToProps = dispatch => ({
    toggleFilter: () => dispatch(toggleFilter(key))
  });

  return connect(mapStateToProps, mapDispatchToProps)(Component);
};

export default connectFilter;

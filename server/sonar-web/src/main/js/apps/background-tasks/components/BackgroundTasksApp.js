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
import moment from 'moment';
import React, { Component } from 'react';

import { DATE } from './../constants';
import Header from './Header';
import StatsContainer from '../containers/StatsContainer';
import SearchContainer from '../containers/SearchContainer';
import TasksContainer from '../containers/TasksContainer';
import ListFooterContainer from '../containers/ListFooterContainer';


export default class BackgroundTasksApp extends Component {
  componentDidMount () {
    this.props.initApp();
  }

  getComponentFilter () {
    if (this.props.options.component) {
      return { componentId: this.props.options.component.id };
    } else {
      return {};
    }
  }

  getDateFilter () {
    const DATE_FORMAT = 'YYYY-MM-DD';
    let filter = {};
    switch (this.state.dateFilter) {
      case DATE.TODAY:
        filter.minSubmittedAt = moment().startOf('day').format(DATE_FORMAT);
        break;
      case DATE.CUSTOM:
        if (this.state.minDate) {
          filter.minSubmittedAt = moment(this.state.minDate).format(DATE_FORMAT);
        }
        if (this.state.maxDate) {
          filter.maxExecutedAt = moment(this.state.maxDate).format(DATE_FORMAT);
        }
        break;
      default:
      // do nothing
    }
    return filter;
  }

  render () {
    return (
        <div className="page">
          <Header/>
          <StatsContainer/>
          <SearchContainer/>
          <TasksContainer/>
          <ListFooterContainer/>
        </div>
    );
  }
}

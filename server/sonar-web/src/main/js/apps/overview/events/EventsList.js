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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';

import Event from './Event';
import EventsListFilter from './EventsListFilter';
import { getEvents } from '../../../api/events';
import { translate } from '../../../helpers/l10n';

const LIMIT = 5;

export default class EventsList extends React.Component {
  state = {
    events: [],
    limited: true,
    filter: 'All'
  };

  componentDidMount () {
    this.mounted = true;
    this.fetchEvents();
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  componentDidUpdate (nextProps) {
    if (nextProps.component !== this.props.component) {
      this.fetchEvents();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchEvents () {
    getEvents(this.props.component.key).then(events => {
      if (this.mounted) {
        const nextEvents = events.map(event => {
          return {
            id: event.id,
            date: moment(event.dt).toDate(),
            type: event.c,
            name: event.n,
            text: event.ds
          };
        });

        this.setState({ events: nextEvents });
      }
    });
  }

  limitEvents (events) {
    return this.state.limited ? events.slice(0, LIMIT) : events;
  }

  filterEvents (events) {
    if (this.state.filter === 'All') {
      return events;
    } else {
      return events.filter(event => event.type === this.state.filter);
    }
  }

  handleClick (e) {
    e.preventDefault();
    this.setState({ limited: !this.state.limited });
  }

  handleFilter (filter) {
    this.setState({ filter });
  }

  renderMoreLink () {
    const text = this.state.limited ?
        translate('widget.events.show_all') :
        translate('hide');

    return (
        <p className="spacer-top note">
          <a onClick={this.handleClick.bind(this)} href="#">{text}</a>
        </p>
    );
  }

  renderList (events) {
    if (events.length) {
      return (
          <ul>
            {events.map(event => (
                <Event key={event.id} event={event}/>
            ))}
          </ul>
      );
    } else {
      return (
          <p className="spacer-top note">
            {translate('no_results')}
          </p>
      );
    }
  }

  render () {
    const filteredEvents = this.filterEvents(this.state.events);
    const events = this.limitEvents(filteredEvents);

    return (
        <div className="overview-meta-card">
          <div className="clearfix">
            <h4 className="pull-left overview-meta-header">
              {translate('widget.events.name')}
            </h4>
            <div className="pull-right">
              <EventsListFilter
                  currentFilter={this.state.filter}
                  onFilter={this.handleFilter.bind(this)}/>
            </div>
          </div>

          {this.renderList(events)}

          {filteredEvents.length > LIMIT && this.renderMoreLink(filteredEvents)}
        </div>
    );
  }
}

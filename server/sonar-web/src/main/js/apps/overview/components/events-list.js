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
import React from 'react';

import { Event } from './event';
import { EventsListFilter } from './events-list-filter';
import { translate } from '../../../helpers/l10n';


const LIMIT = 5;


export const EventsList = React.createClass({
  propTypes: {
    events: React.PropTypes.arrayOf(React.PropTypes.shape({
      id: React.PropTypes.string.isRequired,
      date: React.PropTypes.object.isRequired,
      type: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired,
      text: React.PropTypes.string
    }).isRequired).isRequired
  },

  getInitialState() {
    return { limited: true, filter: 'All' };
  },

  limitEvents(events) {
    return this.state.limited ? events.slice(0, LIMIT) : events;
  },

  filterEvents(events) {
    if (this.state.filter === 'All') {
      return events;
    } else {
      return events.filter(event => event.type === this.state.filter);
    }
  },

  handleClick(e) {
    e.preventDefault();
    this.setState({ limited: !this.state.limited });
  },

  handleFilter(filter) {
    this.setState({ filter });
  },

  renderMoreLink(filteredEvents) {
    if (filteredEvents.length > LIMIT) {
      const text = this.state.limited ? translate('widget.events.show_all') : translate('hide');
      return <p className="spacer-top note">
        <a onClick={this.handleClick} href="#">{text}</a>
      </p>;
    } else {
      return null;
    }
  },

  renderList (events) {
    if (events.length) {
      return <ul>{events.map(event => <Event key={event.id} event={event}/>)}</ul>;
    } else {
      return <p className="spacer-top note">{translate('no_results')}</p>;
    }
  },

  render () {
    const filteredEvents = this.filterEvents(this.props.events);
    const events = this.limitEvents(filteredEvents);
    return <div className="overview-meta-card">
      <div className="clearfix">
        <h4 className="pull-left overview-meta-header">{translate('widget.events.name')}</h4>
        <div className="pull-right">
          <EventsListFilter currentFilter={this.state.filter} onFilter={this.handleFilter}/>
        </div>
      </div>
      {this.renderList(events)}
      {this.renderMoreLink(filteredEvents)}
    </div>;
  }
});

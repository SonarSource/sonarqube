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
import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { QualityProfileLink } from './../../components/shared/quality-profile-link';
import { QualityGateLink } from './../../components/shared/quality-gate-link';
import { getEvents } from '../../api/events';
import { EventsList } from './components/events-list';
import { translate } from '../../helpers/l10n';


export default React.createClass({
  componentDidMount() {
    this.requestEvents();
  },

  requestEvents () {
    return getEvents(this.props.component.key).then(events => {
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
    });
  },

  isView() {
    return this.props.component.qualifier === 'VW' || this.props.component.qualifier === 'SVW';
  },

  isDeveloper() {
    return this.props.component.qualifier === 'DEV';
  },

  renderEvents() {
    if (this.state && this.state.events) {
      return <EventsList component={this.props.component} events={this.state.events}/>;
    } else {
      return null;
    }
  },

  render() {
    let profiles = (this.props.component.profiles || []).map(profile => {
      return (
          <li key={profile.key}>
            <span className="note spacer-right">({profile.language})</span>
            <QualityProfileLink profile={profile.key}>{profile.name}</QualityProfileLink>
          </li>
      );
    });
    let links = (this.props.component.links || []).map(link => {
      let iconClassName = `spacer-right icon-color-link icon-${link.type}`;
      return (
          <li key={link.type}>
            <i className={iconClassName}/>
            <a href={link.href} target="_blank">{link.name}</a>
          </li>
      );
    });

    let descriptionCard = this.props.component.description ? (
        <div className="overview-meta-description big-spacer-bottom">
          {this.props.component.description}
        </div>
    ) : null;

    let linksCard = _.size(this.props.component.links) > 0 ? (
        <ul className="overview-meta-list big-spacer-bottom">
          {links}
        </ul>
    ) : null;

    let keyCard = (
        <div>
          <h4 className="overview-meta-header">{translate('key')}</h4>
          <input
              className="overview-key"
              type="text"
              value={this.props.component.key}
              readOnly={true}/>
        </div>
    );

    let profilesCard = !this.isView() && !this.isDeveloper() && _.size(this.props.component.profiles) > 0 ? (
        <div>
          <h4 className="overview-meta-header">{translate('overview.quality_profiles')}</h4>
          <ul className="overview-meta-list">{profiles}</ul>
        </div>
    ) : null;

    let gateCard = !this.isView() && !this.isDeveloper() && this.props.component.gate ? (
        <div className="big-spacer-bottom">
          <h4 className="overview-meta-header">{translate('overview.quality_gate')}</h4>
          <ul className="overview-meta-list">
            <li>
              {this.props.component.gate.isDefault ?
                  <span className="note spacer-right">(Default)</span> : null}
              <QualityGateLink gate={this.props.component.gate.key}>
                {this.props.component.gate.name}
              </QualityGateLink>
            </li>
          </ul>
        </div>
    ) : null;

    return (
        <div className="overview-meta">
          <div className="overview-meta-card">
            {descriptionCard}
            {linksCard}
            {keyCard}
          </div>
          {(!!gateCard || !!profilesCard) && (
              <div className="overview-meta-card">
                {gateCard}
                {profilesCard}
              </div>
          )}
          {this.renderEvents()}
        </div>
    );
  }
});

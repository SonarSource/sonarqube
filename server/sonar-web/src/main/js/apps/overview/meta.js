import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { QualityProfileLink } from './../../components/shared/quality-profile-link';
import { QualityGateLink } from './../../components/shared/quality-gate-link';
import { getEvents } from '../../api/events';
import { EventsList } from './components/events-list';


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
        <div className="overview-meta-card">
          <div className="overview-meta-description">{this.props.component.description}</div>
        </div>
    ) : null;

    let linksCard = _.size(this.props.component.links) > 0 ? (
        <div className="overview-meta-card">
          <ul className="overview-meta-list">{links}</ul>
        </div>
    ) : null;

    let profilesCard = !this.isView() && !this.isDeveloper() && _.size(this.props.component.profiles) > 0 ? (
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">{window.t('overview.quality_profiles')}</h4>
          <ul className="overview-meta-list">{profiles}</ul>
        </div>
    ) : null;

    let gateCard = !this.isView() && !this.isDeveloper() && this.props.component.gate ? (
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">{window.t('overview.quality_gate')}</h4>
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
          {descriptionCard}
          {linksCard}
          {gateCard}
          {profilesCard}
          {this.renderEvents()}
        </div>
    );
  }
});

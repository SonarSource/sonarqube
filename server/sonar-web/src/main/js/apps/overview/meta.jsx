import React from 'react';
import ProfileLink from './helpers/profile-link';
import GateLink from './helpers/gate-link';

export default React.createClass({
  render() {
    const
        profiles = (this.props.component.profiles || []).map(profile => {
          return (
              <li key={profile.key}>
                <span className="note little-spacer-right">({profile.language})</span>
                <ProfileLink profile={profile.key}>{profile.name}</ProfileLink>
              </li>
          );
        }),
        links = (this.props.component.links || []).map(link => {
          const iconClassName = `little-spacer-right icon-color-link icon-${link.type}`;
          return (
              <li key={link.type}>
                <i className={iconClassName}></i>
                <a href={link.href} target="_blank">{link.name}</a>
              </li>
          );
        });

    const descriptionCard = this.props.component.description ? (
            <div className="overview-card">
              <div className="overview-meta-description">{this.props.component.description}</div>
            </div>
        ) : null,

        linksCard = _.size(this.props.component.links) > 0 ? (
            <div className="overview-card">
              <ul className="overview-meta-list">{links}</ul>
            </div>
        ) : null,

        profilesCard = _.size(this.props.component.profiles) > 0 ? (
            <div className="overview-card">
              <h4 className="overview-meta-header">{window.t('overview.quality_profiles')}</h4>
              <ul className="overview-meta-list">{profiles}</ul>
            </div>
        ) : null,

        gateCard = this.props.component.gate ? (
            <div className="overview-card">
              <h4 className="overview-meta-header">{window.t('overview.quality_gate')}</h4>
              <ul className="overview-meta-list">
                <li>
                  {this.props.component.gate.isDefault ?
                      <span className="note little-spacer-right">(Default)</span> : null}
                  <GateLink gate={this.props.component.gate.key}>{this.props.component.gate.name}</GateLink>
                </li>
              </ul>
            </div>
        ) : null;

    return (
        <div className="overview-meta">
          {descriptionCard}
          {linksCard}
          {profilesCard}
          {gateCard}
        </div>
    );
  }
});

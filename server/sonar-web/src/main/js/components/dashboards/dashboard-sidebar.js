import qs from 'querystring';
import _ from 'underscore';
import classNames from 'classnames';
import React from 'react';

import { getLocalizedDashboardName } from '../../helpers/l10n';
import { getComponentDashboardUrl, getComponentFixedDashboardUrl, getComponentDashboardManagementUrl } from '../../helpers/urls';


const FIXED_DASHBOARDS = [
  { link: '', name: 'overview.page' },
  { link: '/issues', name: 'overview.domain.debt' },
  { link: '/tests', name: 'overview.domain.coverage' },
  { link: '/duplications', name: 'overview.domain.duplications' },
  { link: '/size', name: 'overview.domain.size' }
];

const CUSTOM_DASHBOARDS_LIMIT = 1;


export const DashboardSidebar = React.createClass({
  propTypes: {
    component: React.PropTypes.object.isRequired,
    customDashboards: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  periodParameter() {
    let params = qs.parse(window.location.search.substr(1));
    return params.period ? `&period=${params.period}` : '';
  },

  getPeriod() {
    let params = qs.parse(window.location.search.substr(1));
    return params.period;
  },

  isFixedDashboardActive(fixedDashboard) {
    let path = window.location.pathname;
    return path === `${window.baseUrl}/overview${fixedDashboard.link}`;
  },

  isCustomDashboardActive(customDashboard) {
    let path = window.location.pathname,
        params = qs.parse(window.location.search.substr(1));
    return path.indexOf(`${window.baseUrl}/dashboard`) === 0 && params['did'] === `${customDashboard.key}`;
  },

  isMoreCustomDashboardsActive () {
    let dashboards = _.rest(this.props.customDashboards, CUSTOM_DASHBOARDS_LIMIT);
    return _.any(dashboards, this.isCustomDashboardActive);
  },

  isDashboardManagementActive () {
    let path = window.location.pathname;
    return path.indexOf(`${window.baseUrl}/dashboards`) === 0;
  },

  renderFixedDashboards() {
    return FIXED_DASHBOARDS.map(fixedDashboard => {
      let key = 'fixed-dashboard-' + fixedDashboard.link.substr(1);
      let url = getComponentFixedDashboardUrl(this.props.component.key, fixedDashboard.link);
      let name = window.t(fixedDashboard.name);
      let className = classNames({ active: this.isFixedDashboardActive(fixedDashboard) });
      return <li key={key} className={className}>
        <a href={url}>{name}</a>
      </li>;
    });
  },

  renderCustomDashboards() {
    let dashboards = _.first(this.props.customDashboards, CUSTOM_DASHBOARDS_LIMIT);
    return dashboards.map(this.renderCustomDashboard);
  },

  renderCustomDashboard(customDashboard) {
    let key = 'custom-dashboard-' + customDashboard.key;
    let url = getComponentDashboardUrl(this.props.component.key, customDashboard.key, this.getPeriod());
    let name = getLocalizedDashboardName(customDashboard.name);
    let className = classNames({ active: this.isCustomDashboardActive(customDashboard) });
    return <li key={key} className={className}>
      <a href={url}>{name}</a>
    </li>;
  },

  renderMoreCustomDashboards() {
    if (this.props.customDashboards.length <= CUSTOM_DASHBOARDS_LIMIT) {
      return null;
    }
    let dashboards = _.rest(this.props.customDashboards, CUSTOM_DASHBOARDS_LIMIT)
        .map(this.renderCustomDashboard);
    let className = classNames('dropdown', { active: this.isMoreCustomDashboardsActive() });
    return <li className={className}>
      <a className="dropdown-toggle" data-toggle="dropdown" href="#">
        More&nbsp;
        <i className="icon-dropdown"/>
      </a>
      <ul className="dropdown-menu">{dashboards}</ul>
    </li>;
  },

  renderDashboardsManagementLink() {
    if (!window.SS.user) {
      return null;
    }
    let key = 'dashboard-management';
    let url = getComponentDashboardManagementUrl(this.props.component.key);
    let name = window.t('dashboard.manage_dashboards');
    let className = classNames('pill-right', { active: this.isDashboardManagementActive() });
    return <li key={key} className={className}>
      <a className="note" href={url}>{name}</a>
    </li>;
  },

  render() {
    return <nav className="navbar-side">
      <ul className="pills">
        {this.renderFixedDashboards()}
        {this.renderCustomDashboards()}
        {this.renderMoreCustomDashboards()}
        {this.renderDashboardsManagementLink()}
      </ul>
    </nav>;
  }
});

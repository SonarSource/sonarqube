import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import {STATUSES} from '../../background-tasks/constants';
import {getTasksForComponent} from '../../../api/ce';
import ComponentNavFavorite from './component-nav-favorite';
import ComponentNavBreadcrumbs from './component-nav-breadcrumbs';
import ComponentNavMeta from './component-nav-meta';
import ComponentNavMenu from './component-nav-menu';

export default React.createClass({
  getInitialState() {
    return { component: {}, conf: {} };
  },

  componentDidMount() {
    this.loadDetails().then(this.loadStatus);
  },

  loadDetails() {
    const url = `${window.baseUrl}/api/navigation/component`;
    const data = { componentKey: this.props.componentKey };
    return $.get(url, data).done(r => {
      this.setState({
        component: r,
        conf: r.configuration || {}
      });
    });
  },

  loadStatus(component) {
    getTasksForComponent(component.uuid).done(r => {
      this.setState({
        isPending: !!_.findWhere(r.queue, { status: STATUSES.PENDING }),
        isInProgress: !!_.findWhere(r.queue, { status: STATUSES.IN_PROGRESS }),
        isFailed: r.current && r.current.status === STATUSES.FAILED
      }, this.initTooltips);
    });
  },

  initTooltips() {
    $('[data-toggle="tooltip"]', React.findDOMNode(this)).tooltip({
      container: 'body',
      placement: 'bottom',
      delay: { show: 0, hide: 1000 },
      html: true
    });
  },

  render() {
    return (
        <div className="container">
          <ComponentNavFavorite
              component={this.state.component.key}
              favorite={this.state.component.isFavorite}
              canBeFavorite={this.state.component.canBeFavorite}/>

          <ComponentNavBreadcrumbs
              breadcrumbs={this.state.component.breadcrumbs}/>

          <ComponentNavMeta
              {...this.state}
              version={this.state.component.version}
              snapshotDate={this.state.component.snapshotDate}/>

          <ComponentNavMenu
              component={this.state.component}
              conf={this.state.conf}/>
        </div>
    );
  }
});

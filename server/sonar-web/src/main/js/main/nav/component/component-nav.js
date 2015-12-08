import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import ReactDOM from 'react-dom';
import { STATUSES } from '../../../apps/background-tasks/constants';
import { getTasksForComponent } from '../../../api/ce';
import ComponentNavFavorite from './component-nav-favorite';
import ComponentNavBreadcrumbs from './component-nav-breadcrumbs';
import ComponentNavMeta from './component-nav-meta';
import ComponentNavMenu from './component-nav-menu';
import RecentHistory from './recent-history';

export default React.createClass({
  componentDidMount() {
    this.loadStatus();
    this.populateRecentHistory();
  },

  loadStatus() {
    getTasksForComponent(this.props.component.uuid).then(r => {
      this.setState({
        isPending: !!_.findWhere(r.queue, { status: STATUSES.PENDING }),
        isInProgress: !!_.findWhere(r.queue, { status: STATUSES.IN_PROGRESS }),
        isFailed: r.current && r.current.status === STATUSES.FAILED
      }, this.initTooltips);
    });
  },

  populateRecentHistory() {
    let qualifier = _.last(this.props.component.breadcrumbs).qualifier;
    if (['TRK', 'VW', 'DEV'].indexOf(qualifier) !== -1) {
      RecentHistory.add(this.props.component.key, this.props.component.name, qualifier.toLowerCase());
    }
  },

  initTooltips() {
    $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip({
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
              component={this.props.component.key}
              favorite={this.props.component.isFavorite}
              canBeFavorite={this.props.component.canBeFavorite}/>

          <ComponentNavBreadcrumbs
              breadcrumbs={this.props.component.breadcrumbs}/>

          <ComponentNavMeta
              {...this.props}
              {...this.state}
              version={this.props.component.version}
              snapshotDate={this.props.component.snapshotDate}/>

          <ComponentNavMenu
              component={this.props.component}
              conf={this.props.conf}/>
        </div>
    );
  }
});

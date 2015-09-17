import $ from 'jquery';
import React from 'react';
import ComponentNavFavorite from './component-nav-favorite';
import ComponentNavBreadcrumbs from './component-nav-breadcrumbs';
import ComponentNavMeta from './component-nav-meta';
import ComponentNavMenu from './component-nav-menu';

export default React.createClass({
  getInitialState() {
    return { component: {}, conf: {} };
  },

  componentDidMount() {
    this.loadDetails();
  },

  loadDetails() {
    const url = `${window.baseUrl}/api/navigation/component`;
    const data = { componentKey: this.props.componentKey };
    $.get(url, data).done(r => {
      this.setState({
        component: r,
        conf: r.configuration || {}
      });
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
              version={this.state.component.version}
              snapshotDate={this.state.component.snapshotDate}/>

          <ComponentNavMenu
              component={this.state.component}
              conf={this.state.conf}/>
        </div>
    );
  }
});

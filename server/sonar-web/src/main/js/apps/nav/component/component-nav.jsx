import React from 'react';
import ComponentNavFavorite from './component-nav-favorite';
import ComponentNavBreadcrumbs from './component-nav-breadcrumbs';
import ComponentNavMeta from './component-nav-meta';
import ComponentNavMenu from './component-nav-menu';
import RecentHistory from '../../../libs/recent-history';

const TOP_LEVEL_QUALIFIERS = ['TRK', 'VW', 'SVW', 'DEV'];

export default React.createClass({
  componentDidMount() {
    this.addToHistory();
  },

  addToHistory() {
    let qualifier = _.last(this.props.component.breadcrumbs).qualifier;
    if (TOP_LEVEL_QUALIFIERS.indexOf(qualifier) !== -1) {
      RecentHistory.add(
          this.props.component.key,
          this.props.component.name,
          qualifier.toLowerCase());
    }
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
              version={this.props.component.version}
              snapshotDate={this.props.component.snapshotDate}/>

          <ComponentNavMenu
              component={this.props.component}
              conf={this.props.component.configuration}/>
        </div>
    );
  }
});

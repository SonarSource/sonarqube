import React from 'react';
import Favorite from '../../../components/shared/favorite';

export default React.createClass({
  render() {
    if (!this.props.canBeFavorite) {
      return null;
    }
    return (
        <div className="navbar-context-favorite">
          <Favorite component={this.props.component} favorite={this.props.favorite}/>
        </div>
    );
  }
});

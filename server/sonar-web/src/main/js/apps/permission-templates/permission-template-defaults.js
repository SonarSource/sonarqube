import _ from 'underscore';
import React from 'react';
import QualifierIcon from '../../components/shared/qualifier-icon';

export default React.createClass({
  propTypes: {
    permissionTemplate: React.PropTypes.object.isRequired,
    topQualifiers: React.PropTypes.array.isRequired
  },

  renderIfSingleTopQualifier() {
    return (
        <ul className="list-inline nowrap spacer-bottom">
          <li>Default</li>
        </ul>
    );
  },

  renderIfMultipleTopQualifiers() {
    let defaults = this.props.permissionTemplate.defaultFor.map(qualifier => {
      return <li key={qualifier}><QualifierIcon qualifier={qualifier}/>&nbsp;{window.t('qualifier', qualifier)}</li>;
    });
    return (
        <ul className="list-inline nowrap spacer-bottom">
          <li>Default for</li>
          {defaults}
        </ul>
    );
  },

  render() {
    if (_.size(this.props.permissionTemplate.defaultFor) === 0) {
      return null;
    }
    return this.props.topQualifiers.length === 1 ?
        this.renderIfSingleTopQualifier() :
        this.renderIfMultipleTopQualifiers();
  }
});

import _ from 'underscore';
import React from 'react';
import { setDefaultPermissionTemplate } from '../../api/permissions';
import QualifierIcon from '../../components/shared/qualifier-icon';

export default React.createClass({
  propTypes: {
    permissionTemplate: React.PropTypes.object.isRequired,
    topQualifiers: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  getAvailableQualifiers() {
    return _.difference(this.props.topQualifiers, this.props.permissionTemplate.defaultFor);
  },

  setDefault(qualifier, e) {
    e.preventDefault();
    setDefaultPermissionTemplate(this.props.permissionTemplate.id, qualifier).done(() => this.props.refresh());
  },

  renderIfSingleTopQualifier(availableQualifiers) {
    let qualifiers = availableQualifiers.map(qualifier => {
      return (
          <span key={qualifier} className="text-middle">
            <a onClick={this.setDefault.bind(this, qualifier)} className="button" href="#">Set Default</a>
          </span>
      );
    });

    return <span className="little-spacer-right">{qualifiers}</span>;
  },

  renderIfMultipleTopQualifiers(availableQualifiers) {
    let qualifiers = availableQualifiers.map(qualifier => {
      return (
          <li key={qualifier}>
            <a onClick={this.setDefault.bind(this, qualifier)} href="#">
              Set Default for <QualifierIcon qualifier={qualifier}/> {window.t('qualifier', qualifier)}
            </a>
          </li>
      );
    });

    return (
        <span className="dropdown little-spacer-right">
          <button className="dropdown-toggle" data-toggle="dropdown">
            Set Default <i className="icon-dropdown"></i>
          </button>
          <ul className="dropdown-menu">{qualifiers}</ul>
        </span>
    );
  },

  render() {
    let availableQualifiers = this.getAvailableQualifiers();
    if (availableQualifiers.length === 0) {
      return null;
    }

    return this.props.topQualifiers.length === 1 ?
        this.renderIfSingleTopQualifier(availableQualifiers) :
        this.renderIfMultipleTopQualifiers(availableQualifiers);
  }
});

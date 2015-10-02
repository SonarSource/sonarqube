import React from 'react';
import QualifierIcon from '../../../components/shared/qualifier-icon';

export default React.createClass({
  render() {
    if (!this.props.breadcrumbs) {
      return null;
    }
    const items = this.props.breadcrumbs.map((item, index) => {
      const url = `${window.baseUrl}/dashboard/index?id=${encodeURIComponent(item.key)}`;
      return (
          <li key={index}>
            <a href={url}>
              <QualifierIcon qualifier={item.qualifier}/>&nbsp;{item.name}
            </a>
          </li>
      );
    });
    return (
        <ul className="nav navbar-nav nav-crumbs">{items}</ul>
    );
  }
});

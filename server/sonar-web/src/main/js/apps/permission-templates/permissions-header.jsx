import React from 'react';

export default React.createClass({
  propTypes: {
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  render() {
    let cellWidth = (80 / this.props.permissions.length) + '%';
    let cells = this.props.permissions.map(p => {
      return (
          <th key={p.key} style={{ width: cellWidth }}>
            {p.name}<br/><span className="small">{p.description}</span>
          </th>
      );
    });
    return (
        <thead>
        <tr>
          <th style={{ width: '20%' }}>&nbsp;</th>
          {cells}
          <th className="thin">&nbsp;</th>
        </tr>
        </thead>
    );
  }
});

import React from 'react';
import ItemValue from './item-value';

export default React.createClass({
  render() {
    let items = this.props.items.map(item => {
      return <tr key={item.name}>
        <td className="thin">
          <div style={{ width: '25vw', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.name}</div>
        </td>
        <td style={{ wordBreak: 'break-all' }}><ItemValue name={item.name} value={item.value}/></td>
      </tr>;
    });

    return <div className="big-spacer-bottom">
      <h3 className="spacer-bottom">{this.props.section}</h3>
      <table className="data zebra" id={this.props.section}>
        <tbody>{items}</tbody>
      </table>
    </div>;
  }
});

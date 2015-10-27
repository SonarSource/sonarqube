import React from 'react';
import ItemValue from './item-value';

export default React.createClass({
  render() {
    let rows = Object.keys(this.props.value).map(key => {
      return <tr key={key}>
        <td className="thin nowrap">{key}</td>
        <td><ItemValue value={this.props.value[key]}/></td>
      </tr>;
    });
    return <table className="data"><tbody>{rows}</tbody></table>;
  }
});

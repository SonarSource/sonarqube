import React from 'react';
import Assignee from '../../../components/shared/assignee-helper';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { formatMeasure } from '../../../helpers/measures';


export default class extends React.Component {
  render () {
    let rows = this.props.assignees.map(s => {
      let params = { statuses: 'OPEN,REOPENED' };
      if (s.val) {
        params.assignees = s.val;
      } else {
        params.assigned = 'false';
      }
      let href = getComponentIssuesUrl(this.props.component.key, params);
      return <tr key={s.val}>
        <td>
          <Assignee user={s.user}/>
        </td>
        <td className="thin text-right">
          <a href={href}>{formatMeasure(s.count, 'SHORT_INT')}</a>
        </td>
      </tr>;
    });

    return <table className="data zebra">
      <tbody>{rows}</tbody>
    </table>;
  }
}

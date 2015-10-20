import React from 'react';
import Assignee from '../../../components/shared/assignee-helper';
import { DomainHeader } from '../domain/header';
import { formatMeasure } from '../formatting';
import { componentIssuesUrl } from '../../../helpers/Url';

export default class extends React.Component {
  render () {
    let rows = this.props.assignees.map(s => {
      let href = componentIssuesUrl(this.props.component.key, { statuses: 'OPEN,REOPENED', assignees: s.val });
      return <tr key={s.val}>
        <td>
          <Assignee user={s.user}/>
        </td>
        <td className="thin text-right">
          <a href={href}>{formatMeasure(s.count, 'violations')}</a>
        </td>
      </tr>;
    });

    return <div className="overview-domain-section">
      <DomainHeader title="Issues to Review"/>
      <table className="data zebra">
        <tbody>{rows}</tbody>
      </table>
    </div>;
  }
}

import React from 'react';
import RadioToggle from '../../components/shared/radio-toggle';
import {STATUSES, CURRENTS} from './constants';

export default React.createClass({
  getCurrentsOptions() {
    return [
      { value: CURRENTS.ALL, label: 'All' },
      { value: CURRENTS.ONLY_CURRENTS, label: 'Only Currents' }
    ];
  },

  getStatusOptions() {
    return [
      { value: STATUSES.ALL, label: 'All' },
      { value: STATUSES.SUCCESS, label: 'Success' },
      { value: STATUSES.FAILED, label: 'Failed' },
      { value: STATUSES.CANCELED, label: 'Canceled' }
    ];
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <ul className="list-inline">
            <li>
              <RadioToggle options={this.getStatusOptions()} value={this.props.statusFilter}
                           name="background-task-status" onCheck={this.props.onStatusChange}/>
            </li>
            <li>
              <RadioToggle options={this.getCurrentsOptions()} value={this.props.currentsFilter}
                           name="background-task-currents" onCheck={this.props.onCurrentsChange}/>
            </li>
          </ul>
        </section>
    );
  }
});

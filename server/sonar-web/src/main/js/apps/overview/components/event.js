import React from 'react';
import moment from 'moment';

import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';


export const Event = React.createClass({
  propTypes: {
    event: React.PropTypes.shape({
      id: React.PropTypes.string.isRequired,
      date: React.PropTypes.object.isRequired,
      type: React.PropTypes.string.isRequired,
      name: React.PropTypes.string.isRequired,
      text: React.PropTypes.string
    })
  },

  mixins: [TooltipsMixin],

  render () {
    const { event } = this.props;
    return <li className="spacer-top">
      <p>
        <strong className="js-event-type">{window.t('event.category', event.type)}</strong>
        :&nbsp;
        <span className="js-event-name">{event.name}</span>
        { event.text && <i className="spacer-left icon-help" data-toggle="tooltip" title={event.text}/> }
      </p>
      <p className="note little-spacer-top js-event-date">{moment(event.date).format('LL')}</p>
    </li>;
  }
});

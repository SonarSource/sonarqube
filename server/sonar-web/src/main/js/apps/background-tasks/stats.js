import React from 'react';

import { formatDuration } from './helpers';
import { TooltipsMixin } from '../../components/mixins/tooltips-mixin';


export default React.createClass({
  mixins: [TooltipsMixin],

  onPendingCanceled(e) {
    e.preventDefault();
    this.props.cancelPending();
  },

  onFailuresClick(e) {
    e.preventDefault();
    this.props.showFailures();
  },

  renderInProgressDuration() {
    if (!this.props.inProgressDuration) {
      return null;
    }
    return (
        <span className="huge-spacer-left" title={window.t('background_tasks.in_progress_duration')}
              data-toggle="tooltip">
          <i className="spinner spacer-right" style={{ verticalAlign: 'text-top' }}/>
          <span ref="inProgressDuration" className="emphasised-measure">
            {formatDuration(this.props.inProgressDuration)}
          </span>
        </span>
    );
  },

  renderPending() {
    if (this.props.pendingCount == null) {
      return null;
    }
    if (this.props.pendingCount > 0) {
      return (
          <span>
            <span ref="pendingCount" className="emphasised-measure">{this.props.pendingCount}</span>
            &nbsp;
            {window.t('background_tasks.pending')}
            <a ref="cancelPending" onClick={this.onPendingCanceled} className="icon-delete spacer-left"
               title={window.t('background_tasks.cancel_all_tasks')} data-toggle="tooltip" href="#"></a>
          </span>
      );
    } else {
      return (
          <span>
            <span ref="pendingCount" className="emphasised-measure">{this.props.pendingCount}</span>
            &nbsp;
            {window.t('background_tasks.pending')}
          </span>
      );
    }
  },

  renderFailures() {
    if (this.props.failuresCount == null) {
      return null;
    }

    if (this.props.options && this.props.options.component) {
      return null;
    }

    if (this.props.failuresCount > 0) {
      return (
          <span>
            <a ref="failureCount"
               onClick={this.onFailuresClick}
               className="emphasised-measure"
               data-toggle="tooltip"
               title="Count of projects where processing of most recent analysis report failed"
               href="#">{this.props.failuresCount}</a>
            &nbsp;
            {window.t('background_tasks.failures')}
          </span>
      );
    } else {
      return (
          <span>
            <span ref="failureCount" className="emphasised-measure" data-toggle="tooltip"
                  title="Count of projects where processing of most recent analysis report failed">
              {this.props.failuresCount}
            </span>
            &nbsp;
            {window.t('background_tasks.failures')}
          </span>
      );
    }
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <span>
            {this.renderPending()}
          </span>
          <span className="huge-spacer-left">
            {this.renderFailures()}
          </span>
          {this.renderInProgressDuration()}
        </section>

    );
  }
});

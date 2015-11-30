import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';

export const TooltipsMixin = {
  componentDidMount () {
    this.initTooltips();
  },

  componentWillUpdate() {
    this.hideTooltips();
  },

  componentDidUpdate () {
    this.initTooltips();
  },

  componentWillUnmount() {
    this.destroyTooltips();
  },

  initTooltips () {
    if ($.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip({ container: 'body', placement: 'bottom', html: true });
    }
  },

  hideTooltips () {
    if ($.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('hide');
    }
  },

  destroyTooltips () {
    if ($.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('destroy');
    }
  }
};

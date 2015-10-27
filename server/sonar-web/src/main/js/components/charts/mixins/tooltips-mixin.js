import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';

export const TooltipsMixin = {
  componentDidMount () {
    this.initTooltips();
  },

  componentDidUpdate () {
    this.initTooltips();
  },

  initTooltips () {
    if ($.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip({ container: 'body', placement: 'bottom', html: true });
    }
  }
};

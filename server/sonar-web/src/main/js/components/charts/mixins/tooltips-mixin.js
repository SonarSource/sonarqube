import $ from 'jquery';
import React from 'react';

export const TooltipsMixin = {
  componentDidMount () {
    this.initTooltips();
  },

  componentDidUpdate () {
    this.initTooltips();
  },

  initTooltips () {
    if ($.fn.tooltip) {
      $('[data-toggle="tooltip"]', React.findDOMNode(this))
          .tooltip({ container: 'body', placement: 'bottom', html: true });
    }
  }
};

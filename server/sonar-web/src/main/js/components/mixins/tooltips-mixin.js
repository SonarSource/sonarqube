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
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip({ container: 'body', placement: 'bottom', html: true });
    }
  },

  hideTooltips () {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('hide');
    }
  },

  destroyTooltips () {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('destroy');
    }
  }
};


export const TooltipsContainer = React.createClass({
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
    if ($.fn && $.fn.tooltip) {
      const options = Object.assign({ container: 'body', placement: 'bottom', html: true }, this.props.options);
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip(options);
    }
  },

  hideTooltips () {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('hide');
    }
  },

  destroyTooltips () {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this))
          .tooltip('destroy');
    }
  },

  render () {
    return this.props.children;
  }
});

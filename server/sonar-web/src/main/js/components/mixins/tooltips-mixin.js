import $ from 'jquery';
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

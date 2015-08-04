import React from 'react';
import Main from './main';

const $ = jQuery;

export default {
  start: function (options) {
    $('html').addClass('dashboard-page');
    window.requestMessages().done(() => {
      var el = document.querySelector(options.el);
      React.render(<Main
          component={options.component}
          gate={options.gate}
          measures={options.measures}
          leak={options.leak}/>, el);
    });
  }
};

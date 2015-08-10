import React from 'react';
import Main from './main';

const $ = jQuery;

export default {
  start(options) {
    $('html').addClass('dashboard-page');
    window.requestMessages().done(() => {
      const el = document.querySelector(options.el);
      React.render(<Main
          component={options.component}
          gate={options.gate}
          measures={options.measures}
          leak={options.leak}/>, el);
    });
  }
};

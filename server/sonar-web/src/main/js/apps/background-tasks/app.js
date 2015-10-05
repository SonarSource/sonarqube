import React from 'react';
import Main from './main';

export default {
  start (options) {
    window.requestMessages().done(() => {
      let el = document.querySelector(options.el);
      React.render(<Main options={options}/>, el);
    });
  }
};

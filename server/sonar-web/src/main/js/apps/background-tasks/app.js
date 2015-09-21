import React from 'react';
import Main from './main';

export default {
  start (options) {
    var el = document.querySelector(options.el);
    React.render(<Main/>, el);
  }
};

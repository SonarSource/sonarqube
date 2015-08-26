import React from 'react';
import Main from './main';
import MainComponent from './main-component';

export default {
  start(options) {
    var el = document.querySelector(options.el);
    if (options.component) {
      React.render(<MainComponent component={options.component}/>, el);
    } else {
      React.render(<Main/>, el);
    }
  }
};

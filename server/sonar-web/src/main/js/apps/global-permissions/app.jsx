import React from 'react';
import Main from './main';

const $ = jQuery;

export default {
  start(options) {
    var el = document.querySelector(options.el);
    React.render(<Main/>, el);
  }
};

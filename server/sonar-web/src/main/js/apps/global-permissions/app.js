import React from 'react';
import ReactDOM from 'react-dom';
import Main from './main';

window.sonarqube.appStarted.then(options => {
  var el = document.querySelector(options.el);
  ReactDOM.render(<Main/>, el);
});

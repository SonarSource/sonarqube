import React from 'react';
import ReactDOM from 'react-dom';
import Main from './main';

window.sonarqube.appStarted.then(options => {
  let el = document.querySelector(options.el);
  ReactDOM.render(<Main options={options}/>, el);
});

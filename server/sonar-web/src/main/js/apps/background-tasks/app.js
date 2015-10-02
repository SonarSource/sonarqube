import React from 'react';
import Main from './main';

window.sonarqube.appStarted.then(options => {
  let el = document.querySelector(options.el);
  React.render(<Main options={options}/>, el);
});

import React from 'react';
import Main from './main';

window.sonarqube.appStarted.then(options => {
  var el = document.querySelector(options.el);
  React.render(<Main/>, el);
});



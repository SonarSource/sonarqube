import React from 'react';
import Main from './main';
import '../../helpers/handlebars-helpers';

window.sonarqube.appStarted.then(options => {
  var el = document.querySelector(options.el);
  React.render(<Main topQualifiers={options.rootQualifiers}/>, el);
});

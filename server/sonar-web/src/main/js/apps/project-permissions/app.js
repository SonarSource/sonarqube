import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import Main from './main';

function requestPermissionTemplates () {
  return $.get(baseUrl + '/api/permissions/search_templates');
}

window.sonarqube.appStarted.then(options => {
  requestPermissionTemplates().done(r => {
    var el = document.querySelector(options.el);
    ReactDOM.render(<Main permissionTemplates={r.permissionTemplates}
                          componentId={window.sonarqube.componentId}
                          rootQualifiers={options.rootQualifiers}/>, el);
  });
});

import React from 'react';
import ReactDOM from 'react-dom';
import Main from './main';
import { getCurrentUser } from '../../api/users';

window.sonarqube.appStarted.then(options => {
  getCurrentUser().done(user => {
    let el = document.querySelector(options.el);
    let hasProvisionPermission = user.permissions.global.indexOf('provisioning') !== -1;
    let topLevelQualifiers = options.rootQualifiers;
    ReactDOM.render(<Main hasProvisionPermission={hasProvisionPermission}
                       topLevelQualifiers={topLevelQualifiers}/>, el);
  });
});

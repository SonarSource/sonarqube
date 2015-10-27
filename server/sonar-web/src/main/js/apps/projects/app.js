import React from 'react';
import ReactDOM from 'react-dom';
import Main from './main';
import {getCurrentUser} from '../../api/users';
import '../../helpers/handlebars-helpers';

window.sonarqube.appStarted.then(options => {
  getCurrentUser().done(user => {
    let el = document.querySelector(options.el),
        hasProvisionPermission = user.permissions.global.indexOf('provisioning') !== -1,
        topLevelQualifiers = options.rootQualifiers;
    ReactDOM.render(<Main hasProvisionPermission={hasProvisionPermission}
                       topLevelQualifiers={topLevelQualifiers}/>, el);
  });
});

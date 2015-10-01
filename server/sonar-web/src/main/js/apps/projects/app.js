import $ from 'jquery';
import React from 'react';
import Main from './main';
import {getCurrentUser} from '../../api/users';
import {getGlobalNavigation} from '../../api/nav';

export default {
  start(options) {
    $.when(
        getCurrentUser(),
        getGlobalNavigation(),
        window.requestMessages()
    ).then((user, nav) => {
          let el = document.querySelector(options.el),
              hasProvisionPermission = user[0].permissions.global.indexOf('provisioning') !== -1,
              topLevelQualifiers = nav[0].qualifiers;
          React.render(<Main hasProvisionPermission={hasProvisionPermission}
                             topLevelQualifiers={topLevelQualifiers}/>, el);
        });
  }
};

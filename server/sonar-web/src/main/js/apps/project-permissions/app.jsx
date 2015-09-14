import $ from 'jquery';
import React from 'react';
import Main from './main';

let permissionTemplates = [];

export default {
  start(options) {
    $.when(
        window.requestMessages(),
        this.requestPermissionTemplates()
    ).then(() => {
          var el = document.querySelector(options.el);
          React.render(<Main permissionTemplates={permissionTemplates}/>, el);
        });
  },

  requestPermissionTemplates() {
    return $.get(baseUrl + '/api/permissions/search_templates').done(r => {
      permissionTemplates = r.permissionTemplates;
    });
  }
};

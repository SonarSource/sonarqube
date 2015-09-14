import $ from 'jquery';
import React from 'react';
import Main from './main';

let topQualifiers = [];

export default {
  start(options) {
    $.when(
        window.requestMessages(),
        this.requestTopQualifiers()
    ).then(() => {
          var el = document.querySelector(options.el);
          React.render(<Main topQualifiers={topQualifiers}/>, el);
        });
  },

  requestTopQualifiers() {
    return $.get(baseUrl + '/api/navigation/global').done(r => {
      topQualifiers = r.qualifiers;
    });
  }
};

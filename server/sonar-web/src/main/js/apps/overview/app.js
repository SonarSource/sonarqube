import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import Main from './main';

class App {
  start (options) {
    let opts = _.extend({}, options, window.sonarqube.overview);
    _.extend(opts.component, options.component);
    $('html').toggleClass('dashboard-page', opts.component.hasSnapshot);
    let el = document.querySelector(opts.el);
    React.render(<Main {...opts}/>, el);
  }
}

window.sonarqube.appStarted.then(options => new App().start(options));

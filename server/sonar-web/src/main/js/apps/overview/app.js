import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import ReactDOM from 'react-dom';

import { Overview, EmptyOverview } from './overview';


const LEAK_PERIOD = '1';


class App {
  start (options) {
    let opts = _.extend({}, options, window.sonarqube.overview);
    _.extend(opts.component, options.component);
    opts.urlRoot = window.baseUrl + '/overview';

    $('html').toggleClass('dashboard-page', opts.component.hasSnapshot);
    let el = document.querySelector(opts.el);

    if (opts.component.hasSnapshot) {
      ReactDOM.render(<Overview {...opts} leakPeriodIndex={LEAK_PERIOD}/>, el);
    } else {
      ReactDOM.render(<EmptyOverview/>, el);
    }
  }
}

window.sonarqube.appStarted.then(options => new App().start(options));

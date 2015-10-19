import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import Main from './main';
import Empty from './empty';

class App {
  start(options) {
    let opts = _.extend({}, options, window.sonarqube.overview);
    _.extend(opts.component, options.component);
    $('html').toggleClass('dashboard-page', opts.component.hasSnapshot);
    window.requestMessages().done(() => {
      let el = document.querySelector(opts.el);
      let inner = opts.component.hasSnapshot ? (
          <Main
              component={opts.component}
              gate={opts.gate}
              measures={opts.measures}
              leak={opts.leak}/>
      ) : <Empty/>;
      React.render(inner, el);
    });
  }
}

window.sonarqube.appStarted.then(options => new App().start(options));

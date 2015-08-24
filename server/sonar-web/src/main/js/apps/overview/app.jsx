import React from 'react';
import Main from './main';
import Empty from './empty';

const $ = jQuery;

export default {
  start(options) {
    _.extend(options, window.overview);
    $('html').toggleClass('dashboard-page', options.component.hasSnapshot);
    const el = document.querySelector(options.el);
    const inner = options.component.hasSnapshot ? (
        <Main
            component={options.component}
            gate={options.gate}
            measures={options.measures}
            leak={options.leak}/>
    ) : <Empty/>;
    React.render(inner, el);
  }
};

import React from 'react';
import { render } from 'react-dom';
import { Provider } from 'react-redux';

import Code from './components/Code';
import configureStore from './store/configureStore';


const store = configureStore();


window.sonarqube.appStarted.then(({ el, ...other }) => {
  render(
      <Provider store={store}>
        <Code {...other}/>
      </Provider>,
      document.querySelector(el));
});

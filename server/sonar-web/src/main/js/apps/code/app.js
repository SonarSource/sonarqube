import React from 'react';
import { render } from 'react-dom';
import { Provider } from 'react-redux';
import { Router, Route } from 'react-router';
import { createHashHistory } from 'history';
import { syncReduxAndRouter } from 'redux-simple-router';

import Code from './components/Code';
import configureStore from './store/configureStore';


const store = configureStore();
const history = createHashHistory({
  queryKey: false
});

syncReduxAndRouter(history, store);


window.sonarqube.appStarted.then(({ el, component }) => {
  const CodeWithComponent = () => {
    return <Code component={component}/>;
  };

  render(
      <Provider store={store}>
        <Router history={history}>
          <Route path="/" component={CodeWithComponent}/>
          <Route path="/:path" component={CodeWithComponent}/>
        </Router>
      </Provider>,
      document.querySelector(el));
});

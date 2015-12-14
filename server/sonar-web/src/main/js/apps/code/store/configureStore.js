import { createStore, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';

import rootReducer from '../reducers';


const createStoreWithMiddleware = applyMiddleware(
    thunk,
    createLogger()
)(createStore);


export default function configureStore () {
  return createStoreWithMiddleware(rootReducer);
}

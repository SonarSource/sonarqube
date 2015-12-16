import { createStore, applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';

import rootReducer from '../reducers';

const logger = createLogger({
  predicate: () => process.env.NODE_ENV === 'development'
});

const createStoreWithMiddleware = applyMiddleware(
    thunk,
    logger
)(createStore);


export default function configureStore () {
  return createStoreWithMiddleware(rootReducer);
}

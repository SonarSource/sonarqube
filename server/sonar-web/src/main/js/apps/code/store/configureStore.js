import { createStore, applyMiddleware, combineReducers } from 'redux';
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';
import { routeReducer } from 'redux-simple-router';
import { current, bucket } from '../reducers';

const logger = createLogger({
  predicate: () => process.env.NODE_ENV !== 'production'
});

const createStoreWithMiddleware = applyMiddleware(
    thunk,
    logger
)(createStore);

const reducer = combineReducers({
  routing: routeReducer,
  current,
  bucket
});

export default function configureStore () {
  return createStoreWithMiddleware(reducer);
}

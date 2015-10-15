import 'babelify/polyfill';
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import 'whatwg-fetch';
import moment from 'moment';
import '../libs/third-party/backbone-super.js';
import './processes';
import Navigation from './nav/app';

// set the Backbone's $
Backbone.$ = $;


function requestLocalizationBundle () {
  if (!window.sonarqube.bannedNavigation) {
    return new Promise(resolve => window.requestMessages().done(resolve));
  } else {
    return Promise.resolve();
  }
}

function startNavigation () {
  if (!window.sonarqube.bannedNavigation) {
    return new Navigation().start();
  } else {
    return Promise.resolve();
  }
}

function prepareAppOptions (navResponse) {
  let appOptions = { el: '#content' };
  if (navResponse) {
    appOptions.rootQualifiers = navResponse.global.qualifiers;
    if (navResponse.component) {
      appOptions.component = {
        id: navResponse.component.uuid,
        key: navResponse.component.key,
        name: navResponse.component.name,
        qualifier: _.last(navResponse.component.breadcrumbs).qualifier
      };
    }
  }
  return appOptions;
}

function getPreferredLanguage () {
  return window.navigator.languages ? window.navigator.languages[0] : window.navigator.language;
}

moment.locale(getPreferredLanguage());

window.sonarqube.appStarted = Promise.resolve()
    .then(requestLocalizationBundle)
    .then(startNavigation)
    .then(prepareAppOptions);

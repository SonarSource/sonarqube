/* globals global: false */

var jsdom = require('jsdom');

// A super simple DOM ready for React to render into
// Store this DOM and the window in global scope ready for React to access
global.document = jsdom.jsdom('<!doctype html><html><body></body></html>');
global.window = document.defaultView;
global.navigator = document.defaultView.navigator;

global.window.baseUrl = '';
global.window.t = global.window.tp = function () {
  var args = Array.prototype.slice.call(arguments, 0);
  return args.join('.');
};

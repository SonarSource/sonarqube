require.config({
  baseUrl: window.baseUrl + '/js',
  urlArgs: 'v=' + window.sonarVersion,
  paths: {
    'react': 'libs/third-party/react-with-addons',
    'underscore': 'libs/shim/underscore-shim',
    'jquery': 'libs/shim/jquery-shim',
    'backbone': 'libs/third-party/backbone',
    'backbone.marionette': 'libs/third-party/backbone.marionette'
  }
});

require([
  './apps/main/app',
  './components/common/processes'
], function (App) {
  new App({
    space: window.space,
    componentKey: window.component,
    lang: window.pageLang
  }).start();
});

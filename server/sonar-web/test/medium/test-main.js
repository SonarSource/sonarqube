require.config({
  baseUrl: '../../build/js',
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
  './components/common/processes',
  'libs/third-party/jquery.mockjax'
], function (App) {
  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 50;

  jQuery.mockjax({ url: '/api/l10n/index', responseText: '{}' });
  jQuery.mockjax({ url: '/api/users/current', responseText: '{ "isLoggedIn":true, "login":"admin", "name":"Administrator", "permissions": { "global": ["provisioning", "dryRunScan", "shareDashboard", "scan", "profileadmin", "admin"] } }' });
  jQuery.mockjax({ url: '/api/navigation/component', responseText: '{"key":"org.codehaus.sonar:sonar","uuid":"uuid","name":"SonarQube","isComparable":true,"canBeFavorite":true,"isFavorite":true,"dashboards":[{"key":109,"name":"Dev"},{"key":1,"name":"Dashboard"},{"key":2,"name":"SQALE"},{"key":8,"name":"Hotspots"},{"key":88,"name":"Issues"},{"key":18,"name":"TimeMachine"},{"key":13,"name":"QA"},{"key":59,"name":"By Developers"}],"version":"5.2-SNAPSHOT","snapshotDate":"2015-08-25T10:37:21+0200","extensions":[],"breadcrumbs":[{"key":"org.codehaus.sonar:sonar","name":"SonarQube","qualifier":"TRK"}]}' });

  window.App = new App({
    space: window.space,
    componentKey: window.component,
    lang: window.pageLang
  });
  window.App.start();

  jQuery('#content').addClass('ready');
});

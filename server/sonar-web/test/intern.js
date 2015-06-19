/* jshint node:true */
define(['intern'], function (intern) {
  var useBrowserStack = intern.args.useBrowserStack,
      tunnel = useBrowserStack ? 'BrowserStackTunnel' : 'NullTunnel';

  return {
    excludeInstrumentation: /(test|third-party|node_modules)\//,

    defaultTimeout: 60 * 1000,

    reporters: [
      { id: 'Runner' },
      { id: 'Lcov' },
      { id: 'LcovHtml', directory: 'target/web-tests' }
    ],

    suites: [
      'test/unit/application.spec'
    ],

    functionalSuites: [
      'test/medium/users.spec'
    ],

    tunnel: tunnel,
    environments: [
      { browserName: 'firefox' }
    ]
  };
});

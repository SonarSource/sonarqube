/* jshint node:true */
define(['intern'], function (intern) {
  var useBrowserStack = intern.args.useBrowserStack,
      tunnel = useBrowserStack ? 'BrowserStackTunnel' : 'NullTunnel';

  return {
    excludeInstrumentation: true,

    defaultTimeout: 60 * 1000,

    reporters: [
      { id: 'Runner' },
      { id: 'Lcov' },
      { id: 'LcovHtml', directory: 'target/web-tests' }
    ],

    suites: [],

    functionalSuites: [
      'test/medium/api-documentation.spec',
      'test/medium/coding-rules.spec',
      'test/medium/custom-measures.spec',
      'test/medium/global-permissions.spec',
      'test/medium/groups.spec',
      'test/medium/issues.spec',
      'test/medium/maintenance.spec',
      'test/medium/metrics.spec',
      'test/medium/project-permissions.spec',
      'test/medium/quality-gates.spec',
      'test/medium/quality-profiles.spec',
      'test/medium/source-viewer.spec',
      'test/medium/update-center.spec',
      'test/medium/users.spec'
    ],

    tunnel: tunnel,
    environments: [
      { browserName: 'firefox' }
    ],

    loaderOptions: {
      paths: {
        'react': '../../build/js/libs/third-party/react-with-addons',
        'underscore': '../../build/js/libs/third-party/shim/underscore-shim',
        'jquery': '../../build/js/libs/third-party/shim/jquery-shim',
        'backbone': '../../build/js/libs/third-party/shim/backbone-shim',
        'backbone.marionette': '../../build/js/libs/third-party/shim/marionette-shim'
      },
      map: {
        '*': {
          'components/shared/qualifier-icon': '../../build/js/components/shared/qualifier-icon'
        }
      }
    }
  };
});

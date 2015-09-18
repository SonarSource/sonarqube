/* jshint node:true */
define(['intern'], function (intern) {
  var useBrowserStack = intern.args.useBrowserStack,
      tunnel = useBrowserStack ? 'BrowserStackTunnel' : 'NullTunnel';

  return {
    excludeInstrumentation: /(((test|third-party|node_modules)\/)|(templates.js$))/,

    defaultTimeout: 60 * 1000,

    reporters: [
      { id: 'Runner' },
      { id: 'Lcov' },
      { id: 'LcovHtml', directory: 'target/web-tests' }
    ],

    suites: [
      'test/unit/application.spec',
      'test/unit/issue.spec',
      'test/unit/code-with-issue-locations-helper.spec',
      'test/unit/nav/component/component-nav-breadcrumbs.spec'
    ],

    functionalSuites: [
      'test/medium/users.spec',
      'test/medium/issues.spec',
      'test/medium/update-center.spec',
      'test/medium/computation.spec',
      'test/medium/coding-rules.spec',
      'test/medium/custom-measures.spec',
      'test/medium/quality-profiles.spec',
      'test/medium/source-viewer.spec',
      'test/medium/global-permissions.spec',
      'test/medium/project-permissions.spec'
    ],

    tunnel: tunnel,
    environments: [
      { browserName: 'firefox' }
    ],

    loaderOptions: {
      paths: {
        'react': '../../build/js/libs/third-party/react-with-addons'
      },
      map: {
        '*': {
          'components/shared/qualifier-icon': '../../build/js/components/shared/qualifier-icon'
        }
      }
    }
  };
});

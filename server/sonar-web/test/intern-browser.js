/* jshint node:true */
define({
  excludeInstrumentation: /(test|third-party|node_modules)\//,

  defaultTimeout: 60 * 1000,

  suites: [
    'test/unit/application.spec',
    'test/unit/issue.spec'
  ],

  tunnel: 'NullTunnel',
  environments: [
    { browserName: 'firefox' }
  ],

  loaderOptions: {
    paths: {
      'react': 'build/js/libs/third-party/react-with-addons'
    }
  }
});

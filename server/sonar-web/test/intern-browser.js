/* jshint node:true */
define({
  excludeInstrumentation: /(test|third-party|node_modules)\//,

  defaultTimeout: 60 * 1000,

  suites: [
    'test/unit/application.spec'
  ],

  tunnel: 'NullTunnel',
  environments: [
    { browserName: 'firefox' }
  ]
});

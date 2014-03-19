({
  appDir: '.',
  baseUrl: '.',
  dir: 'build',
  preserveLicenseComments: false,
  skipDirOptimize: true,

  modules: [
  	{ name: 'quality-gate/app' },
  	{ name: 'issues/app' },
  	{ name: 'measures/app' },
  	{ name: 'common/select-list' }
  ],

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars',
    'moment': 'third-party/moment',
    'select-list': 'common/select-list'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    },
    'moment': {
      exports: 'moment'
    },
    'select-list': {
      exports: 'SelectList'
    }
  }
})

({
  appDir: '.',
  baseUrl: '.',
  dir: 'DEFINED IN POM.XML',

  modules: [
  	{ name: 'quality-gate/app' },
  	{ name: 'issues' },
  	{ name: 'measures' }
  ],

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars',
    'moment': 'third-party/moment',
    'select-list': 'select-list'
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

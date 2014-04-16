requirejs.config
  baseUrl: "#{baseUrl}/js"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'
    'jquery.mockjax': 'third-party/jquery.mockjax'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'


requirejs [
  'component-viewer/main',
], (
  ComponentViewer
) ->

  TEST_RESOURCE_KEY = 'org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/resources/ResourceTypeTree.java'


  @componentViewer = new ComponentViewer()
  @componentViewer.render().$el.appendTo '#body'

  @componentViewer.open TEST_RESOURCE_KEY
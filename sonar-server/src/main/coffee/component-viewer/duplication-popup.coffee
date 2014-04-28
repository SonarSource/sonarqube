define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class DuplicationPopupView extends Popup
    template: Templates['duplicationPopup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      @options.main.addTransition key, 'duplication', [
        {
          key: 'org.codehaus.sonar:sonar-plugin-api:src/test/java/org/sonar/api/resources/ResourceTypeTree.java'
          name: 'ResourceTypeTree.java'
          active: true
        }
        {
          key: 'org.codehaus.sonar:sonar-batch:src/main/java/org/sonar/batch/phases/PhaseExecutor.java'
          name: 'PhaseExecutor.java'
          active: false
        }
      ], []


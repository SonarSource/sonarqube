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


  class CoveragePopupView extends Popup
    template: Templates['coveragePopup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      test = $(e.currentTarget).data 'test'
      @options.main.addTransition key, 'coverage', [
        {
          key: 'org.codehaus.sonar:sonar-plugin-api:src/test/java/org/sonar/api/resources/ResourceTypeTreeTest.java'
          name: 'forbidDuplicatedType'
          subname: 'ResourceTypeTreeTest.java'
          active: test == 'forbidDuplicatedType'
        }
        {
          key: 'org.codehaus.sonar:sonar-plugin-api:src/test/java/org/sonar/api/resources/ResourceTypeTreeTest.java'
          name: 'forbidNullRelation'
          subname: 'ResourceTypeTreeTest.java'
          active: test == 'forbidNullRelation'
        }
        {
          key: 'org.codehaus.sonar:sonar-plugin-api:src/test/java/org/sonar/api/resources/ResourceTypeTest.java'
          name: 'fail_on_duplicated_qualifier'
          subname: 'ResourceTypeTest.java'
          active: test == 'fail_on_duplicated_qualifier'
        }
      ], [
        {
          key: key,
          name: test
        }
      ]

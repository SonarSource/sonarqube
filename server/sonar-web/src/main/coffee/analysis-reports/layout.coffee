define [
  'templates/analysis-reports'
], ->

  class extends Marionette.Layout
    template: Templates['analysis-reports-layout']



    regions:
      actionsRegion: '.analysis-reports-actions'
      resultsRegion: '.analysis-reports-results'


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')

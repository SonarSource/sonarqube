define [
  'backbone.marionette'
], (
  Marionette
) ->

  class extends Marionette.ItemView
    className: 'search-navigator-no-results'


    template: ->
      t 'issue_filter.no_issues'

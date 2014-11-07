define [
  'backbone.marionette'
], (
  Marionette
) ->

  class extends Marionette.ItemView
    className: 'issues-no-results'


    template: ->
      t 'issue_filter.no_issues'

define [
  'backbone.marionette'
], (
  Marionette
) ->

  class extends Marionette.ItemView
    className: 'issues-no-results'
    tagName: 'li'


    template: ->
      t 'issue_filter.no_issues'

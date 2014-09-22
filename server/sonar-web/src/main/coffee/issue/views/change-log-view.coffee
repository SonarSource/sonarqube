define [
  'backbone.marionette'
  'templates/issue'
], (
  Marionette
  Templates
) ->

  class IssueDetailChangeLogView extends Marionette.ItemView
    template: Templates['change-log']


    collectionEvents:
      'sync': 'render'


    serializeData: ->
      _.extend super,
        issue: @options.issue.toJSON()

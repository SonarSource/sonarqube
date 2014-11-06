define [
  'backbone.marionette'
  'templates/issues'
  'issues/issue-view'
], (
  Marionette
  Templates
  IssueBoxView
) ->

  class extends Marionette.ItemView
    tagName: 'li'
    className: 'issue-box'
    template: Templates['issues-workspace-list-item']


    events:
      'click .js-issues-to-source': 'openComponentViewer'


    initialize: (options) ->
      @listenTo options.app.state, 'change:selectedIndex', @select


    onRender: ->
      @issueBoxView = new IssueBoxView model: @model
      @$('.issue-box-details').append @issueBoxView.render().el
      @select()


    select: ->
      selected = @options.index == @options.app.state.get 'selectedIndex'
      @$el.toggleClass 'selected', selected


    onClose: ->
      @issueBoxView?.close()


    openComponentViewer: ->
      @options.app.state.set selectedIndex: @options.index
      @options.app.controller.showComponentViewer @model

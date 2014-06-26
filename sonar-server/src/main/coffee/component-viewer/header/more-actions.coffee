define [
  'backbone.marionette'
  'templates/component-viewer'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    className: 'component-viewer-header-more-actions'
    template: Templates['more-actions']


    events:
      'click .js-new-window': 'openNewWindow'
      'click .js-full-source': 'showFullSource'
      'click .js-raw-source': 'showRawSource'
      'click .js-extension': 'showExtension'


    onRender: ->
      $('body').on 'click.component-viewer-more-actions', =>
        $('body').off 'click.component-viewer-more-actions'
        @close()


    openNewWindow: ->
      @options.main.headerView.getPermalink()


    showFullSource: ->
      @options.main.showAllLines()


    showRawSource: ->
      @options.main.showRawSources()

    showExtension: (e) ->
      key = $(e.currentTarget).data 'key'
      @options.main.headerView.showExtension key


    serializeData: ->
      _.extend super,
        state: @options.main.state.toJSON()
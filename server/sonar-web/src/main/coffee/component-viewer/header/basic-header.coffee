define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
], (
  Marionette
  Templates
  BaseHeaderView
) ->


  class extends BaseHeaderView
    template: Templates['cw-basic-header']


    events:
      'click .js-filter-lines': 'filterByLines'
      'click .js-filter-ncloc': 'filterByNcloc'


    filterByLines: (e) ->
      @header.filterLines e, 'filterByLines'
      @state.set 'activeHeaderItem', '.js-filter-lines'


    filterByNcloc: (e) ->
      @header.filterLines e, 'filterByNcloc'
      @state.set 'activeHeaderItem', '.js-filter-ncloc'

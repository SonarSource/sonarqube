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
    template: Templates['duplications-header']


    events:
      'click .js-filter-duplications': 'filterByDuplications'


    filterByDuplications: (e) ->
      @header.filterLines e, 'filterByDuplications'
      @state.set 'activeHeaderItem', '.js-filter-duplications'

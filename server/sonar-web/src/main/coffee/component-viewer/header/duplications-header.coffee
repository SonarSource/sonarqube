define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
], (
  Marionette
  Templates
  BaseHeaderView
) ->

  $ = jQuery


  class extends BaseHeaderView
    template: Templates['cw-duplications-header']


    events:
      'click .js-filter-duplications': 'filterByDuplications'


    filterByDuplications: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByDuplications'
      @state.set 'activeHeaderItem', '.js-filter-duplications'

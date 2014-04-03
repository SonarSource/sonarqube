define [
  'backbone.marionette',
  'coding-rules/views/coding-rules-list-item-view',
  'coding-rules/views/coding-rules-list-empty-view'
], (
  Marionette,
  CodingRulesListItemView,
  CodingRulesListEmptyView
) ->

  class CodingRulesListView extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: CodingRulesListItemView,
    emptyView: CodingRulesListEmptyView,


    itemViewOptions: ->
      listView: @, app: @options.app


    initialize: ->
      openRule = (el) -> el.click()
      @openRule = _.debounce openRule, 300
      key.setScope 'list'


    onRender: ->
      key 'up', 'list', => @selectPrev()
      key 'down', 'list', => @selectNext()


    selectIssue: (el, open) ->
      @$('.active').removeClass 'active'
      el.addClass 'active'
      @openRule el if open


    selectFirst: ->
      @selected = -1
      @selectNext()


    selectNext: ->
      if @selected < @collection.length - 1
        @selected++
        child = @$el.children().eq @selected
        container = jQuery('.navigator-results')
        containerHeight = container.height()
        bottom = child.position().top + child.outerHeight()
        if bottom > containerHeight
          container.scrollTop(container.scrollTop() - containerHeight + bottom)
        @selectIssue child, true


    selectPrev: ->
      if @selected > 0
        @selected--
        child = @$el.children().eq @selected
        container = jQuery('.navigator-results')
        top = child.position().top
        if top < 0
          container.scrollTop(container.scrollTop() + top)
        @selectIssue child, true
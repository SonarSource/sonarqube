define [
  'backbone.marionette',
  'coding-rules-old/views/coding-rules-list-item-view',
  'coding-rules-old/views/coding-rules-list-empty-view'
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
      key 'up', 'list', (e) =>
        @selectPrev()
        #e.stopPropagation()
      key 'down', 'list', (e) =>
        @selectNext()
        #e.stopPropagation()

      $scrollEl = jQuery('.navigator-results')
      scrollEl = $scrollEl.get(0)
      onScroll = =>
        if scrollEl.offsetHeight + scrollEl.scrollTop >= scrollEl.scrollHeight
          @options.app.fetchNextPage()
      throttledScroll = _.throttle onScroll, 300
      $scrollEl.off('scroll').on 'scroll', throttledScroll


    onClose: ->
      @unbindEvents()


    unbindEvents: ->
      key.unbind 'up', 'list'
      key.unbind 'down', 'list'


    selectIssue: (el, open) ->
      @$('.active').removeClass 'active'
      el.addClass 'active'
      ruleKey = el.find('[name]').attr('name')
      rule = @collection.findWhere key: ruleKey
      @selected = @collection.indexOf(rule)
      @openRule el if open


    selectFirst: ->
      @selected = -1
      @selectNext()


    selectCurrent: ->
      @selected--
      @selectNext()


    selectNext: ->
      if @selected + 1 < @collection.length
        @selected += 1
        child = @$el.children().eq(@selected)
        container = jQuery('.navigator-results')
        containerHeight = container.height()
        bottom = child.position().top + child.outerHeight()
        if bottom > containerHeight
          container.scrollTop(container.scrollTop() - containerHeight + bottom)
        @selectIssue child, true


    selectPrev: ->
      if @selected > 0
        @selected -= 1
        child = @$el.children().eq(@selected)
        container = jQuery('.navigator-results')
        top = child.position().top
        if top < 0
          container.scrollTop(container.scrollTop() + top)
        @selectIssue child, true

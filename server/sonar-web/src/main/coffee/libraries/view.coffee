define [
  'backbone.marionette'
  'templates/libraries'
], (
  Marionette,
  Templates
) ->

  $ = jQuery



  class extends Marionette.ItemView
    template: Templates['libraries']


    ui:
      filter: '.js-libraries-filter'
      collapseAll: '.js-libraries-collapse-all'
      expandAll: '.js-libraries-expand-all'


    events:
      'change .js-test-libraries': 'toggleTestLibraries'
      'click @ui.collapseAll': 'collapseAll'
      'click @ui.expandAll': 'expandAll'
      'click .libraries-tree-with-subtree > .libraries-tree-name': 'toggleSubTree'


    onRender: ->
      @toggleActions()
      filterFn = _.bind @filter, @
      debouncedFilterFn = _.debounce filterFn, 250
      @ui.filter.off('keyup').on 'keyup', debouncedFilterFn


    filter: (e) ->
      term = $(e.currentTarget).val()
      @expandAll()
      @$('.libraries-tree li').addClass 'libraries-tree-subtree-hidden'
      @$('.libraries-tree li').each (index, el) =>
        name = $(el).children('.libraries-tree-name').text()
        @showTree $(el) if name.match new RegExp(term, 'i')


    showTree: (el) ->
      el.removeClass 'libraries-tree-subtree-hidden'
      el.parents('.libraries-tree-subtree-hidden').removeClass 'libraries-tree-subtree-hidden'


    toggleTestLibraries: ->
      @$('.libraries-tree').toggleClass 'libraries-tree-show-tests'


    collapseAll: ->
      @$('.libraries-tree-with-subtree').addClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    expandAll: ->
      @$('.libraries-tree-with-subtree').removeClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    toggleSubTree: (e) ->
      tree = $(e.currentTarget).parent()
      tree.toggleClass 'libraries-tree-subtree-collapsed'
      @toggleActions()


    toggleActions: ->
      subTreesCount = @$('.libraries-tree-with-subtree').length
      subTreesCollapsedCount = @$('.libraries-tree-subtree-collapsed').length
      @ui.collapseAll.toggle subTreesCount > subTreesCollapsedCount
      @ui.expandAll.toggle subTreesCollapsedCount > 0


    serializeData: ->
      _.extend super,
        usagesUrl: "#{baseUrl}/dependencies/index?search=#{window.resourceKey}"


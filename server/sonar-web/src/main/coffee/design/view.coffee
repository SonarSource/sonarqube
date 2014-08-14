define [
  'backbone.marionette',
  'design/info-view',
  'templates/design'
], (
  Marionette,
  InfoView
  Templates
) ->

  $ = jQuery
  API_DEPENDECIES = "#{baseUrl}/api/dependencies"



  class extends Marionette.Layout
    template: Templates['design']
    className: 'dsm'


    regions:
      infoRegion: '.dsm-info'


    ui:
      titles: '.dsm-body-title'
      cells: '.dsm-body-cell'
      dependencies: '.dsm-body-dependency'


    events:
      'click @ui.titles': 'highlightComponent'
      'dblclick @ui.titles': 'goToComponent'
      'click @ui.cells': 'highlightCell'
      'dblclick @ui.dependencies': 'showDependencies'


    clearCells: ->
      @ui.titles.removeClass 'dsm-body-highlighted dsm-body-usage dsm-body-dependency'
      @ui.cells.removeClass 'dsm-body-highlighted dsm-body-usage dsm-body-dependency'


    highlightComponent: (e) ->
      index = @ui.titles.index $(e.currentTarget)
      @clearCells()
      @highlightRow index
      @highlightColumn index
      @highlightUsages index
      @highlightDependencies index


    highlightCell: (e) ->
      cell = $(e.currentTarget)
      column = cell.parent().children().index(cell) - 1
      row = cell.parent().parent().children().index cell.parent()
      @clearCells()
      if row == column
        @highlightRow row
        @highlightColumn row
        @highlightUsages row
        @highlightDependencies row
      else
        @highlightRow column, 'dsm-body-usage'
        @highlightColumn column, 'dsm-body-usage'
        @highlightRow row, 'dsm-body-dependency'
        @highlightColumn row, 'dsm-body-dependency'


    highlightRow: (index, c = 'dsm-body-highlighted') ->
      @$(".dsm-body tr:eq(#{index}) td").addClass c


    highlightColumn: (index, c = 'dsm-body-highlighted') ->
      @$(".dsm-body tr").each ->
        $(this).find("td:eq(#{index + 1})").addClass c


    highlightUsages: (index) ->
      @collection.at(index).get('v').forEach (d, i) =>
        if i < index && d.w?
          @$("tr:eq(#{i}) .dsm-body-title").addClass 'dsm-body-usage'


    highlightDependencies: (index) ->
      @collection.forEach (model, i) =>
        if model.get('v')[index].w?
          @$("tr:eq(#{i}) .dsm-body-title").addClass 'dsm-body-dependency'


    goToComponent: (e) ->
      cell = $(e.currentTarget)
      row = cell.parent().parent().children().index cell.parent()
      model = @collection.at(row)
      page = if model.get('q') == 'CLA' || model.get('q') == 'FIL' then 'dashboard' else 'design'
      window.location = "#{baseUrl}/#{page}/index/#{model.get 'i'}"


    showDependencies: (e) ->
      cell = $(e.currentTarget)
      column = cell.parent().children().index(cell) - 1
      row = cell.parent().parent().children().index cell.parent()
      id = @collection.at(row).get('v')[column].i
      @showInfoViewSpinner()
      @scrollToInfoView()
      $.get API_DEPENDECIES, parent: id, (data) =>
        @infoRegion.show new InfoView
          collection: new Backbone.Collection data
          first: @collection.at(column).toJSON()
          second: @collection.at(row).toJSON()
        @scrollToInfoView()


    showInfoViewSpinner: ->
      @infoRegion.reset()
      @$(@infoRegion.el).html '<i class="spinner"></i>'


    scrollToInfoView: ->
      delta = @$(@infoRegion.el).offset().top - 40
      $('html, body').animate { scrollTop: delta }, 500

define [
  'backbone.marionette',
  'templates/design'
], (
  Marionette,
  Templates
) ->

  $ = jQuery



  class AppLayout extends Marionette.Layout
    template: Templates['design']
    className: 'dsm'


    ui:
      titles: '.dsm-body-title'


    events:
      'click @ui.titles': 'highlightComponent'


    highlightComponent: (e) ->
      e.preventDefault()
      index = @ui.titles.index $(e.currentTarget)
      @$('.dsm-body-highlighted').removeClass 'dsm-body-highlighted'
      @$('.dsm-body-usage').removeClass 'dsm-body-usage'
      @$('.dsm-body-dependency').removeClass 'dsm-body-dependency'
      @highlightRow index
      @highlightColumn index
      @highlightUsages index
      @highlightDependencies index



    highlightRow: (index) ->
      @$(".dsm-body tr:eq(#{index}) td").addClass 'dsm-body-highlighted'


    highlightColumn: (index) ->
      @$(".dsm-body tr").each ->
        $(this).find("td:eq(#{index + 1})").addClass 'dsm-body-highlighted'


    highlightUsages: (index) ->
      @collection.at(index).get('v').forEach (d, i) =>
        if i < index && d.w?
          @$("tr:eq(#{i})").addClass 'dsm-body-usage'


    highlightDependencies: (index) ->
      @collection.forEach (model, i) =>
        if model.get('v')[index].w?
          @$("tr:eq(#{i})").addClass 'dsm-body-dependency'


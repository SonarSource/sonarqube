define [
  'backbone.marionette',
  'handlebars',
], (
  Marionette,
  Handlebars,
) ->

  class QualityGateNewView extends Marionette.ItemView
    className: 'quality-gate'
    template: Handlebars.compile jQuery('#quality-gate-new-template').html()


    ui:
      input: '#quality-gate-renaming-input'
      header: '.quality-gate-header'


    events:
      'click #quality-gate-save': 'save'
      'click #quality-gate-cancel-save': 'cancel'


    onDomRefresh: ->
      @ui.input.focus()


    save: ->
      @showHeaderSpinner()
      name = @ui.input.val()
      jQuery.ajax
        url: "#{baseUrl}/api/qualitygates/create"
        type: 'POST'
        data: name: name
      .done (r) =>
        @model.set r
        @options.app.qualityGates.add @model
        @options.app.router.navigate "show/#{@model.id}", trigger: true


    cancel: ->
      @options.app.openFirstQualityGate()


    showHeaderSpinner: ->
      @ui.header.addClass 'navigator-fetching'


    hideHeaderSpinner: ->
      @ui.header.removeClass 'navigator-fetching'

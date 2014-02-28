define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars,
) ->

  class QualityGateEditView extends Marionette.ItemView
    className: 'modal'
    template: Handlebars.compile jQuery('#quality-gate-edit-template').html()


    ui:
      nameInput: '#quality-gate-edit-name'


    events:
      'submit form': 'onSubmit'
      'click #quality-gate-cancel-create': 'hide'


    onRender: ->
      @$el.dialog
        dialogClass: 'no-close',
        width: '600px',
        draggable: false,
        autoOpen: false,
        modal: true,
        minHeight: 50,
        resizable: false,
        title: null


    show: ->
      @render()
      @$el.dialog 'open'
      @ui.nameInput.focus()


    hide: ->
      @$el.dialog 'close'


    saveRequest: (method, data) ->
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualitygates/#{method}"
        data: data
      .done => @hide()


    onSubmit: (e) ->
      e.preventDefault()
      if @model.isNew()
        @createQualityGate()
      else
        @saveQualityGate()

    createQualityGate: ->
      data = name: @ui.nameInput.val()
      @saveRequest('create', data).done (r) =>
        @model.set id: r.id, name: r.name
        @options.app.qualityGates.add @model
        @options.app.router.navigate "show/#{r.id}", trigger: true


    saveQualityGate: ->
      data = id: @model.id, name: @ui.nameInput.val()
      @saveRequest('rename', data).done (r) =>
        @model.set name: r.name

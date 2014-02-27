define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars,
) ->

  class QualityGateDetailRenamingView extends Marionette.ItemView
    template: Handlebars.compile jQuery('#quality-gate-detail-renaming-template').html()


    ui:
      input: '#quality-gate-renaming-input'


    events:
      'click #quality-gate-rename': 'rename'
      'click #quality-gate-cancel-rename': 'cancel'


    onDomRefresh: ->
      @ui.input.focus()


    rename: ->
      @options.detailView.showHeaderSpinner()
      newName = @ui.input.val()
      jQuery.ajax({
        url: "#{baseUrl}/api/qualitygates/rename"
        type: 'POST'
        data:
          id: @model.id
          name: newName
      }).done =>
        @model.set 'name', newName
        @options.detailView.showHeader()
        @options.detailView.hideHeaderSpinner()


    cancel: ->
      @options.detailView.showHeader()

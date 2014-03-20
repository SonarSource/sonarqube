define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesDetailQualityProfilesView extends Marionette.ItemView
    className: 'coding-rules-detail-quality-profile'
    template: getTemplate '#coding-rules-detail-quality-profile-template'


    ui:
      update: '.coding-rules-detail-quality-profile-update'
      severitySelect: '.coding-rules-detail-quality-profile-severity'

      note: '.coding-rules-detail-quality-profile-note'
      noteForm: '.coding-rules-detail-quality-profile-note-form'
      noteText: '.coding-rules-detail-quality-profile-note-text'
      noteAdd: '.coding-rules-detail-quality-profile-note-add'
      noteEdit: '.coding-rules-detail-quality-profile-note-edit'
      noteDelete: '.coding-rules-detail-quality-profile-note-delete'
      noteCancel: '.coding-rules-detail-quality-profile-note-cancel'
      noteSubmit: '.coding-rules-detail-quality-profile-note-submit'


    events:
      'click @ui.noteAdd': 'editNote'
      'click @ui.noteEdit': 'editNote'
      'click @ui.noteDelete': 'deleteNote'
      'click @ui.noteCancel': 'cancelNote'
      'click @ui.noteSubmit': 'submitNote'

      'change .coding-rules-detail-parameters select': 'enableUpdate'
      'keyup .coding-rules-detail-parameters input': 'enableUpdate'


    editNote: ->
      @ui.note.hide()
      @ui.noteForm.show()
      @ui.noteText.focus()


    deleteNote: ->
      @ui.noteText.val ''
      @submitNote().done =>
        @model.unset 'note'
        @render()


    cancelNote: ->
      @ui.note.show()
      @ui.noteForm.hide()


    submitNote: ->
      @ui.note.html '<i class="spinner"></i>'
      @ui.noteForm.html '<i class="spinner"></i>'
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/note"
        dataType: 'json'
        data: text: @ui.noteText.val()
      .done (r) =>
        @model.set 'note', r.note
        @render()


    enableUpdate: ->
      @ui.update.prop 'disabled', false


    onRender: ->
      @ui.noteForm.hide()

      format = (state) ->
        return state.text unless state.id
        "<i class='icon-severity-#{state.id.toLowerCase()}'></i> #{state.text}"

      @ui.severitySelect.val @model.get 'severity'
      @ui.severitySelect.select2
        width: '200px'
        minimumResultsForSearch: 999
        formatResult: format
        formatSelection: format
        escapeMarkup: (m) -> m


    getParent: ->
      return null unless @model.get 'inherits'
      @options.qualityProfiles.findWhere(key: @model.get('inherits')).toJSON()


    enhanceParameters: ->
      parent = @getParent()
      parameters = @model.get 'parameters'
      return parameters unless parent
      parameters.map (p) ->
        _.extend p, original: _.findWhere(parent.parameters, key: p.key).value


    serializeData: ->
      _.extend super,
        parent: @getParent()
        parameters: @enhanceParameters()
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
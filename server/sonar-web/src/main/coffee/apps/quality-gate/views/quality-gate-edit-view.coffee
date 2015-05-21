#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  '../templates'
], ->

  class QualityGateEditView extends Marionette.ItemView
    template: Templates['quality-gate-edit']


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


    saveRequest: (data) ->
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualitygates/#{@method}"
        data: data
      .done => @hide()


    onSubmit: (e) ->
      e.preventDefault()
      switch @method
        when 'create' then @createQualityGate()
        when 'copy' then @copyQualityGate()
        when 'rename' then @saveQualityGate()
        else


    createQualityGate: ->
      data = name: @ui.nameInput.val()
      @saveRequest(data).done (r) =>
        @model.set id: r.id, name: r.name
        @options.app.qualityGates.add @model
        @options.app.router.navigate "show/#{r.id}", trigger: true


    saveQualityGate: ->
      data = id: @model.id, name: @ui.nameInput.val()
      @saveRequest(data).done (r) =>
        @model.set name: r.name


    copyQualityGate: ->
      data = id: @model.id, name: @ui.nameInput.val()
      @saveRequest(data).done (r) =>
        @model.set id: r.id, name: r.name
        @options.app.qualityGates.add @model
        @options.app.router.navigate "show/#{r.id}", trigger: true


    serializeData: ->
      if @model
        _.extend @model.toJSON(), method: @method

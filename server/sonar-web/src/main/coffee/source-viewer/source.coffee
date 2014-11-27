define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model
    idAttribute: 'uuid'

    defaults: ->
      hasSource: false
      hasCoverage: false
      hasDuplications: false
      hasSCM: false


    key: ->
      @get 'key'


    addMeta: (meta) ->
      source = @get 'source'
      metaIdx = 0
      metaLine = meta[metaIdx]
      source.forEach (line) ->
        while metaLine? && line.line > metaLine.line
          metaIdx++
          metaLine = meta[metaIdx]
        if metaLine? && line.line == metaLine.line
          _.extend line, metaLine
          metaIdx++
          metaLine = meta[metaIdx]
      @set source: source


    addDuplications: (duplications) ->
      source = @get 'source'
      return unless source
      source.forEach (line) ->
        lineDuplications = []
        duplications.forEach (d, i) ->
          duplicated = false
          d.blocks.forEach (b) ->
            if b._ref == '1'
              lineFrom = b.from
              lineTo = b.from + b.size
              duplicated = true if line.line >= lineFrom && line.line <= lineTo
          lineDuplications.push if duplicated then i + 1 else false
        line.duplications = lineDuplications
      @set source: source


class Treemap extends window.SonarWidgets.BaseWidget
  sizeLow: 11
  sizeMedium: 13
  sizeHigh: 24


  constructor: ->
    @addField 'width', null
    @addField 'height', null
    @addField 'maxResultsReached', false
    super


  getNodes: ->
    @treemap.nodes(children: @components()).filter (d) -> !d.children


  renderTreemap: ->
    nodes = @getNodes()
    @cells = @box.selectAll('.treemap-cell').data nodes
    @cells.exit().remove()

    cellsEnter = @cells.enter().append 'div'
    cellsEnter.classed 'treemap-cell', true
    cellsEnter.append('div').classed 'treemap-inner', true

    @cells.attr 'title', (d) => @tooltip d
    @cells.style 'background-color', (d) =>
      if @colorMetric.value(d)? then @color @colorMetric.value(d) else @colorUnknown

    @cellsInner = @box.selectAll('.treemap-inner').data nodes
    @cellsInner.text (d) -> d.longName

#    cellsLink = cellsEnter.append('a').classed 'treemap-dashboard', true
#    cellsLink.attr 'href', (d) =>
#      url = @options().baseUrl + encodeURIComponent(d.key)
#      url += '?metric=' + encodeURIComponent(@colorMetric.key) if d.qualifier == 'CLA' || d.qualifier == 'FIL'
#      url
#    cellsLink.append('i').attr 'class', (d) -> "icon-qualifier-#{d.qualifier.toLowerCase()}"

    @attachEvents cellsEnter


  attachEvents: (cells) ->
    cells.on 'click', (d) =>
      @requestChildren d.key


  positionCells: ->
    @cells.style 'left', (d) -> "#{d.x}px"
    @cells.style 'top', (d) -> "#{d.y}px"
    @cells.style 'width', (d) -> "#{d.dx}px"
    @cells.style 'height', (d) -> "#{d.dy}px"
    @cells.style 'line-height', (d) -> "#{d.dy}px"
    @cells.style 'font-size', (d) => "#{@size d.dx}px"
#    @cells.classed 'treemap-cell-small', (d) -> d.dy < 60
#    @cells.classed 'treemap-cell-very-small', (d) -> d.dx < 20 || d.dy < 20


  renderLegend: (box) ->
    @legend = box.insert 'div', ':first-child'
    @legend.classed 'legend', true
    @legend.classed 'legend-html', true
    @legend.append('span').classed('legend-text', true).html "Size: <span class='legend-text-main'>#{@sizeMetric.name}</span>"
    @legend.append('span').classed('legend-text', true).html "Color: <span class='legend-text-main'>#{@colorMetric.name}</span>"

    # Show maxResultsReached message
    if @maxResultsReached()
      maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
      maxResultsReachedLabel.classed 'max-results-reached-message', true


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    @box = box.append('div').classed 'treemap-container', true

    # Configure metrics
    @addMetric 'colorMetric', 0
    @addMetric 'sizeMetric', 1

    # Configure scales
    @color = d3.scale.linear().domain([0, 25, 50, 75, 100])
    if @colorMetric.direction == 1
      @color.range @colors5
    else
      @color.range @colors5r
    @size = d3.scale.linear().domain([80, 300]).range([@sizeLow, @sizeHigh]).clamp true

    @treemap = d3.layout.treemap()
    @treemap.value (d) => @sizeMetric.value d

    @renderLegend box
    @renderTreemap()
    super


  update: ->
    @width @box.property 'offsetWidth'
    @height (@width() / 100.0 * @options().heightInPercents)
    @box.style 'height', "#{@height()}px"
    @treemap.size [@width(), @height()]
    @cells.data @getNodes()
    @positionCells()


  stopDrilldown: ->



  requestChildren: (key) ->
    metrics = @metricsPriority().join ','
    RESOURCES_URL = "#{baseUrl}/api/resources/index"
    jQuery.get(RESOURCES_URL, resource: key, depth: 1, metrics: metrics).done (r) =>
      components = _.filter r, (component) =>
        hasSizeMetric = => _.findWhere component.msr, key: @sizeMetric.key
        _.isArray(component.msr) && component.msr.length > 0 && hasSizeMetric()

      if _.isArray(components) && components.length > 0
        components = components.map (component) =>
          measures = {}
          component.msr.forEach (measure) ->
            measures[measure.key] = val: measure.val, fval: measure.frmt_val

          key: component.key
          name: component.name
          longName: component.lname
          qualifier: component.qualifier
          measures: measures
        @components components
        @renderTreemap()
        @positionCells()
      else @stopDrilldown()



window.SonarWidgets.Treemap = Treemap

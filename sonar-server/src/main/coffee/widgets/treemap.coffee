class Treemap extends window.SonarWidgets.BaseWidget
  sizeLow: 10
  sizeHigh: 24


  constructor: ->
    @addField 'width', null
    @addField 'height', null
    @addField 'maxResultsReached', false
    super


  getNodes: ->
    @treemap.nodes(children: @components()).filter (d) -> !d.children


  renderTreemap: ->
    @treemap = d3.layout.treemap()
    @treemap.value (d) =>
      @sizeMetric.value d
    @cells = @box.selectAll('.treemap-cell').data @getNodes()

    cellsEnter = @cells.enter().append 'div'
    cellsEnter.classed 'treemap-cell', true
    cellsEnter.attr 'title', (d) => @tooltip d
    cellsEnter.style 'color', (d) =>
      if @colorMetric.value(d)? then @color @colorMetric.value(d) else @colorUnknown
    cellsEnter.style 'font-size', (d) => "#{@size @sizeMetric.value d}px"

    cellsLink = cellsEnter.append('a').classed 'treemap-dashboard', true
    cellsLink.attr 'href', (d) =>
      url = @options().baseUrl + encodeURIComponent(d.key)
      url += '?metric=' + encodeURIComponent(@colorMetric.key) if d.qualifier == 'CLA' || d.qualifier == 'FIL'
      url
    cellsLink.append('i').attr 'class', (d) -> "icon-qualifier-#{d.qualifier.toLowerCase()}"

    @cellsInner = cellsEnter.append('div').classed 'treemap-inner', true
    @cellsInner.text (d) -> d.longName
    @cellsInner.style 'border-color', (d) =>
      if @colorMetric.value(d)? then @color @colorMetric.value(d) else @colorUnknown

    @attachEvents cellsEnter


  attachEvents: (cells) ->


  positionCells: ->
    @cells.style 'left', (d) -> "#{d.x}px"
    @cells.style 'top', (d) -> "#{d.y}px"
    @cells.style 'width', (d) -> "#{d.dx}px"
    @cells.style 'height', (d) -> "#{d.dy}px"
    @cells.classed 'treemap-cell-small', (d) -> d.dy < 60
    @cells.classed 'treemap-cell-very-small', (d) -> d.dx < 20 || d.dy < 20
    @cellsInner.style 'line-height', (d) -> "#{d.dy}px"


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    @box = box.append('div').classed 'treemap-container', true

    # Configure metrics
    @addMetric 'colorMetric', 0
    @addMetric 'sizeMetric', 1

    # Configure scales
    @color = d3.scale.linear().domain([0, 100])
    if @colorMetric.direction == 1
      @color.range [@colorLow, @colorHigh]
    else
      @color.range [@colorHigh, @colorLow]

    sizeDomain = d3.extent @components(), (d) => @sizeMetric.value d
    @size = d3.scale.linear().domain(sizeDomain).range [@sizeLow, @sizeHigh]

    # Show maxResultsReached message
    if @maxResultsReached()
      maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
      maxResultsReachedLabel.classed 'max-results-reached-message', true

    @renderTreemap()
    super


  update: ->
    @width @box.property 'offsetWidth'
    @height (@width() / 100.0 * @options().heightInPercents)
    @box.style 'height', "#{@height()}px"
    @treemap.size [@width(), @height()]
    @cells.data @getNodes()
    @positionCells()



window.SonarWidgets.Treemap = Treemap

class TagCloud extends window.SonarWidgets.BaseWidget
  sizeLow: 10
  sizeHigh: 24


  constructor: ->
    @addField 'width', []
    @addField 'height', []
    @addField 'tags', []
    @addField 'maxResultsReached', false
    super


  renderWords: ->
    window.requestMessages().done =>
      words = @wordContainer.selectAll('.cloud-word').data @tags()

      wordsEnter = words.enter().append('a').classed 'cloud-word', true
      wordsEnter.text (d) -> d.key
      wordsEnter.attr 'href', (d) =>
        url = @options().baseUrl + '|tags=' + d.key
        if @options().createdAfter
          url += '|createdAfter=' + @options().createdAfter
        url
      wordsEnter.attr 'title', (d) => @tooltip d

      words.style 'font-size', (d) =>
        "#{@size d.value}px"

      words.sort (a, b) =>
        if a.key.toLowerCase() > b.key.toLowerCase() then 1 else -1


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    box.classed 'cloud-widget', true
    @wordContainer = box.append 'div'

    sizeDomain = d3.extent @tags(), (d) => d.value
    @size = d3.scale.linear().domain(sizeDomain).range [@sizeLow, @sizeHigh]

    # Show maxResultsReached message
    if @maxResultsReached()
      maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
      maxResultsReachedLabel.classed 'max-results-reached-message', true

    @renderWords()

    super


  tooltip: (d) ->
    suffixKey = if d.value == 1 then 'issue' else 'issues'
    suffix = t(suffixKey)
    "#{d.value}\u00a0" + suffix


  parseSource: (response) ->
    @tags(response.tags)


window.SonarWidgets.TagCloud = TagCloud

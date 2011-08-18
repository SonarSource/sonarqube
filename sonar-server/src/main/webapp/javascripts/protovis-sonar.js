function displayTrendChart(divId, data) {

	/* Sizing and scales. */
	var w = 400 - 40, 
		h = 300 - 25,
		S=2;


	var x = pv.Scale.linear(pv.blend(pv.map(data, function(d) d)), function(d) d.x).range(0, w);
	var y = new Array(data.length);
	for(var i = 0; i < data.length; i++){ 
		y[i]=pv.Scale.linear(data[i], function(d) d.y).range(0, h)
	}
	var interpolate = "linear"; /* cardinal or linear */
	var idx = -1;

	/* The root panel. */
	var vis = new pv.Panel()
	.canvas(document.getElementById(divId))
	.width(w)
	.height(h)
	.left(30)
	.right(10)
	.bottom(20)
	.top(5)
	.strokeStyle("#CCC");

	/* X-axis */
	vis.add(pv.Rule)
	.data(x.ticks())
	.left(x)
	.bottom(-5)
	.height(5)
	.anchor("bottom")
	.add(pv.Label)
	.text(x.tickFormat);

	/* Y-axis and ticks. */
	var show_y_axis = (data.length==1)
	if (show_y_axis) { 
		vis.add(pv.Rule)
		.data(y[0].ticks(5))
		.bottom(y[0])
		.strokeStyle(function(d) d ? "#eee" : "#000")
		.anchor("left")
		.add(pv.Label)
		.text(y[0].tickFormat); 
	}

	/* A panel for each data series. */
	var panel = vis.add(pv.Panel)
	.data(data);

	/* The line. */
	var line = panel.add(pv.Line)
	.data(function(array) array)
	.left(function(d) x(d.x))
	.bottom(function(d) y[this.parent.index](d.y))
	.interpolate(function() interpolate)
	.lineWidth(2);

	/* The mouseover dots and label. */
	line.add(pv.Dot)
	.visible(function() idx >= 0)
	.data(function(d) [d[idx]])
	.fillStyle(function() line.strokeStyle())
	.strokeStyle("#000")
	.size(20) 
	.lineWidth(1)
	.add(pv.Dot)
	.left(10)
	.bottom(function() this.parent.index * 12 + 10)
	.anchor("right").add(pv.Label)
	.text(function(d) d.y.toFixed(2));


	/* An invisible bar to capture events (without flickering). */
	vis.add(pv.Bar)
	.fillStyle("rgba(0,0,0,.001)")
	.event("mouseout", function() {
		i = -1;
		return vis;
	})
	.event("mousemove", function() {
		var mx = x.invert(vis.mouse().x);
		idx = pv.search(data[0].map(function(d) d.x), mx);
		idx = idx < 0 ? (-idx - 2) : idx;
		return vis;
	});
	vis.render();

}
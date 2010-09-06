#
# image_output.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require 'RMagick'
require File.dirname(__FILE__) + "/output_base"

class Treemap::ImageOutput < Treemap::OutputBase
    def initialize
        super

        # default options for ImageOutput

        yield self if block_given?
    end

    def setup_draw
        draw = Magick::Draw.new
        draw.stroke_width(1)
        draw.stroke("#000000")
        draw.stroke_opacity(1)
        draw.fill_opacity(1)
        draw.font_family = "Verdana"
        draw.pointsize = 12
        draw.gravity = Magick::WestGravity

        return draw
    end

    def new_image
        Magick::Image.new(@width, @height) {self.background_color = "white"}
    end

    def to_png(node, filename="treemap.png")
        #
        # XXX Need to flesh out this method. Add in label drawing.
        #

        image = self.new_image
        draw = self.setup_draw

        @bounds = self.bounds

        # Pad for root border
        @bounds.x2 -= 1
        @bounds.y2 -= 1

        @layout.process(node, @bounds)

        draw_map(node, draw, image)

        # render image
        draw.draw(image)
        image.write(filename)
    end

    def draw_map(node, draw, image)
        return "" if node.nil?
        if(node.color.nil?)
            draw.fill("#CCCCCC")
        else
            draw.fill("#" + @color.get_hex_color(node.color))
        end
        draw.rectangle(node.bounds.x1, node.bounds.y1, node.bounds.x2, node.bounds.y2)
        node.children.each do |c|
            draw_map(c, draw, image)
        end
    end
end

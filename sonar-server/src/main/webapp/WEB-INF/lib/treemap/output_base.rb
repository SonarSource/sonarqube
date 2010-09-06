#
# output_base.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require 'rexml/document'

module Treemap
    class OutputBase
        attr_accessor(:width, :height, :layout, :color, :margin_top, :margin_left)

        def initialize
            @width = 800
            @height = 600
            @margin_top = 0
            @margin_left = 0
            @layout = Treemap::SquarifiedLayout.new
            @color = Treemap::GradientColor.new
            yield self if block_given?
        end

    protected
        def bounds
            x1 = self.margin_left
            y1 = self.margin_top
            x2 = self.width + self.margin_left
            y2 = self.height + self.margin_top
            bounds = Treemap::Rectangle.new(x1, y1, x2, y2)
            return bounds
        end
    end
end

#
# rectangle.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

module Treemap
    class Rectangle
        attr_accessor :x1, :y1, :x2, :y2

        def initialize(x1, y1, x2, y2)
            @x1 = x1
            @y1 = y1
            @x2 = x2
            @y2 = y2

            yield self if block_given?
        end

        def to_s
            "[" + [@x1, @y1, @x2, @y2].join(",") + "]"
        end

        def width
            @x2 - @x1
        end

        def height
            @y2 - @y1
        end
    end
end

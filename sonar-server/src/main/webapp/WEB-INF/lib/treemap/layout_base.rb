#--
# layout_base.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#++

module Treemap
    class LayoutBase
        attr_accessor :position, :color

        def initialize
            # Similar to the css style position. If set to :fixed x,y bounds calculations
            # should be computed relative to the root bounds. If set to :absolute then they
            # should be computed relative to the parent bounds.
            # See http://www.w3.org/TR/CSS2/visuren.html#positioning-scheme
            @position = :fixed
            yield self if block_given?
        end

        # Subclasses will override
        def process(node, bounds)
        end
    end
end

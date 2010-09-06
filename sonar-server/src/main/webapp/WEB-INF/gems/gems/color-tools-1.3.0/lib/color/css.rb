#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: css.rb,v 1.1 2005/08/05 23:07:20 austin Exp $
#++

require 'color'

  # This namespace contains some CSS colour names.
module Color::CSS
    # Returns the RGB colour for name or +nil+ if the name is not valid.
  def self.[](name)
    @colors[name.to_s.downcase.to_sym]
  end

  @colors = {}
  Color::RGB.constants.each do |const|
    next if const == "PDF_FORMAT_STR"
    next if const == "Metallic"
    @colors[const.downcase.to_sym] ||= Color::RGB.const_get(const)
  end
end

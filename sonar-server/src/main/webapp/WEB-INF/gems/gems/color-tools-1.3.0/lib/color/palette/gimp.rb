#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: gimp.rb,v 1.3 2005/08/08 02:44:17 austin Exp $
#++

require 'color/palette'

  # A class that can read a GIMP (GNU Image Manipulation Program) palette
  # file and provide a Hash-like interface to the contents. GIMP colour
  # palettes are RGB values only.
  #
  # Because two or more entries in a GIMP palette may have the same name,
  # all named entries are returned as an array.
  #
  #   pal = Color::Palette::Gimp.from_file(my_gimp_palette)
  #   pal[0]          => Color::RGB<...>
  #   pal["white"]    => [ Color::RGB<...> ]
  #   pal["unknown"]  => [ Color::RGB<...>, Color::RGB<...>, ... ]
  #
  # GIMP Palettes are always indexable by insertion order (an integer key).
class Color::Palette::Gimp
  include Enumerable

  class << self
      # Create a GIMP palette object from the named file.
    def from_file(filename)
      File.open(filename, "rb") { |io| Color::Palette::Gimp.from_io(io) }
    end

      # Create a GIMP palette object from the provided IO.
    def from_io(io)
      Color::Palette::Gimp.new(io.read)
    end
  end

    # Create a new GIMP palette.
  def initialize(palette)
    @colors   = []
    @names    = {}
    @valid    = false
    @name     = "(unnamed)"

    index     = 0

    palette.split($/).each do |line|
      line.chomp!
      line.gsub!(/\s*#.*\Z/, '')

      next if line.empty?

      if line =~ /\AGIMP Palette\Z/
        @valid = true
        next
      end

      info = /(\w+):\s(.*$)/.match(line)
      if info
        @name = info.captures[1] if info.captures[0] =~ /name/i
        next
      end

      line.gsub!(/^\s+/, '')
      data = line.split(/\s+/, 4)
      name = data.pop.strip
      data.map! { |el| el.to_i }

      color = Color::RGB.new(*data)

      @colors[index]  = color
      @names[name] ||= []
      @names[name]  << color

      index += 1
    end
  end

  def [](key)
    if key.kind_of?(Numeric)
      @colors[key]
    else
      @names[key]
    end
  end

    # Loops through each colour.
  def each
    @colors.each { |el| yield el }
  end

    # Loops through each named colour set.
  def each_name #:yields color_name, color_set:#
    @names.each { |color_name, color_set| yield color_name, color_set }
  end

    # Returns true if this is believed to be a valid GIMP palette.
  def valid?
    @valid
  end

  attr_reader :name
end

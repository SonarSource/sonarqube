#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: cmyk.rb,v 1.5 2005/08/08 02:44:17 austin Exp $
#++

  # An CMYK colour object. CMYK (cyan, magenta, yellow, and black) colours
  # are based on additive percentages of ink. A CMYK colour of (0.3, 0, 0.8,
  # 0.3) would be mixed from 30% cyan, 0% magenta, 80% yellow, and 30%
  # black.
class Color::CMYK
    # The format of a DeviceCMYK colour for PDF. In color-tools 2.0 this
    # will be removed from this package and added back as a modification by
    # the PDF::Writer package.
  PDF_FORMAT_STR = "%.3f %.3f %.3f %.3f %s"

    # Compares the other colour to this one. The other colour will be
    # converted to CMYK before comparison, so the comparison between a CMYK
    # colour and a non-CMYK colour will be approximate and based on the
    # other colour's #to_cmyk conversion. If there is no #to_cmyk
    # conversion, this will raise an exception. This will report that two
    # CMYK colours are equivalent if all component values are within 1e-4
    # (0.0001) of each other.
  def ==(other)
    other = other.to_cmyk
    other.kind_of?(Color::CMYK) and
    ((@c - other.c).abs <= 1e-4) and
    ((@m - other.m).abs <= 1e-4) and
    ((@y - other.y).abs <= 1e-4) and
    ((@k - other.k).abs <= 1e-4)
  end

    # Creates a CMYK colour object from fractional values 0..1.
    #
    #   Color::CMYK.from_fraction(0.3, 0, 0.8, 0.3)
  def self.from_fraction(c = 0, m = 0, y = 0, k = 0)
    colour = Color::CMYK.new
    colour.c = c
    colour.m = m
    colour.y = y
    colour.k = k
    colour
  end

    # Creates a CMYK colour object from percentages. Internally, the colour
    # is managed as fractional values 0..1.
    #
    #   Color::CMYK.from_fraction(30, 0, 80, 30)
  def initialize(c = 0, m = 0, y = 0, k = 0)
    @c = c / 100.0
    @m = m / 100.0
    @y = y / 100.0
    @k = k / 100.0
  end

    # Present the colour as a DeviceCMYK fill colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_fill
    PDF_FORMAT_STR % [ @c, @m, @y, @k, "k" ]
  end

    # Present the colour as a DeviceCMYK stroke colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_stroke
    PDF_FORMAT_STR % [ @c, @m, @y, @k, "K" ]
  end

    # Present the colour as an RGB HTML/CSS colour string. Note that this
    # will perform a #to_rgb operation using the default conversion formula.
  def html
    to_rgb.html
  end

    # Converts the CMYK colour to RGB. Most colour experts strongly suggest
    # that this is not a good idea (some even suggesting that it's a very
    # bad idea). CMYK represents additive percentages of inks on white
    # paper, whereas RGB represents mixed colour intensities on a black
    # screen.
    #
    # However, the colour conversion can be done, and there are two
    # different methods for the conversion that provide slightly different
    # results. Adobe PDF conversions are done with the first form.
    #
    #     # Adobe PDF Display Formula
    #   r = 1.0 - min(1.0, c + k)
    #   g = 1.0 - min(1.0, m + k)
    #   b = 1.0 - min(1.0, y + k)
    #
    #     # Other
    #   r = 1.0 - (c * (1.0 - k) + k)
    #   g = 1.0 - (m * (1.0 - k) + k)
    #   b = 1.0 - (y * (1.0 - k) + k)
    #
    # If we have a CMYK colour of [33% 66% 83% 25%], the first method will
    # give an approximate RGB colour of (107, 23, 0) or #6b1700. The second
    # method will give an approximate RGB colour of (128, 65, 33) or
    # #804121. Which is correct? Although the colours may seem to be
    # drastically different in the RGB colour space, they are very similar
    # colours, differing mostly in intensity. The first is a darker,
    # slightly redder brown; the second is a lighter brown.
    #
    # Because of this subtlety, both methods are now offered for conversion
    # in color-tools 1.2 or later. The Adobe method is not used by default;
    # to enable it, pass +true+ to #to_rgb.
    #
    # Future versions of color-tools may offer other conversion mechanisms
    # that offer greater colour fidelity.
  def to_rgb(use_adobe_method = false)
    if use_adobe_method
      r = 1.0 - [1.0, @c + @k].min
      g = 1.0 - [1.0, @m + @k].min
      b = 1.0 - [1.0, @y + @k].min
    else
      r = 1.0 - (@c.to_f * (1.0 - @k.to_f) + @k.to_f)
      g = 1.0 - (@m.to_f * (1.0 - @k.to_f) + @k.to_f)
      b = 1.0 - (@y.to_f * (1.0 - @k.to_f) + @k.to_f)
    end
    Color::RGB.from_fraction(r, g, b)
  end

    # Converts the CMYK colour to a single greyscale value. There are
    # undoubtedly multiple methods for this conversion, but only a minor
    # variant of the Adobe conversion method will be used:
    #
    #   g = 1.0 - min(1.0, 0.299 * c + 0.587 * m + 0.114 * y + k)
    #
    # This treats the CMY values similarly to YIQ (NTSC) values and then
    # adds the level of black. This is a variant of the Adobe version
    # because it uses the more precise YIQ (NTSC) conversion values for Y
    # (intensity) rather than the approximates provided by Adobe (0.3, 0.59,
    # and 0.11).
  def to_grayscale
    c = 0.299 * @c.to_f
    m = 0.587 * @m.to_f
    y = 0.114 * @y.to_f
    g = 1.0 - [1.0, c + m + y + @k].min
    Color::GrayScale.from_fraction(g)
  end
  alias to_greyscale to_grayscale

  def to_cmyk
    self
  end

    # Converts to RGB then YIQ.
  def to_yiq
    to_rgb.to_yiq
  end

    # Converts to RGB then HSL.
  def to_hsl
    to_rgb.to_hsl
  end

  attr_accessor :c, :m, :y, :k
  remove_method :c=, :m=, :y=, :k= ;
  def c=(cc) #:nodoc:
    cc = 1.0 if cc > 1
    cc = 0.0 if cc < 0
    @c = cc
  end
  def m=(mm) #:nodoc:
    mm = 1.0 if mm > 1
    mm = 0.0 if mm < 0
    @m = mm
  end
  def y=(yy) #:nodoc:
    yy = 1.0 if yy > 1
    yy = 0.0 if yy < 0
    @y = yy
  end
  def k=(kk) #:nodoc:
    kk = 1.0 if kk > 1
    kk = 0.0 if kk < 0
    @k = kk
  end
end

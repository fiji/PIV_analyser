import fiji.plugin.pivanalyser.PIV_analyser as PIV_analyser
import fiji.plugin.pivanalyser.Stopwatch as Stopwatch
from ij import ImagePlus
from ij import IJ
from ij import WindowManager
from ij.process import ColorProcessor

DISPLAY_COLOR_WHEEL = True
SHOW_RESULTS = True

# couple first and second image.
pairs = [ [ 1, 2 ] ] 

# Block size
blocksize = PIV_analyser.WINDOW_SIZE._32x32 

# Get image.
imp = WindowManager.getCurrentImage()
if imp is None:
	IJ.error( 'Please open an image with at least 2 frames first.' )

# Initialize the plugin.
piv = PIV_analyser()
piv.setup( "", imp )

# Setup image pair.
piv.setImagePairs( pairs )

# Setup window size.
piv.setWinsize( blocksize )

# Set interpolation.
piv.setInterpolation( True )

# Run it and time it.
print( 'Running the plugin...' )
stopwatch = Stopwatch()
stopwatch.start()

# If SHOW_RESULTS is False, the image outputs won't be shown.
outputs = piv.exec( SHOW_RESULTS )

# Unwrap outputs. You can do whatever you want with them, they are ImagePlus.
velocity_u = outputs[ 0 ]
velocity_v = outputs[ 1 ]
peak_height = outputs[ 2 ]
color_scale = outputs[ 3] 

stopwatch.stop()
print( "Done in: " + str( stopwatch ) )

if DISPLAY_COLOR_WHEEL:
	cp = ColorProcessor(256, 256)
	PIV_analyser.colorCircle(cp)
	ImagePlus("Color coded orientation", cp).show()

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.ArrayIndexOutOfBoundsException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

/**
 * <h3>PIV analysis</h3>
 * 
 * This plugin calculates the optic flow for each pair of images made with the
 * given stack. It does using the PIV method, which is the most basic technique
 * for optic flow, and is a block-based method. This a technique, mainly used in
 * acoustics or in fluids mechanics, that enables the measurements of a velocity
 * field in one plane, using imaging and image analysis. It can be seen as one
 * of the most simple pattern matching problem implementation.
 * <p>
 * See the book <cite>Raffael, M.; Willert, C. & Kompenhans, J. (2007),
 * Particle Image Velocimetry: A Practical Guide (2nd ed.), Berlin:
 * Springer-Verlag, ISBN 978-3-540-72307-3 </cite>
 * <p>
 * PIV analysis is based on inferring in what direction and in what amount a
 * part of an image has moved between two successive instant. In the
 * aforementioned domains, a flow is visualized by seeding it with
 * light-reflecting particle (smoke in air, bubbles, glass beads in water, ...)
 * and imaged at two very close instant. The cross-correlation between parts of
 * the two images where pattern generated by particles can be seen is then used
 * to compute the velocity field.
 * <p>
 * The PIV algorithm is made of the following steps:
 * <ol>
 * <li>two images are acquired of the same object are acquired at two successive
 * instant;
 * <li>they are spliced in small pieces called interrogation windows;
 * <li>the cross-correlation between the two images is computed for each small
 * window;
 * <li>the peak in the resulting correlation image is searched for. The peak
 * location gives the displacement for which the two image parts look the best
 * alike, that is: the amount by which the second image has to be moved to look
 * like the first image the best;
 * <li>the velocity vector at this point is defined as the peak's position. This
 * assume that between the two successive instants, the image did not change too
 * much in content, but moved or deformed.
 * </ol>
 * 
 * 
 * <h3>Application to Life-science images</h3>
 * 
 * Here are two examples of its first applications in Biology:
 * <ul>
 * <li>comparing flows in a drosophila embryo during gastrulation in control
 * situations and after photo-ablation - <cite>Supatto, W.; D�barre, D. &
 * Moulia, B. et al. (2005), "In vivo modulation of morphogenetic movements in
 * Drosophila embryos with femtosecond laser pulses", PNAS 102: 1047-1052, PMID
 * 15657140</cite>
 * 
 * <li>quantifying blood flow during cardiogenesis in zebrafish embryo - <cite>
 * Hove, J.R. (2003), "Intracardiac fluid forces are an essential epigenetic
 * factor for embryonic cardiogenesis", Nature 421: 172-177, PMID
 * 12520305</cite>
 * </ul>
 * 
 * For us, the main interest of this technique is that it allows the computation
 * of a velocity field without having to segment objects out of an image and
 * track them, which makes it particularly interesting when dealing with
 * brightfield or DIC images. The trade off is loss of precision, but also the
 * fact that you might get completely irrelevant vectors.
 * 
 * <h3>Algorithm</h3>
 * 
 * As stated before, this plugin implements a very naive and promotive
 * algorithm, without a sense of subtlety. It has a very pedestrian approach,
 * that make it slow. For each pixel away from the border of the image, a block
 * is extracted for the front image and the back image. The correlation matrix
 * is then calculated from these two blocks, and the result is analyzed to
 * produce a flow vector. This is a highly redundant process, for when the
 * algorithm moves to the next pixel to the right, the blocks content change
 * only a little (only one column is replaced actually, and the rest is shifted
 * left), but the whole correlation matrix is recalculated from scratch (a lot
 * of wasted CPU cycles).
 * <p>
 * Typically, on a MacBook (grey model, 2009), for an 8-bit stack with a window
 * size of 8x8, the plugin, the plugin can process a stack of 200x200 in
 * approximatively 2 seconds. I recommend downsampling the images, this would
 * diminish the density of results, but make this plugin affordable.
 * 
 * <h3>How to use</h3>
 * 
 * <h4>ROI selection</h4>
 * 
 * If you want to restrain the analysis to only a certain portion of the image,
 * select it in a ROI of any shape.
 * 
 * <h4>Image features</h4>
 * 
 * Tough you don't need to have traceable objects for PIV, there is still some
 * requirements for it.
 * <p>
 * PIV is based on a very simple pattern matching algorithm. This means that, to
 * be effective, there must be some patterns in your image. Typically, the dotty
 * structures of aggregate in a epi-fluorescence movie or the granularity that
 * can be see on a brightfield image will do. As a rule of thumb if you can see
 * something moving by eye, the PIV might be able to get it quantitatively;
 * otherwise not of course.
 * <p>
 * Also keep in mind that (so far) it works only for a 2D plane. With optical
 * sections, the algorithm might become puzzled by the appearance of grains
 * moving in the Z direction. Also with thick specimen imaged in brightfield:
 * the image of a dot deforms while moving in Z, which may lead to irrelevant
 * values.
 * 
 * <h4>Choice of the interrogation window size</h4>
 * 
 * To perform pattern matching, the image is spliced in small pieces. Since you
 * want to have clear patterns to match, it is a good idea to choose you
 * interrogation window size bigger than the image feature whose movement you
 * interested in.
 * <p>
 * Let us suppose that one one image, some structure can be found in the shape
 * of 'grains' whose size is about 10-15 pixels. To always get at least a full
 * one, a window of 32x32 pixels would be relevant.
 * 
 * <h4>Averaging</h4>
 * 
 * In fluid dynamics, velocity fields resulting from PIV are generally heavily
 * spatially filtered afterward. In these cases, each of this vector represents
 * velocity in a fluid for which the flow is generally spatially correlated on a
 * large scale, so filtering in the spatial domain makes sense.
 * <p>
 * One can recommend to filter temporally, that is: for every vector in the
 * field, replace its value by the moving average from a few frames before until
 * a few frames after. If your movie is oversampled temporally, that might be
 * effective in averaging outliers.
 * 
 * <h4>Filtering</h4>
 * The plugin returns also a stack made of the peak height and a tentative
 * signal/noise ratio for the analysis.
 * <p>
 * The peak height is just the value of the correlation peak for each pixels.
 * This can be used to mask irrelevant part of the result image. The algorithm
 * will be typically puzzled where there is no structure to correlate. In these
 * parts of the image, the peak height will be typically low; one can then
 * threshold the peak height image, and use it to mask the result image.
 * 
 * <h3>Version history</h3>
 * 
 * <ul>
 * <li> 1.0 - April 2009 - First public release.
 * <li> 1.1 - April 2009 - is now interruptible
 * </ul>
 * 
 * <h3>License: GPL</h3>
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License 2 as published by the Free
 * Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * @author Jean-Yves Tinevez <tinevez@mpi-cbg.de>
 * @version 1.1
 */
public class PIV_analyser implements PlugInFilter {
	
	/*
	 * FIELDS
	 */
	private ImagePlus imp;
	private PairingParam pairs_param;
	private int[][] image_pairs;
	private int winsize_x = 16;
	private int winsize_y = 16;
	private boolean do_interpolation = true;
	private boolean do_masking = false;
	private double mask_value = 0.5;
	
	/*
	 * CONSTANTS
	 */
	private static final String VERSION_STR = "1.1";
	private final static int COLOR_CIRCLE_SIZE = 128;

	/*
	 * INNER CLASSES
	 */
	
	/**
	 * Enumeration constant that stores all possible window size for this plugin. 
	 * Because we use the Fast Hartley Transform of ImageJ, only square windows
	 * of power of 2 are possible.
	 */
	public enum WINDOW_SIZE {
		_4x4, _8x8, _16x16, _32x32, _64x64, _128x128;
		private static final int[] WS = { 4, 8, 16, 32, 64, 128 };
		private static final String[] STR = { "4x4", "8x8", "16x16", "32x32",
				"64x64", "128x128" };

		/**
		 * Return the window size as int corresponding to this enum constant.
		 * @return  the window size
		 */
		public int toInt() {
			return WS[this.ordinal()];
		}

	};

	/**
	 * Utility class used to store parameters specifying how to pair images 
	 * for PIV analysis.
	 */
	protected class PairingParam {
		int first;
		int last;
		int step;
		int jump;
	}

	/**
	 * Utility class used to store flow vector at each point.
	 */
	protected class PIVresult {
		int max_x;
		int max_y;
		float max_x_interpolated;
		float max_y_interpolated;
		float peak_height;
		float snr;
	}

	/*
	 * CONSTRUCTOR
	 */

	public PIV_analyser() {
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Sets the ImagePlus that this plugin should analyze.
	 * 
	 * @param arg  Ignored
	 * @param imp  The ImagePlus to analyze
	 */
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		this.pairs_param = new PairingParam();

		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		return DOES_16 + DOES_32 + DOES_8G + STACK_REQUIRED + SUPPORTS_MASKING
				+ NO_CHANGES;
	}

	/**
	 * Generate a generic dialog for the user to parameterize this plugin.
	 * Should not be called, else by ImageJ menu itself.
	 */
	public void run(ImageProcessor ip) {

		// Get stack & set fields
		ImageStack stack = imp.getStack();
		int nslices = stack.getSize();
		if (nslices < 2) {
			IJ.error("PIV_analysis requires at least two frames in the stack.");
		}

		// Prepare dialog
		String current = imp.getTitle();
		GenericDialog gd = new GenericDialog("PIV analysis, v"+VERSION_STR);
		gd.addMessage(current);
		gd.addChoice("Window size (px)", WINDOW_SIZE.STR, WINDOW_SIZE.STR[3]);
		gd.addCheckbox("Diplay color wheel", true);
		gd.addMessage("Sub-pixel accuracy:");
		gd.addCheckbox("Interpolate", false);
		gd.addMessage("Masking with correlation peak:");
		gd.addCheckbox("Do masking", false);
		gd.addNumericField("Masking level", 0.5, 2);
		gd.showDialog();

		// Collect dialog
		if (gd.wasCanceled())
			return;
		winsize_x = WINDOW_SIZE.WS[gd.getNextChoiceIndex()];
		winsize_y = winsize_x;

		// Show color wheel if demanded
		if (gd.getNextBoolean()) displayColorCircle(winsize_x);
		
		// Interpolate max position
		do_interpolation = gd.getNextBoolean();
		
		// Masking
		do_masking = gd.getNextBoolean();
		mask_value = gd.getNextNumber();

		// Build image pairs
		pairs_param.first = 1;
		pairs_param.last = nslices;
		pairs_param.step = 1;
		pairs_param.jump = 1;
		setImagePairs(buildImagePairs());

		// Execute calculation
		exec(true); // true flag enable live display
	}

	/**
	 * Executes the calculation for the value passed to this plugin's fields.
	 * 
	 * @param show_calculation
	 *            If true, results will be displayed during calculation
	 * @return  an array of ImagePlus containing: the image for the U compoenent of 
	 * vector flow, the V component, the correlation peak height, and the color
	 * encoded direction of the flow. 
	 * @see  #setup(String, ImagePlus) setup
	 * @see  #setImagePairs(int[][]) setImagePairs 
	 * @see  #setWinsize(WINDOW_SIZE) setWinsize
	 * @see #setInterpolation(boolean) setInterpolation 
	 */
	final public ImagePlus[] exec(final boolean show_calculation) {
		final ImageStack stack = imp.getStack();
		final int npairs = this.getImagePairs().length;
		final int image_width = stack.getWidth();
		final int image_height = stack.getHeight();

		FloatProcessor front_block_imp, back_block_imp;
		int front_image, back_image;
		FloatProcessor back_imp, front_imp;
		int x, y;

		// Prepare results holder
		ImageCanvas color_canvas;
		// Stacks
		ImageStack u_st = new ImageStack(image_width, image_height, npairs);
		ImageStack v_st = new ImageStack(image_width, image_height, npairs);
		ImageStack pkh_st = new ImageStack(image_width, image_height, npairs);
		// ImageStack snr_st = new ImageStack(image_width, image_height,
		// npairs);
		ImageStack color_st = new ImageStack(image_width, image_height, npairs);
		// ImagePlus
		ImagePlus u_imp = imp.createImagePlus();
		ImagePlus v_imp = imp.createImagePlus();
		ImagePlus pkh_imp = imp.createImagePlus();
		// ImagePlus snr_imp = imp.createImagePlus();
		ImagePlus color_imp = imp.createImagePlus();
		// Color processor
		ColorProcessor color_ip;
		// Arrays
		float[][] u = new float[image_width][image_height];
		float[][] v = new float[image_width][image_height];
		float[][] pkh = new float[image_width][image_height];
		// float[][] snr = new float[image_width][image_height];
		int[][] color_angle = new int[image_width][image_height];

		FHT fft_front, fft_back, pcm;
		PIVresult piv;

		// ImageStack debug = new ImageStack(winsize_x,winsize_y);

		// Copy and store the current roi
		final Roi roi;
		if (imp.getRoi() == null) {
			roi = null;
		} else {
			roi = (Roi) imp.getRoi().clone();
		}
		
		// Loop over image pairs
		for (int i = 0; i < npairs; i++) {

			back_image = getImagePairs()[i][0];
			front_image = getImagePairs()[i][1];
			back_imp = (FloatProcessor) stack.getProcessor(back_image).convertToFloat();
			front_imp = (FloatProcessor) stack.getProcessor(front_image).convertToFloat();

			if (IJ.escapePressed()) {
				IJ.showStatus("PIV analysis cancelled.");
				break;
			}
			for (x = 0; x <= image_width - winsize_x; x++) {

				for (y = 0; y <= image_height - winsize_y; y++) {
					
					// skip if current point is not in roi
					if ( (roi != null) && (!roi.contains(x+winsize_x/2, y+winsize_y/2)) ) continue;

					front_imp.setRoi(x, y, winsize_x, winsize_y);
					front_block_imp = (FloatProcessor) front_imp.crop();
					back_imp.setRoi(x, y, winsize_x, winsize_y);
					back_block_imp = (FloatProcessor) back_imp.crop();

					// Substract mean
					substractMean(back_block_imp);
					substractMean(front_block_imp);

					// Compute correlation matrix
					fft_front = new FHT(front_block_imp);
					fft_front.setShowProgress(false);
					fft_back = new FHT(back_block_imp);
					fft_back.setShowProgress(false);
					fft_front.transform();
					fft_back.transform();
					pcm = fft_front.conjugateMultiply(fft_back);
					pcm.setShowProgress(false);
					pcm.inverseTransform();
					pcm.swapQuadrants(); // centered in middle of window

					// debug.addSlice("X="+x+" Y="+y, pcm);

					piv = findMax(pcm, do_interpolation);

					u[x + winsize_x / 2][y + winsize_y / 2] = piv.max_x_interpolated;
					v[x + winsize_x / 2][y + winsize_y / 2] = piv.max_y_interpolated;
					pkh[x + winsize_x / 2][y + winsize_y / 2] = piv.peak_height;
					// snr[x + block_width / 2][y + block_height / 2] = piv.snr;
				}
			}

			// Do masking 
			if (do_masking) {
				float max_pkh = getMax(pkh);
				mask(u, pkh, max_pkh);
				mask(v, pkh, max_pkh);
			}
			
			// Compute color vector
			for (x = 0; x <= image_width - winsize_x; x++) {
				for (y = 0; y <= image_height - winsize_y; y++){
					color_angle[x + winsize_x / 2][y + winsize_y / 2] = colorVector(
							u[x + winsize_x / 2][y + winsize_y / 2], 
							v[x + winsize_x / 2][y + winsize_y / 2],
							winsize_x / 4);
				}
			}

			// Add to stack
			u_st.setPixels(new FloatProcessor(u).getPixels(), i + 1);
			v_st.setPixels(new FloatProcessor(v).getPixels(), i + 1);
			pkh_st.setPixels(new FloatProcessor(pkh).getPixels(), i + 1);
			// snr_st.setPixels(new FloatProcessor(snr).getPixels(), i + 1);
			color_ip = new ColorProcessor(image_width, image_height);
			color_ip.setIntArray(color_angle);
			color_st.setPixels(color_ip.getPixels(), i + 1);

			if (show_calculation) {
				imp.setSlice(i + 1);
				if (i == 0) {
					u_imp.setStack("U", u_st);
					v_imp.setStack("V", v_st);
					pkh_imp.setStack("Peak height", pkh_st);
					// snr_imp.setStack("Signal/Noise ratio", snr_st);
					color_imp.setStack("Flow direction", color_st);
					u_imp.show();
					v_imp.show();
					pkh_imp.show();
					// snr_imp.show();
					color_imp.show();
				}
				u_imp.setSlice(i);
				v_imp.setSlice(i);
				pkh_imp.setSlice(i);
				// snr_imp.setSlice(i);
				color_imp.setSlice(i);
				pkh_imp.resetDisplayRange();
				u_imp.resetDisplayRange();
				v_imp.resetDisplayRange();
				// snr_imp.resetDisplayRange();
				color_imp.resetDisplayRange();
				color_canvas = color_imp.getCanvas();
				// Add the MouseMotionListener that "deconvolves" color
				color_canvas.addMouseMotionListener(getColorMouseListener(winsize_x/4.0f));
			}
			IJ.showProgress(i, npairs);
		}
		// new ImagePlus("DEBUG", debug).show();
		return new ImagePlus[] { u_imp, v_imp, pkh_imp, color_imp };

	}

	/**
	 * Finds the maximum location in a correlation matrix. In the framework of
	 * this plugin, this will give the flow vector.
	 * <p>
	 * If the flag <i>interpolate</i> is set to true, the maximum location will
	 * be interpolated (to get sub-pixel accuracy) using a Taylor expansion over
	 * a 3x3 neighborhood around the maxima.
	 * The peak value (non-interpolated) is returned for futher filtering
	 * purpose. So far, there is no estimation of the signal/noise ratio
	 * calculated.
	 * 
	 * @param pcm
	 *            The correlation matrix
	 * @param interpolate
	 *            Boolean flag
	 * @return The maximum location and value
	 * @see FHT
	 * @see PIVresult
	 */
	final public PIVresult findMax(final FHT pcm, boolean interpolate) {
		PIVresult piv = new PIVresult();
		final float[] pixels = (float[]) pcm.getPixels();
		final float e00, e10, e20, e01, e11, e21, e02, e12, e22;
		float pkh = -Float.MAX_VALUE;
		int loc = 0;
		// float sum = 0.0f;
		// float sum_sqr = 0.0f;
		float val;
		// We loop to find max, but also compute mean
		for (int i = 0; i < pixels.length; i++) {
			val = pixels[i];
			// sum += val;
			// sum_sqr += val * val;
			if (val > pkh) {
				pkh = val;
				loc = i;
			}
		}
		// final float mean = sum / pixels.length;
		// final float var = (sum_sqr - pixels.length * mean * mean)
		// / pixels.length;

		piv.peak_height = pkh;
		// if (var <= 1e-6)
		// return piv;
		// piv.snr = (float) Math.sqrt(var) / pkh;
		// piv.snr = pkh - mean;

		piv.max_x = loc % winsize_x - winsize_x / 2;
		piv.max_y = loc / winsize_x - winsize_y / 2;

		// Create 3x3 neighborhood
		if (interpolate) {
			try {
				// Get neighborhood
				e00 = pixels[loc - winsize_x - 1];
				e10 = pixels[loc - winsize_x];
				e20 = pixels[loc - winsize_x + 1];
				e01 = pixels[loc - 1];
				e11 = pixels[loc];
				e21 = pixels[loc + 1];
				e02 = pixels[loc + winsize_x - 1];
				e12 = pixels[loc + winsize_x];
				e22 = pixels[loc + winsize_x + 1];

				final float dx, dy;
				final float dxy, dxx, dyy;
				final float ox, oy;

				// derive at (x, y, i) by center of difference -> D
				dx = (e21 - e01) / 2.0f;
				dy = (e12 - e10) / 2.0f;

				// create hessian at (x, y, i) by laplace -> H
				final float e11_2 = 2.0f * e11;
				dxx = e01 - e11_2 + e21;
				dyy = e10 - e11_2 + e12;
				dxy = (e22 - e02 - e20 + e00) / 4.0f;

				// invert hessian -> H-1
				final float det = dxx * dyy - dxy * dxy;
				if (det == 0) {
					// data is linearly dependent, can't interpolate
					piv.max_x_interpolated = (float) piv.max_x;
					piv.max_y_interpolated = (float) piv.max_y;
				}

				// localize O = H-1 x D
				ox = +dyy / det * dx - dxy / det * dy;
				oy = -dxy / det * dx + dxx / det * dy;

				piv.max_x_interpolated = (float) piv.max_x - 3 * ox;
				piv.max_y_interpolated = (float) piv.max_y - 3 * oy;

			} catch (ArrayIndexOutOfBoundsException e) {
				piv.max_x_interpolated = (float) piv.max_x;
				piv.max_y_interpolated = (float) piv.max_y;
			}
		} else {
			piv.max_x_interpolated = (float) piv.max_x;
			piv.max_y_interpolated = (float) piv.max_y;
		}

		return piv;
	}

	void showAbout() {
		IJ.showMessage("About PIV_analysis...",
				 " <h3>PIV analysis</h3> \n" +
				 " This plugin calculates the optic flow for each pair of images made with the\n" + 
				 " given stack. It does using the PIV method, which is the most basic technique\n" + 
				 " for optic flow, and is a block-based method. This a technique, mainly used in\n" + 
				 " acoustics or in fluids mechanics, that enables the measurements of a velocity\n" + 
				 " field in one plane, using imaging and image analysis. It can be seen as one\n" + 
				 " of the most simple pattern matching problem implementation.\n" + 
				 " \n " +
				 " PIV analysis is based on inferring in what direction and in what amount a\n " + 
				 " part of an image has moved between two successive instant. In the\n " + 
				 " aforementioned domains, a flow is visualized by seeding it with\n " + 
				 " light-reflecting particle (smoke in air, bubbles, glass beads in water, ...)\n " + 
				 " and imaged at two very close instant. The cross-correlation between parts of\n " + 
				 " the two images where pattern generated by particles can be seen is then used\n " + 
				 " to compute the velocity field.\n " + 
				 " \n " + 
				 " The PIV algorithm is made of the following steps:\n " + 
				 " <ol>\n " + 
				 " <li>two images are acquired of the same object are acquired at two successive\n " + 
				 " instant;\n " + 
				 " <li>they are spliced in small pieces called interrogation windows;\n " + 
				 " <li>the cross-correlation between the two images is computed for each small\n " + 
				 " window;\n " + 
				 " <li>the peak in the resulting correlation image is searched for. The peak\n " + 
				 " location gives the displacement for which the two image parts look the best\n " + 
				 " alike, that is: the amount by which the second image has to be moved to look\n " + 
				 " like the first image the best;\n " + 
				 " <li>the velocity vector at this point is defined as the peak's position. This\n " + 
				 " assume that between the two successive instants, the image did not change too\n " + 
				 " much in content, but moved or deformed.\n " + 
				 " </ol>\n "  ); 
	}

	/**
	 * Returns an int that encodes for a color describing the orientation of the
	 * vector whose coordinates are given in argument. The luminance value
	 * encodes the length of the vector normalized by a max value.
	 * <p>
	 * Taken from Stephan Saalfeld Optic_Flow.java plugin.
	 * 
	 * @param xs   The X coordinate of the vector
	 * @param ys  The Y coordinate of the vector
	 * @param maxDistance   The max expected length of a vector
	 * @return an int encoding for a color
	 */
	final static protected int colorVector(float xs, float ys, float maxDistance) {
		xs /= maxDistance;
		ys /= maxDistance;
		final double a = Math.sqrt(xs * xs + ys * ys);
		if (a == 0.0)
			return 0;

		double o = (Math.atan2(xs / a, ys / a) + Math.PI) / Math.PI * 3;

		final double r, g, b;

		if (o < 3)
			r = Math.min(1.0, Math.max(0.0, 2.0 - o)) * a;
		else
			r = Math.min(1.0, Math.max(0.0, o - 4.0)) * a;

		o += 2;
		if (o >= 6)
			o -= 6;

		if (o < 3)
			g = Math.min(1.0, Math.max(0.0, 2.0 - o)) * a;
		else
			g = Math.min(1.0, Math.max(0.0, o - 4.0)) * a;

		o += 2;
		if (o >= 6)
			o -= 6;

		if (o < 3)
			b = Math.min(1.0, Math.max(0.0, 2.0 - o)) * a;
		else
			b = Math.min(1.0, Math.max(0.0, o - 4.0)) * a;

		return (((int) (r * 255) << 8) + (int) (g * 255) << 8)
				+ (int) (b * 255) ;
	}

	/**
	 * Generate a color circle that shows the mapping between color and
	 * orientation, to use in conjunction with
	 * {@link PIV_analyser#colorVector(float, float, float)}.
	 * <p>
	 * Taken and modified from Stephan Saalfeld Optic_Flow.java plugin.
	 * 
	 * @param ip   The ImageProcessor to draw the color circle in
	 * @param maxDistance  The max expected displacement
	 */
	final static public void colorCircle(ColorProcessor ip) {
		final int lx = ip.getWidth();
		final int ly = ip.getHeight();
		final int r1 = Math.min(lx, ly) / 2;
		float dx, dy, l;

		for (int y = 0; y < ly; ++y) {
			dy = y - ly / 2;
			
			for (int x = 0; x < lx; ++x) {
				dx = x - lx / 2;
				l = (float) Math.sqrt(dx * dx + dy * dy);

				if (l > r1 )
					ip.putPixel(x, y, 0);
				else
					ip.putPixel(x, y, colorVector(dx , dy , r1) );
			}
		}
	}
	
	final static public void displayColorCircle(final float maxDisplacement) {
		ColorProcessor cp = new ColorProcessor(COLOR_CIRCLE_SIZE, COLOR_CIRCLE_SIZE);
		colorCircle(cp);
		ImagePlus cwimp = new ImagePlus("Color coded orientation", cp);
		cwimp.show();
		final ImageCanvas cwcanvas = cwimp.getCanvas();
//		cwcanvas.addMouseMotionListener(getColorMouseListener());
		cwcanvas.addMouseMotionListener(new MouseMotionListener() {

			private final int xc = COLOR_CIRCLE_SIZE/2;
			private final int yc = COLOR_CIRCLE_SIZE/2;
			
			public void mouseDragged(MouseEvent e) {}

			public void mouseMoved(MouseEvent e) {
				Point coord = cwcanvas.getCursorLoc();
				int x = coord.x;
				int y = coord.y;
				int dx = x-xc;
				int dy = y-yc;
				double v = Math.sqrt(dx*dx+dy*dy) / COLOR_CIRCLE_SIZE * maxDisplacement;
				double alpha = -Math.toDegrees( Math.atan2(dy, dx) );
				IJ.showStatus( String.format("Velocity: %5.1f px/frame - Direction %3.0f�", v, alpha));
			}});
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/** 
	 * Creates the mouse listener that will "deconvolve" the color coded 
	 * flow vector in an orientation and magnitude. It is not very clever,
	 * since the color was calculated from the flow vector in the method
	 * {@link #colorVector(float, float, float) colorVector}, but it is cheaper
	 * memory-wise than storing the 2 float arrays for each slice.
	 * 
	 *  @return  the mouse motion listener
	 * 
	 */
	final private static MouseMotionListener getColorMouseListener(final float maxVel) {
		return new MouseMotionListener() {

			public void mouseDragged(MouseEvent e) {			}

			public void mouseMoved(MouseEvent e) {
				final ImageCanvas source = (ImageCanvas) e.getComponent();
				final Point coord = source.getCursorLoc();
				final BufferedImage im = (BufferedImage) ( (ImageWindow) source.getParent() ).getImagePlus().getImage();
				final int cp =im.getRGB(coord.x, coord.y);
				final float r = (float) ( (cp >> 16) &0xff);
				final float g = (float) ( (cp >> 8) &0xff);
				final float b = (float) ( (cp)  &0xff);
				double alpha;
				double magnitude;
				if (b == 0) {
					// alpha in [0, 2pi/3]
					if (r>g) {
						alpha = (Math.PI/3) * g/r;
						magnitude = r/255 * maxVel;
					} else {
						alpha = (2*Math.PI/3) * (1 - 0.5*r/g) ;
						magnitude = g/255 * maxVel;
					}
				} else if (r == 0) {
					// alpha in [-2pi/3, -pi/6]
					if (g>b) {
						alpha = (2*Math.PI/3) * (1 + 0.5*b/g);
						magnitude = g/255 * maxVel;
					} else {
						alpha = (2*Math.PI/3) * (2 - 0.5*g/b);
						magnitude = b/255 * maxVel;
					}
				} else {
					if (b>r) {
						alpha =  (2*Math.PI/3) * (2 + 0.5*r/b);						
						magnitude = b/255 * maxVel;
					} else {
						alpha =  (2*Math.PI/3) * (3 - 0.5*b/r);
						magnitude = r/255 * maxVel;
					}
				}
				// put into normal coordinate
				alpha = -(alpha-Math.PI/2);
				if (alpha <- Math.PI) alpha += 2*Math.PI;
				
				IJ.showStatus(String.format("Velocity: %5.1f px/frame - Direction %3.0f�", 
						magnitude, Math.toDegrees(alpha)));
				
			}
		};
	}
	
	final private void mask(float[][] arr, final float[][] mask_arr, final float max_mask_value) {
		final float val = (float) (mask_value * max_mask_value);
		for (int i = 0; i < mask_arr.length; i++) {
			for (int j = 0; j < mask_arr[i].length; j++) {
				if ( mask_arr[i][j] < val ) arr[i][j] = 0.0f; // Could be Float.NaN but would have messed with grayscales 
			}
		}
	}
	
	final static public float getMax(final float[][] arr) {
		float max = - Float.MAX_VALUE;
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				if (arr[i][j] > max) max = arr[i][j]; 
			}
		}
		return max;
	}

	private static void substractMean(FloatProcessor fp) {
		float[] pixels = (float[]) fp.getPixels();
		float sum = pixels[0];
		for (int i = 1; i < pixels.length; i++) {
			sum += pixels[i];
		}
		final float mean = sum / pixels.length;
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] -= mean;
		}

	}

	/**
	 * Build a 2D array of int specifying how to pair images. The pairing is
	 * built according to this class field ParingParam param.
	 * 
	 * @return the array of array of image pairs
	 */
	private int[][] buildImagePairs() {
		return buildImagePairs(pairs_param);
	}

	/**
	 * Build a 2D array of int specifying how to pair images.
	 * <p>
	 * Image pair are built as following:
	 * <ul>
	 * <li>Back images runs from <i>first</i>, stepping by <i>step</i>.
	 * <li>They are coupled with front images whose index is <i>jump</i> steps
	 * ahead of theirs.
	 * </ul>
	 * 
	 * @param first   The first index of the image pairs
	 * @param last    The max index of the image pairs
	 * @param step    Step from one pair to another
	 * @param jump    Step between back and front image
	 * @return A nx2 array of int, containing the indices of images for the n
	 *         pairs
	 */
	private static int[][] buildImagePairs(int first, int last, int step,
			int jump) {
		int npairs = (int) Math.ceil((last - (first + jump - 1)) / step);
		int[][] pairs = new int[npairs][2];
		int front, back;

		for (int index = 0; index < npairs; index++) {
			back = first + index * step;
			front = back + jump;
			pairs[index][0] = back;
			pairs[index][1] = front;
		}
		return pairs;
	}

	/**
	 * Build a 2D array of int specifying how to pair images, according to the
	 * ParingParam param given in argument.
	 * 
	 * @param param
	 *            The PairingParam object that specifies how to build image
	 *            pairs.
	 * @return the array of array of image pairs
	 */
	private static int[][] buildImagePairs(PairingParam param) {
		int first = param.first;
		int last = param.last;
		int step = param.step;
		int jump = param.jump;
		return buildImagePairs(first, last, step, jump);
	}

	/*
	 * SETTERS AND GETTERS
	 */

	/**
	 * Specifies on what pair of images to do the analysis. The
	 * <i>image_pair</i> argument must be a nx2 int array, specifying the back
	 * and front image for the n desired steps.
	 * 
	 * @param image_pairs   The nx2 array of int specifying the image pairs.
	 */
	public void setImagePairs(int[][] image_pairs) {
		this.image_pairs = image_pairs;
	}

	public int[][] getImagePairs() {
		return image_pairs;
	}

	/**
	 * Sets the size of the interrogation window for the analysis. So far, only
	 * square window of power of 2 size are allowed.
	 * 
	 * @param ws  The window size, specified by an enum
	 */
	public void setWinsize(WINDOW_SIZE ws) {
		this.winsize_x = ws.toInt();
		this.winsize_y = ws.toInt();
	}

	public int[] getWinsize() {
		return new int[] { winsize_x, winsize_y };
	}

	/**
	 * If set to true, the vector flow will be interpolated to get a sub-pixel
	 * accuracy, using a Taylor expansion on a 3x3 neighborhood around the
	 * maximum position.
	 * 
	 * @param doit
	 *            The boolean flag
	 */
	public void setInterpolation(boolean doit) {
		this.do_interpolation = doit;
	}

	public boolean getInterpolation() {
		return do_interpolation;
	}

}
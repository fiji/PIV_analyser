/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2009 - 2021 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.pivanalyser;

/**
 * Time the execution of any block of code.
 * <p>
 * Copied from <a href=
 * "http://www.javapractices.com/topic/TopicAction.do?Id=85">javapractice.com</a>
 */

public final class Stopwatch
{

	/**
	 * An example of the use of this class to time the execution of String
	 * manipulation code.
	 */
	public static void main( final String... arguments )
	{
		final Stopwatch stopwatch = new Stopwatch();

		stopwatch.start();

		// do stuff
		final StringBuilder messageOne = new StringBuilder();
		final int numIterations = 5000;
		for ( int idx = 0; idx < numIterations; ++idx )
		{
			messageOne.append( "blah" );
		}

		stopwatch.stop();
		// Note that there is no need to call a method to get the duration,
		// since toString is automatic here
		System.out.println( "The reading for StringBuilder is: " + stopwatch );

		// reuse the same stopwatch to measure an alternative implementation
		// Note that there is no need to call a reset method.
		stopwatch.start();

		// do stuff again
		String messageTwo = null;
		for ( int idx = 0; idx < numIterations; ++idx )
		{
			messageTwo = messageTwo + "blah";
		}

		stopwatch.stop();
		// perform a numeric comparsion
		if ( stopwatch.toValue() > 5 )
		{
			System.out.println( "The reading is high: " + stopwatch );
		}
		else
		{
			System.out.println( "The reading is low: " + stopwatch );
		}
	}

	/**
	 * Start the stopwatch.
	 *
	 * @throws IllegalStateException
	 *             if the stopwatch is already running.
	 */
	public void start()
	{
		if ( fIsRunning )
		{ throw new IllegalStateException( "Must stop before calling start again." ); }
		// reset both start and stop
		fStart = System.currentTimeMillis();
		fStop = 0;
		fIsRunning = true;
		fHasBeenUsedOnce = true;
	}

	/**
	 * Stop the stopwatch.
	 *
	 * @throws IllegalStateException
	 *             if the stopwatch is not already running.
	 */
	public void stop()
	{
		if ( !fIsRunning )
		{ throw new IllegalStateException( "Cannot stop if not currently running." ); }
		fStop = System.currentTimeMillis();
		fIsRunning = false;
	}

	/**
	 * Express the "reading" on the stopwatch.
	 *
	 * @throws IllegalStateException
	 *             if the Stopwatch has never been used, or if the stopwatch is
	 *             still running.
	 */
	@Override
	public String toString()
	{
		validateIsReadable();
		final StringBuilder result = new StringBuilder();
		result.append( fStop - fStart );
		result.append( " ms" );
		return result.toString();
	}

	/**
	 * Express the "reading" on the stopwatch as a numeric type.
	 *
	 * @throws IllegalStateException
	 *             if the Stopwatch has never been used, or if the stopwatch is
	 *             still running.
	 */
	public long toValue()
	{
		validateIsReadable();
		return fStop - fStart;
	}

	// PRIVATE ////
	private long fStart;

	private long fStop;

	private boolean fIsRunning;

	private boolean fHasBeenUsedOnce;

	/**
	 * Throws IllegalStateException if the watch has never been started, or if
	 * the watch is still running.
	 */
	private void validateIsReadable()
	{
		if ( fIsRunning )
		{
			final String message = "Cannot read a stopwatch which is still running.";
			throw new IllegalStateException( message );
		}
		if ( !fHasBeenUsedOnce )
		{
			final String message = "Cannot read a stopwatch which has never been started.";
			throw new IllegalStateException( message );
		}
	}
}

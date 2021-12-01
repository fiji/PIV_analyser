package fiji.plugin.pivanalyser;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils
{
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

	public static String now()
	{
		final Calendar cal = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat( DATE_FORMAT_NOW );
		return sdf.format( cal.getTime() );

	}

	public static void main( final String arg[] )
	{
		System.out.println( "Now : " + DateUtils.now() );
	}
}

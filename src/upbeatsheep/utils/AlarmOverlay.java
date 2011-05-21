package upbeatsheep.utils;


import upbeatsheep.CommuteAlarm.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class AlarmOverlay extends Overlay {

	GeoPoint alarmLocation;
	private MapView mapView;
	private Paint circlePainter;
	private Point screenCurrentPoint;
	private GeoPoint geoCurrentPoint;
	private int meters;

	protected final Bitmap DIRECTION_ARROW;
	
	private final float DIRECTION_ARROW_CENTER_X;
    private final float DIRECTION_ARROW_CENTER_Y;
    private final int DIRECTION_ARROW_WIDTH;
    private final int DIRECTION_ARROW_HEIGHT;
	
	public AlarmOverlay(Context context, GeoPoint alarmLocation, MapView mapView) {
		super();
		this.alarmLocation = alarmLocation;
		this.mapView = mapView;
		
		this.DIRECTION_ARROW = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker);
        
        this.DIRECTION_ARROW_CENTER_X = this.DIRECTION_ARROW.getWidth() / 2 - 0.5f;
        this.DIRECTION_ARROW_CENTER_Y = 0;
        this.DIRECTION_ARROW_HEIGHT = this.DIRECTION_ARROW.getHeight();
        this.DIRECTION_ARROW_WIDTH = this.DIRECTION_ARROW.getWidth();
	}

	// This method is used to get user submitted radius from our application
	public void setMeters(int meters) {
		this.meters = meters;
	}

	@Override
	public synchronized boolean draw(Canvas canvas, MapView mapView,
			boolean shadow, long when) {
		// Set the painter to paint our circle. setColor = blue, setAlpha = 70
		// so the background
		// can still be seen. Feel free to change these settings
		circlePainter = new Paint();
		circlePainter.setAntiAlias(true);
		circlePainter.setStrokeWidth(2.0f);
		circlePainter.setColor(0xff6666ff);
		circlePainter.setStyle(Style.FILL_AND_STROKE);
		circlePainter.setAlpha(70);

		// Get projection from the mapView.
		Projection projection = mapView.getProjection();
		// Get current location
		geoCurrentPoint = alarmLocation;
		screenCurrentPoint = new Point();
		// Project the gps coordinate to screen coordinate
		projection.toPixels(geoCurrentPoint, screenCurrentPoint);

		int radius = metersToRadius(geoCurrentPoint.getLatitudeE6() / 1000000);
		// draw the blue circle
		canvas.drawCircle(screenCurrentPoint.x, screenCurrentPoint.y, radius,
				circlePainter);
		
		Bitmap arrow = Bitmap.createBitmap(DIRECTION_ARROW, 0, 0, DIRECTION_ARROW_WIDTH, DIRECTION_ARROW_HEIGHT);
		
		Paint mPaint = new Paint();
		Matrix matrix = new Matrix();
		matrix.postTranslate(-arrow.getWidth()/2, -arrow.getHeight());
		matrix.postTranslate(screenCurrentPoint.x, screenCurrentPoint.y);
		
		canvas.drawBitmap(arrow, matrix, mPaint);
		
		
		return super.draw(canvas, mapView, shadow, when);
	}

	// hack to get more accurate radius, because the accuracy is changing as the
	// location
	// getting further away from the equator
	public int metersToRadius(double latitude) {
		return (int) (mapView.getProjection().metersToEquatorPixels(meters) * (1 / Math.cos(Math.toRadians(latitude))));
	}
}

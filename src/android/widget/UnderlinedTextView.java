package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;


public class UnderlinedTextView extends TextView{
	private Paint paint = new Paint();
	
	public UnderlinedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPaint();
	}
	
	public UnderlinedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaint();
	}
	
	public UnderlinedTextView(Context context) {
		super(context);
		initPaint();
	}
	
	private void initPaint() {
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);
		paint.setColor(Color.RED);
	}
	
	//HACK: assuming that we'll never need to set bg color, thus overriding this to set underline
	@Override
	public void setBackgroundColor(int color) {
		paint.setColor(color);
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int height = getHeight();
		int width = getWidth();
		canvas.drawLine(0, height - 5, width, height - 5, paint); //draw underline
		super.onDraw(canvas);
	}
}
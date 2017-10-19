
package log350.example.example6;

import java.util.ArrayList;
//import java.util.List;

import android.content.Context;
//import android.graphics.Matrix;
import android.graphics.Canvas;
//import android.graphics.Rect;
//import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;



// This class stores the current position of a finger,
// as well as the history of previous positions of that finger
// during its drag.
//
// An instance of this class is created when a finger makes contact
// with the multitouch surface.  The instance stores all
// subsequent positions of the finger, and is destroyed
// when the finger is lifted off the multitouch surface.
class MyCursor {
	
	// Each finger in contact with the multitouch surface is given
	// a unique id by the framework (or computing platform).
	// There is no guarantee that these ids will be consecutive nor increasing.
	// For example, when two fingers are in contact with the multitouch surface,
	// their ids may be 0 and 1, respectively,
	// or their ids may be 14 and 9, respectively.
	public int id; // identifier

	// This stores the history of positions of the "cursor" (finger)
	// in pixel coordinates.
	// The first position is where the finger pressed down,
	// and the last position is the current position of the finger.
	private ArrayList< Point2D > positions = new ArrayList< Point2D >();



	// These are used to store what the cursor is being used for.
	public static final int TYPE_DRAGGING = 0; // the finger can be used for dragging objects, zooming, drawing a lasso, ...
	public static final int TYPE_BUTTON = 1; // the finger is pressing a virtual button
	public static final int TYPE_IGNORE = 2; // the finger should not be there and will be ignored
	public static final int TYPE_CREATE = 3;
	public int type = TYPE_IGNORE;




	public MyCursor( int id, float x, float y ) {
		this.id = id;
		positions.add( new Point2D(x,y) );
	}

	public ArrayList< Point2D > getPositions() { return positions; }

	public void addPosition( Point2D p ) {
		positions.add( p );
	}

	public Point2D getFirstPosition() {
		if ( positions == null || positions.size() < 1 )
			return null;
		return positions.get( 0 );
	}
	public Point2D getCurrentPosition() {
		if ( positions == null || positions.size() < 1 )
			return null;
		return positions.get( positions.size()-1 );
	}
	public Point2D getPreviousPosition() {
		if ( positions == null || positions.size() == 0 )
			return null;
		if ( positions.size() == 1 )
			return positions.get( 0 );
		return positions.get( positions.size()-2 );
	}


	public int getType() { return type; }
	public void setType( int type ) { this.type = type; }
}


// This stores a set of instances of MyCursor.
// Each cursor can be identified by its id,
// which is assigned by the framework or computing platform.
// Each cursor can also be identified by its index in this class's container.
// For example, if an instance of this class is storing 3 cursors,
// their ids may be 2, 18, 7,
// but their indices should be 0, 1, 2.
class CursorContainer {
	private ArrayList< MyCursor > cursors = new ArrayList< MyCursor >();

	public int getNumCursors() { return cursors.size(); }
	public MyCursor getCursorByIndex( int index ) { return cursors.get( index ); }

	public int findIndexOfCursorById( int id ) {
		for ( int i = 0; i < cursors.size(); ++i ) {
			if ( cursors.get(i).id == id )
				return i;
		}
		return -1;
	}
	public MyCursor getCursorById( int id ) {
		int index = findIndexOfCursorById( id );
		return ( index == -1 ) ? null : cursors.get( index );
	}

	// Returns the number of cursors that are of the given type.
	public int getNumCursorsOfGivenType( int type ) {
		int num = 0;
		for ( int i = 0; i < cursors.size(); ++i ) {
			if ( cursors.get(i).getType() == type )
				num ++;
		}
		return num;
	}

	// Returns the (i)th cursor of the given type,
	// or null if no such cursor exists.
	// Can be used for retrieving both cursors of type TYPE_DRAGGING, for example,
	// by calling getCursorByType( MyCursor.TYPE_DRAGGING, 0 )
	// and getCursorByType( MyCursor.TYPE_DRAGGING, 1 ),
	// when there may be cursors of other type present at the same time.
	public MyCursor getCursorByType( int type, int i ) {
		for ( int ii = 0; ii < cursors.size(); ++ii ) {
			if ( cursors.get(ii).getType() == type ) {
				if ( i == 0 )
					return cursors.get(ii);
				else
					i --;
			}
		}
		return null;
	}

	public ArrayList<Point2D> getPositionsOfCursorsOfType(int type){

		ArrayList<Point2D> crs = new ArrayList<Point2D>();

		for(MyCursor c : cursors){

			if(c.getType() == type){
				crs.add(c.getCurrentPosition());
			}
		}

		return crs;
	}

	// Returns index of updated cursor.
	// If a cursor with the given id does not already exist, a new cursor for it is created.
	public int updateCursorById(
		int id,
		float x, float y
	) {
		Point2D updatedPosition = new Point2D( x, y );
		int index = findIndexOfCursorById( id );
		if ( index == -1 ) {
			cursors.add( new MyCursor( id, x, y ) );
			index = cursors.size() - 1;
		}
		MyCursor c = cursors.get( index );
		if ( ! c.getCurrentPosition().equals( updatedPosition ) ) {
			c.addPosition( updatedPosition );
		}
		return index;
	}
	public void removeCursorByIndex( int index ) {
		cursors.remove( index );
	}

	public void removeAllCursors(){

		cursors.clear();

	}
}



public class DrawingView extends View {

	Paint paint = new Paint();

	GraphicsWrapper gw = new GraphicsWrapper();

	ShapeContainer shapeContainer = new ShapeContainer();
	ArrayList< Shape > selectedShapes = new ArrayList< Shape >();
	CursorContainer cursorContainer = new CursorContainer();

	static final int MODE_NEUTRAL = 0; // the default mode
	static final int MODE_CAMERA_MANIPULATION = 1; // the user is panning/zooming the camera
	static final int MODE_SHAPE_MANIPULATION = 2; // the user is translating/rotating/scaling a shape
    static final int MODE_POLYGON_MANIPULATION = 3; // the user is translating/rotating/scaling a shape
	static final int MODE_LASSO = 4; // the user is drawing a lasso to select shapes
	static final int MODE_CREATE = 5;
	static final int MODE_FRAMETOSHAPE = 6;
	static final int MODE_FRAMETOSCENE = 7;
	static final int MODE_ERASE = 8;
	int currentMode = MODE_NEUTRAL;

	// This is only used when currentMode==MODE_SHAPE_MANIPULATION, otherwise it is equal to -1
	int indexOfShapeBeingManipulated = -1;

	MyButton lassoButton = new MyButton( "Lasso", 10, 70, 140, 140 );
	MyButton createButton = new MyButton("Create", 10, 220, 140, 140);
	MyButton frameToShapeButton = new MyButton("Zoom Shape", 10, 370, 140, 140);
	MyButton frameToSceneButton = new MyButton("AutoZoom", 10, 520, 140, 140);
	MyButton eraseButton = new MyButton("Erase", 10, 670, 140, 140);

	OnTouchListener touchListener;
    ArrayList<Point2D> lassoPolygonPoints;

    public DrawingView(Context context) {
		super(context);
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		
		this.setOnTouchListener(getTouchListener());
		
		this.setBackgroundColor(Color.WHITE);
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);

		ArrayList< Point2D > arrayList = new ArrayList< Point2D >();
		arrayList.add( new Point2D(100,50) );
		arrayList.add( new Point2D(100,650) );
		arrayList.add( new Point2D(400,350) );
		shapeContainer.addShape( arrayList );
		arrayList.clear();
		arrayList.add( new Point2D(500,100) );
		arrayList.add( new Point2D(800,100) );
		arrayList.add( new Point2D(800,300) );
		arrayList.add( new Point2D(500,300) );
		shapeContainer.addShape( arrayList );
		arrayList.clear();
		arrayList.add( new Point2D(450,400) );
		arrayList.add( new Point2D(750,400) );
		arrayList.add( new Point2D(950,600) );
		arrayList.add( new Point2D(850,700) );
		arrayList.add( new Point2D(650,700) );
		shapeContainer.addShape( arrayList );
		arrayList.clear();
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		// The view is constantly redrawn by this method

		gw.set( paint, canvas );
		gw.clear( 0.0f, 0.0f, 0.0f );

		gw.setCoordinateSystemToWorldSpaceUnits();

		gw.setLineWidth( 1 );

		// draw a polygon around the currently selected shapes
		if ( selectedShapes.size() > 0 ) {
			ArrayList< Point2D > points = new ArrayList< Point2D >();
			AlignedRectangle2D rect = new AlignedRectangle2D();
			for ( Shape s : selectedShapes ) {
				for ( Point2D p : s.getPoints() ) {
					points.add( p );
					rect.bound( p );
				}
			}
			points = Point2DUtil.computeConvexHull( points );
			points = Point2DUtil.computeExpandedPolygon( points, rect.getDiagonal().length()/30 );

			gw.setColor( 1.0f, 0.0f, 0.0f, 0.8f );
			gw.fillPolygon( points );
		}

		// draw all the shapes
		shapeContainer.draw( gw, indexOfShapeBeingManipulated );

		gw.setCoordinateSystemToPixels();

		lassoButton.draw( gw, currentMode == MODE_LASSO );
		createButton.draw( gw, currentMode ==  MODE_CREATE);
		frameToShapeButton.draw(gw, currentMode == MODE_FRAMETOSHAPE);
		frameToSceneButton.draw(gw, currentMode == MODE_FRAMETOSCENE);
		eraseButton.draw(gw, currentMode == MODE_ERASE);

		/*if(lassoPolygonPoints != null && lassoPolygonPoints.size() > 0) {
			gw.setColor(0.0f, 0.0f, 1f, 0.5f);

			ArrayList<Point2D> w = new ArrayList<Point2D>();

			for(Point2D l : lassoPolygonPoints){

				w.add(gw.convertWorldSpaceUnitsToPixels(l));

			}

			gw.fillPolygon(w);
		}*/

		if ( currentMode == MODE_LASSO ) {
			MyCursor lassoCursor = cursorContainer.getCursorByType( MyCursor.TYPE_DRAGGING, 0 );
			if ( lassoCursor != null ) {
				gw.setColor(1.0f,0.0f,0.0f,0.5f);
				gw.fillPolygon( lassoCursor.getPositions() );
			}
		}


		if ( cursorContainer.getNumCursors() > 0 ) {
			gw.setFontHeight( 30 );
			gw.setLineWidth( 2 );
			gw.setColor( 1.0f, 1.0f, 1.0f );
			gw.drawString( 50, 50, "[" + cursorContainer.getNumCursors() + " contacts]");
		}

	}
	
	/**
	 * Returns a listener
	 * @return a listener
	 */
	private OnTouchListener getTouchListener(){
		if ( touchListener == null ) {
			touchListener = new OnTouchListener() {
				
				public boolean onTouch(View v, MotionEvent event) {

					int type = MotionEvent.ACTION_MOVE;
					switch ( event.getActionMasked() ) {
					case MotionEvent.ACTION_DOWN :
						type = MotionEvent.ACTION_DOWN;
						break;
					case MotionEvent.ACTION_MOVE :
						type = MotionEvent.ACTION_MOVE;
						break;
					case MotionEvent.ACTION_UP :
					case MotionEvent.ACTION_POINTER_UP :
					case MotionEvent.ACTION_CANCEL :
						type = MotionEvent.ACTION_UP;
						break;
					}


					int id = event.getPointerId(event.getActionIndex());
					float x = event.getX(event.getActionIndex());
					float y = event.getY(event.getActionIndex());
					// Find the cursor that corresponds to the event id, if such a cursor already exists.
					// If no such cursor exists, the below index will be -1, and the reference to cursor will be null.
					int cursorIndex = cursorContainer.findIndexOfCursorById( id );
					MyCursor cursor = (cursorIndex==-1) ? null : cursorContainer.getCursorByIndex( cursorIndex );

					if ( cursor == null ) {
						// The event does not correspond to any existing cursor.
						// In other words, this is a new finger touching the screen.
						// The event is probably of type DOWN.
						// A new cursor will need to be created for the event.
						if ( type == MotionEvent.ACTION_UP ) {
							// This should never happen, but if it does, just ignore the event.
							return true;
						}
						type = MotionEvent.ACTION_DOWN;
						// Cause a new cursor to be created to keep track of this event id in the future
						cursorIndex = cursorContainer.updateCursorById( id, x, y );
						cursor = cursorContainer.getCursorByIndex( cursorIndex );

						// we will set the type of the cursor later, by calling cursor.setType( MyCursor.TYPE_... );
					}
					else {
						// The event corresponds to an already existing cursor
						// (and the cursor was probably created during an earlier event of type TOUCH_EVENT_DOWN).
						// The current event is probably of type MOVE or UP.

						cursorContainer.updateCursorById( id, x, y );
						
						if ( type == MotionEvent.ACTION_MOVE ) {
							// Other fingers may have also moved, and there new positions are available in the event passed to us.
							// For safety, we update all cursors now with their latest positions.
							for ( int i = 0; i < event.getPointerCount(); ++i ) {
								int tmp_id = event.getPointerId(i);
								cursorContainer.updateCursorById( tmp_id, event.getX(i), event.getY(i) );
							}
						}
					}
					
					switch ( currentMode ) {
						case MODE_NEUTRAL:
							if (cursorContainer.getNumCursors() == 1 && type == MotionEvent.ACTION_DOWN) {
								Point2D p_pixels = new Point2D(x, y);
								Point2D p_world = gw.convertPixelsToWorldSpaceUnits(p_pixels);
								indexOfShapeBeingManipulated = shapeContainer.indexOfShapeContainingGivenPoint(p_world);

								// Hull
								ArrayList< Point2D > points = new ArrayList< Point2D >();
								AlignedRectangle2D rect = new AlignedRectangle2D();
								for ( Shape s : selectedShapes ) {
									for ( Point2D p : s.getPoints() ) {
										points.add( p );
										rect.bound( p );
									}
								}
								points = Point2DUtil.computeConvexHull( points );
								points = Point2DUtil.computeExpandedPolygon( points, rect.getDiagonal().length()/30 );

                                //Get polygon points

								if (lassoButton.contains(p_pixels)) {
									currentMode = MODE_LASSO;
									cursor.setType(MyCursor.TYPE_BUTTON);
								} else if (createButton.contains(p_pixels)) {
									currentMode = MODE_CREATE;
									cursor.setType(MyCursor.TYPE_BUTTON);
								}
								else if (frameToShapeButton.contains(p_pixels)) {
									currentMode = MODE_FRAMETOSHAPE;
									cursor.setType(MyCursor.TYPE_BUTTON);
								}
								else if (frameToSceneButton.contains(p_pixels)) {
									currentMode = MODE_FRAMETOSCENE;
								}
								else if (eraseButton.contains(p_pixels)) {
									currentMode = MODE_ERASE;
									cursor.setType(MyCursor.TYPE_BUTTON);
								}
								else if (indexOfShapeBeingManipulated >= 0) {
									currentMode = MODE_SHAPE_MANIPULATION;
									cursor.setType(MyCursor.TYPE_DRAGGING);
								}
								else if(lassoPolygonPoints != null && Point2DUtil.isPointInsidePolygon(points, p_world)){
									Log.d("IN HERE", "CLICKED ON POLYGON");
									currentMode = MODE_POLYGON_MANIPULATION;
									cursor.setType(MyCursor.TYPE_DRAGGING);
								}
								else {
									Log.d("OK cam", "onTouch: ");
									currentMode = MODE_CAMERA_MANIPULATION;
									cursor.setType(MyCursor.TYPE_DRAGGING);
								}
							}
							break;
						case MODE_CAMERA_MANIPULATION:
							if (type == MotionEvent.ACTION_MOVE) {
								if (cursorContainer.getNumCursors() == 1) {
									MyCursor cursor0 = cursorContainer.getCursorByIndex(0);

									gw.panBasedOnDisplacementOfOnePoint(
											cursor0.getPreviousPosition(),
											cursor0.getCurrentPosition()
									);

								} else if (cursorContainer.getNumCursors() == 2 ) {
									MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
									MyCursor cursor1 = cursorContainer.getCursorByIndex(1);
									// MyCursor otherCursor = ( cursor == cursor0 ) ? cursor1 : cursor0;
									gw.panAndZoomBasedOnDisplacementOfTwoPoints(
											cursor0.getPreviousPosition(),
											cursor1.getPreviousPosition(),
											cursor0.getCurrentPosition(),
											cursor1.getCurrentPosition()
									);
								}
							} else if (type == MotionEvent.ACTION_UP) {
								cursorContainer.removeCursorByIndex(cursorIndex);
								if (cursorContainer.getNumCursors() == 0)
									currentMode = MODE_NEUTRAL;
							}
							break;
						case MODE_SHAPE_MANIPULATION:
							if (type == MotionEvent.ACTION_MOVE && indexOfShapeBeingManipulated >= 0) {
								Shape shape = shapeContainer.getShape(indexOfShapeBeingManipulated);
								// translation of the shape with one finger
								if (cursorContainer.getNumCursors() == 1) {
									MyCursor cursor0 = cursorContainer.getCursorByIndex(0);

									Point2DUtil.translatePointsBasedOnDisplacementOfOnepoint(
											shape.getPoints(),
											gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
											gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition())
									);
								} else if (cursorContainer.getNumCursors() == 2) { // transformation of the shape with 2 fingers
									MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
									MyCursor cursor1 = cursorContainer.getCursorByIndex(1);

									Point2DUtil.transformPointsBasedOnDisplacementOfTwoPoints(
											shape.getPoints(),
											gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
											gw.convertPixelsToWorldSpaceUnits(cursor1.getPreviousPosition()),
											gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition()),
											gw.convertPixelsToWorldSpaceUnits(cursor1.getCurrentPosition())
									);
								}
							} else if (type == MotionEvent.ACTION_UP) {
								cursorContainer.removeCursorByIndex(cursorIndex);
								if (cursorContainer.getNumCursors() == 0) {
									currentMode = MODE_NEUTRAL;
									indexOfShapeBeingManipulated = -1;
								}
							}
							break;
                        case MODE_POLYGON_MANIPULATION:
							if (type == MotionEvent.ACTION_MOVE) {
								if (cursorContainer.getNumCursors() == 1) {
									MyCursor cursor0 = cursorContainer.getCursorByIndex(0);
									for ( Shape s : selectedShapes ) {
										Point2DUtil.translatePointsBasedOnDisplacementOfOnepoint(
												s.getPoints(),
												gw.convertPixelsToWorldSpaceUnits(cursor0.getPreviousPosition()),
												gw.convertPixelsToWorldSpaceUnits(cursor0.getCurrentPosition())
										);
									}
								}
							} else if (type == MotionEvent.ACTION_UP) {
								cursorContainer.removeCursorByIndex(cursorIndex);
								if (cursorContainer.getNumCursors() == 0)
									currentMode = MODE_NEUTRAL;
								indexOfShapeBeingManipulated = -1;
								Log.d("ICI", "onTouch: ");
							}
                            break;
						case MODE_LASSO:
							if (type == MotionEvent.ACTION_DOWN) {
								if (cursorContainer.getNumCursorsOfGivenType(MyCursor.TYPE_DRAGGING) == 1)
									// there's already a finger dragging out the lasso
									cursor.setType(MyCursor.TYPE_IGNORE);
								else
									cursor.setType(MyCursor.TYPE_DRAGGING);
							} else if (type == MotionEvent.ACTION_MOVE) {
								// no further updating necessary here
							} else if (type == MotionEvent.ACTION_UP) {
								if (cursor.getType() == MyCursor.TYPE_DRAGGING) {
									// complete a lasso selection
									selectedShapes.clear();

									// Need to transform the positions of the cursor from pixels to world space coordinates.
									// We will store the world space coordinates in the following data structure.
									lassoPolygonPoints = new ArrayList<Point2D>();
									for (Point2D p : cursor.getPositions())
										lassoPolygonPoints.add(gw.convertPixelsToWorldSpaceUnits(p));

									for (Shape s : shapeContainer.shapes) {
										if (s.isContainedInLassoPolygon(lassoPolygonPoints)) {
											selectedShapes.add(s);
										}
									}
								}
								cursorContainer.removeCursorByIndex(cursorIndex);
								if (cursorContainer.getNumCursors() == 0) {
									currentMode = MODE_NEUTRAL;
								}
							}
							break;
						case MODE_CREATE:

							if (type == MotionEvent.ACTION_DOWN) {

								Point2D p_pixels = new Point2D(x, y);

								if(createButton.contains(p_pixels) && cursorContainer.getNumCursors() <= 2){
									currentMode = MODE_NEUTRAL;
									cursorContainer.removeAllCursors();
									break;
								}

								cursor.setType(MyCursor.TYPE_CREATE);

								Log.d("NUMBER OF POINTS", String.valueOf(cursorContainer.getNumCursorsOfGivenType(MyCursor.TYPE_CREATE)));

							} else if (type == MotionEvent.ACTION_UP) {


								if (cursorContainer.getNumCursors() >= 3) {

									Log.d("CREATE", "DRAW SHAPE");

									ArrayList<Point2D> positions = cursorContainer.getPositionsOfCursorsOfType(MyCursor.TYPE_CREATE);

									Log.d("CREATE", String.valueOf(positions.size()));


									//Convert them to world units
									for (int i = 0; i < positions.size(); i++) {

										positions.set(i, gw.convertPixelsToWorldSpaceUnits(positions.get(i)));
									}

									shapeContainer.addShape((Point2DUtil.computeConvexHull(positions)));

									cursorContainer.removeAllCursors();

								}

								if (cursorContainer.getNumCursors() == 0) {
									currentMode = MODE_NEUTRAL;
								}
							}

							break;

						case MODE_FRAMETOSHAPE:
							if (type == MotionEvent.ACTION_DOWN) {
								Point2D p_pixels = new Point2D(x, y);
								if (frameToShapeButton.contains(p_pixels)) {
									currentMode = MODE_NEUTRAL;
									break;
								}

								Point2D p_world = gw.convertPixelsToWorldSpaceUnits(p_pixels);
								indexOfShapeBeingManipulated = shapeContainer.indexOfShapeContainingGivenPoint(p_world);
								if (indexOfShapeBeingManipulated != -1) {
									AlignedRectangle2D targetRect = shapeContainer.getShape(indexOfShapeBeingManipulated).getBoundingRectangle();
									gw.frame(targetRect, true);
									indexOfShapeBeingManipulated = -1;
									currentMode = MODE_NEUTRAL;
								}
							}
							break;

						case MODE_FRAMETOSCENE:

							gw.frame(shapeContainer.getBoundingRectangle(), true);
							currentMode = MODE_NEUTRAL;
							break;

						case MODE_ERASE:
							if (type == MotionEvent.ACTION_DOWN) {
								Point2D hitPoint = new Point2D(x, y);
								if (eraseButton.contains(hitPoint)) {
									currentMode = MODE_NEUTRAL;
									break;
								}

								Point2D p_world = gw.convertPixelsToWorldSpaceUnits(hitPoint);
								indexOfShapeBeingManipulated = shapeContainer.indexOfShapeContainingGivenPoint(p_world);
								if (indexOfShapeBeingManipulated != -1) {
									AlignedRectangle2D targetRect = shapeContainer.getShape(indexOfShapeBeingManipulated).getBoundingRectangle();
									selectedShapes.remove(shapeContainer.getShape(indexOfShapeBeingManipulated));
									shapeContainer.removeShape(indexOfShapeBeingManipulated);
									indexOfShapeBeingManipulated = -1;
									currentMode = MODE_NEUTRAL;
								}
							}
							break;
					}

					v.invalidate();
					
					return true;
				}
			};
		}
		return touchListener;
	}

}



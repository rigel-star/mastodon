package net.trackmate.model;

import static net.trackmate.graph.mempool.ByteUtils.DOUBLE_SIZE;
import static net.trackmate.graph.mempool.ByteUtils.INT_SIZE;
import net.imglib2.RealLocalizable;
import net.trackmate.graph.AbstractVertex;
import net.trackmate.graph.AbstractVertexPool;
import net.trackmate.graph.mempool.ByteMappedElement;
import net.trackmate.trackscheme.HasTimepoint;

/**
 * Base class for specialized vertices that are part of a graph, and are used to
 * store spatial and temporal location.
 * <p>
 * The class ships the minimal required feature, that is X, Y, Z, time-point,
 * and radius.
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 *
 * @param <V>
 *            the recursive type of the concrete implementation.
 */
public class AbstractSpot< V extends AbstractSpot< V >> extends AbstractVertex< V, Link< V >, ByteMappedElement > implements RealLocalizable, HasTimepoint
{
	protected static final int X_OFFSET = AbstractVertex.SIZE_IN_BYTES;
	protected static final int Y_OFFSET = X_OFFSET + DOUBLE_SIZE;
	protected static final int Z_OFFSET = Y_OFFSET + DOUBLE_SIZE;
	protected static final int TP_OFFSET = Z_OFFSET + DOUBLE_SIZE;
	protected static final int RADIUS_OFFSET = TP_OFFSET + DOUBLE_SIZE;
	protected static final int SIZE_IN_BYTES = RADIUS_OFFSET + INT_SIZE;

	@Override
	protected void setToUninitializedState()
	{
		super.setToUninitializedState();
	}

	AbstractSpot< V > init( final int timepointId, final double x, final double y, final double z, final double radius )
	{
		setX( x );
		setY( y );
		setZ( z );
		setTimePointId( timepointId );
		setRadius( radius );
		return this;
	}

	public double getX()
	{
		return access.getDouble( X_OFFSET );
	}

	public void setX( final double x )
	{
		access.putDouble( x, X_OFFSET );
	}

	public double getY()
	{
		return access.getDouble( Y_OFFSET );
	}

	public void setY( final double y )
	{
		access.putDouble( y, Y_OFFSET );
	}

	public double getZ()
	{
		return access.getDouble( Z_OFFSET );
	}

	public void setZ( final double z )
	{
		access.putDouble( z, Z_OFFSET );
	}

	public int getTimePointId()
	{
		return access.getInt( TP_OFFSET );
	}

	public void setTimePointId( final int tp )
	{
		access.putInt( tp, TP_OFFSET );
	}

	@Override
	public int getTimePoint()
	{
		return getTimePointId();
	}

	public void setRadius( final double radius )
	{
		access.putDouble( radius, RADIUS_OFFSET );
	}

	public double getRadius()
	{
		return access.getDouble( RADIUS_OFFSET );
	}

	AbstractSpot( final AbstractVertexPool< V, Link< V >, ByteMappedElement > pool )
	{
		super( pool );
	}

	// === RealLocalizable ===

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public void localize( final float[] position )
	{
		position[ 0 ] = ( float ) getX();
		position[ 1 ] = ( float ) getY();
		position[ 2 ] = ( float ) getZ();
	}

	@Override
	public void localize( final double[] position )
	{
		position[ 0 ] = getX();
		position[ 1 ] = getY();
		position[ 2 ] = getZ();
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return ( float ) getDoublePosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return ( d == 0 ) ? getX() : ( ( d == 1 ) ? getY() : getZ() );
	}

	/**
	 * Exposes the underlying ByteMappedElement for efficient IO operations.
	 *
	 * @return the underlying spot access object.
	 */
	ByteMappedElement getAccess()
	{
		return access;
	}

}
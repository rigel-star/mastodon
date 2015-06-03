package net.trackmate.trackscheme;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Layouting of a {@link TrackSchemeGraph} into layout coordinates.
 *
 * <p>
 * This determines the layoutX coordinates for all vertices. (The layoutY
 * coordinates of vertices are given by the timepoint.)
 *
 * <p>
 * See {@link VertexOrder}. for transforming layout coordinates to screen
 * coordinates.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class LineageTreeLayout
{
	/**
	 * Stores where a track ends in X position.
	 */
	final TDoubleArrayList columns = new TDoubleArrayList();

	List< String > columnNames = new ArrayList< String >();

	private double rightmost;

	private final TrackSchemeGraph graph;

	private int timestamp;

	private int mark;

	public LineageTreeLayout( final TrackSchemeGraph graph )
	{
		this.graph = graph;
		rightmost = 0;
		timestamp = 0;
	}

	/**
	 * Call {@link #layoutX(TrackSchemeVertex)} for every root.
	 */
	public void layoutX( final List< TrackSchemeVertex > layoutRoots )
	{
		layoutX( layoutRoots, -1 );
	}

	/**
	 * Call {@link #layoutX(TrackSchemeVertex)} for every root.
	 */
	public void layoutX( final List< TrackSchemeVertex > layoutRoots, final int mark )
	{
		reset();
		++timestamp;
		this.mark = mark;

		columns.clear();
		columnNames.clear();
		columns.add( 0 );
		for ( final TrackSchemeVertex root : layoutRoots )
		{
			layoutX( root );
			columns.add( rightmost );
			columnNames.add( "Root " + root.getLabel() );
		}
	}

	/**
	 * Get the timestamp that was used in the last layout (the timestamp which
	 * was set in all vertices laid out during last {@link #layoutX(List)}.)
	 *
	 * @return timestamp used in last layout.
	 */
	public int getCurrentLayoutTimestamp()
	{
		return timestamp;
	}

	/**
	 * Get a new layout timestamp.
	 * (The next layout will then use the timestamp after that).
	 *
	 * @return
	 */
	public int nextLayoutTimestamp()
	{
		++timestamp;
		return timestamp;
	}

	private void reset()
	{
		rightmost = 0;
	}

	/**
	 * Recursively lay out vertices such that
	 * <ul>
	 * <li>leafs are assigned layoutX = 0, 1, 2, ...
	 * <li>non-leafs are centered between first and last child's layoutX
	 * <li>for layout of vertices with more then one parent, only first incoming
	 * edge counts as parent edge
	 * </ul>
	 *
	 * @param v
	 *            root of sub-tree to layout.
	 */
	private void layoutX( final TrackSchemeVertex v )
	{
		int numLaidOutChildren = 0;
		double firstChildX = 0;
		double lastChildX = 0;

		if ( v.getLayoutTimestamp() < mark )
		{
			v.setGhost( true );
			v.setLayoutTimestamp( timestamp );
		}
		else
		{
			v.setGhost( false );
			v.setLayoutTimestamp( timestamp );
			if ( !v.outgoingEdges().isEmpty() && !v.isGhost() )
			{
				final TrackSchemeVertex child = graph.vertexRef();
				final TrackSchemeEdge edge = graph.edgeRef();
				final Iterator< TrackSchemeEdge > iterator = v.outgoingEdges().iterator();
				while ( layoutNextChild( iterator, child, edge ) )
				{
					if ( ++numLaidOutChildren == 1 )
						firstChildX = child.getLayoutX();
					else
						lastChildX = child.getLayoutX();
				}
				graph.releaseRef( edge );
				graph.releaseRef( child );
			}
		}

		switch( numLaidOutChildren )
		{
		case 0:
			v.setLayoutX( rightmost );
			rightmost += 1;
			break;
		case 1:
			v.setLayoutX( firstChildX );
			break;
		default:
			v.setLayoutX( ( firstChildX + lastChildX ) / 2 );
		}
	}

	private boolean layoutNextChild( final Iterator< TrackSchemeEdge > iterator, final TrackSchemeVertex child, final TrackSchemeEdge edge )
	{
		while ( iterator.hasNext() )
		{
			final TrackSchemeEdge next = iterator.next();
			next.getTarget( child );
			if ( child.getLayoutTimestamp() < timestamp )
			{
				child.setLayoutInEdgeIndex( next.getInternalPoolIndex() );
				layoutX( child );
				return true;
			}
		}
		return false;
	}
}

package org.mastodon.revised.ui.grouping;

import org.mastodon.revised.ui.selection.NavigationHandler;
import org.mastodon.revised.ui.selection.NavigationHandlerImp;
import org.mastodon.revised.ui.selection.NavigationListener;
import org.mastodon.util.Listeners;

/**
 * TODO
 *
 * @param <V>
 *            the type of vertices.
 * @param <E>
 *            the type of edges.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ForwardingNavigationHandler< V, E > implements NavigationHandler< V, E >, NavigationListener< V, E >, ForwardingModel< NavigationHandler< V, E > >
{
	private NavigationHandler< V, E > handler;

	private final Listeners.List< NavigationListener< V, E > > listeners = new Listeners.SynchronizedList<>();

	@Override
	public Listeners< NavigationListener< V, E > > listeners()
	{
		return listeners;
	}

	@Override
	public void navigateToVertex( final V vertex )
	{
		listeners.list.forEach( l -> l.navigateToVertex( vertex ) );
	}

	@Override
	public void navigateToEdge( final E edge )
	{
		listeners.list.forEach( l -> l.navigateToEdge( edge ) );
	}

	@Override
	public void notifyNavigateToVertex( final V vertex )
	{
		handler.notifyNavigateToVertex( vertex );
	}

	@Override
	public void notifyNavigateToEdge( final E edge )
	{
		handler.notifyNavigateToEdge( edge );
	}

	@Override
	public void linkTo( final NavigationHandler< V, E > newHandler, final boolean copyCurrentStateToNewModel )
	{
		if ( handler != null )
			handler.listeners().remove( this );
		newHandler.listeners().add( this );
		handler = newHandler;
	}

	@Override
	public NavigationHandler< V, E > asT()
	{
		return this;
	}

	public static class Factory< V, E> implements GroupableModelFactory< NavigationHandler< V, E > >
	{
		@Override
		public NavigationHandler< V, E > createBackingModel()
		{
			return new NavigationHandlerImp<>();
		}

		@Override
		public ForwardingModel< NavigationHandler< V, E > > createForwardingModel()
		{
			return new ForwardingNavigationHandler<>();
		}
	};
}

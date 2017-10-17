package org.mastodon.revised.mamut;

import bdv.spimdata.SpimDataMinimal;
import bdv.tools.ToggleDialogAction;
import bdv.viewer.RequestRepaint;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.mastodon.adapter.FocusModelAdapter;
import org.mastodon.adapter.HighlightModelAdapter;
import org.mastodon.adapter.NavigationHandlerAdapter;
import org.mastodon.adapter.RefBimap;
import org.mastodon.adapter.SelectionModelAdapter;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ListenableReadOnlyGraph;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.grouping.GroupManager;
import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightListener;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.TimepointListener;
import org.mastodon.model.TimepointModel;
import org.mastodon.revised.bdv.BigDataViewerMaMuT;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.overlay.BdvHighlightHandler;
import org.mastodon.revised.bdv.overlay.BdvSelectionBehaviours;
import org.mastodon.revised.bdv.overlay.EditBehaviours;
import org.mastodon.revised.bdv.overlay.EditSpecialBehaviours;
import org.mastodon.revised.bdv.overlay.OverlayContext;
import org.mastodon.revised.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.revised.bdv.overlay.OverlayNavigation;
import org.mastodon.revised.bdv.overlay.RenderSettings;
import org.mastodon.revised.bdv.overlay.RenderSettings.UpdateListener;
import org.mastodon.revised.bdv.overlay.ui.RenderSettingsDialog;
import org.mastodon.revised.bdv.overlay.wrap.OverlayContextWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayEdgeWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayEdgeWrapperBimap;
import org.mastodon.revised.bdv.overlay.wrap.OverlayGraphWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayVertexWrapper;
import org.mastodon.revised.bdv.overlay.wrap.OverlayVertexWrapperBimap;
import org.mastodon.revised.context.Context;
import org.mastodon.revised.context.ContextChooser;
import org.mastodon.revised.context.ContextListener;
import org.mastodon.revised.context.ContextProvider;
import org.mastodon.revised.model.mamut.BoundingSphereRadiusStatistics;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelOverlayProperties;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.trackscheme.TrackSchemeContextListener;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeEdgeBimap;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.TrackSchemeVertexBimap;
import org.mastodon.revised.trackscheme.display.TrackSchemeEditBehaviours;
import org.mastodon.revised.trackscheme.display.TrackSchemeFrame;
import org.mastodon.revised.trackscheme.display.TrackSchemeOptions;
import org.mastodon.revised.trackscheme.display.ui.TrackSchemeStyleChooser;
import org.mastodon.revised.trackscheme.wrap.DefaultModelGraphProperties;
import org.mastodon.revised.trackscheme.wrap.ModelGraphProperties;
import org.mastodon.revised.ui.HighlightBehaviours;
import org.mastodon.revised.ui.SelectionActions;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import static org.mastodon.revised.mamut.MamutAppModel.NAVIGATION;
import static org.mastodon.revised.mamut.MamutAppModel.TIMEPOINT;

public class WindowManager
{
	/**
	 * Information for one BigDataViewer window.
	 */
	public static class BdvWindow
	{
		private final ViewerFrame viewerFrame;

		private final OverlayGraphRenderer< ?, ? > tracksOverlay;

		private final GroupHandle groupHandle;

		private final ContextProvider< Spot > contextProvider;

		public BdvWindow(
				final ViewerFrame viewerFrame,
				final OverlayGraphRenderer< ?, ? > tracksOverlay,
				final GroupHandle groupHandle,
				final ContextProvider< Spot > contextProvider )
		{
			this.viewerFrame = viewerFrame;
			this.tracksOverlay = tracksOverlay;
			this.groupHandle = groupHandle;
			this.contextProvider = contextProvider;
		}

		public ViewerFrame getViewerFrame()
		{
			return viewerFrame;
		}

		public OverlayGraphRenderer< ?, ? > getTracksOverlay()
		{
			return tracksOverlay;
		}

		public GroupHandle getGroupHandle()
		{
			return groupHandle;
		}

		public ContextProvider< Spot > getContextProvider()
		{
			return contextProvider;
		}
	}

	/**
	 * Information for one TrackScheme window.
	 */
	public static class TsWindow
	{
		private final TrackSchemeFrame trackSchemeFrame;

		private final GroupHandle groupHandle;

		private final ContextChooser< Spot > contextChooser;

		public TsWindow(
				final TrackSchemeFrame trackSchemeFrame,
				final GroupHandle groupHandle,
				final ContextChooser< Spot > contextChooser )
		{
			this.trackSchemeFrame = trackSchemeFrame;
			this.groupHandle = groupHandle;
			this.contextChooser = contextChooser;
		}

		public TrackSchemeFrame getTrackSchemeFrame()
		{
			return trackSchemeFrame;
		}

		public GroupHandle getGroupHandle()
		{
			return groupHandle;
		}

		public ContextChooser< Spot > getContextChooser()
		{
			return contextChooser;
		}
	}

	/**
	 * TODO!!! related to {@link OverlayContextWrapper}
	 *
	 * @param <V>
	 *            the type of vertices in the model.
	 *
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	public static class BdvContextAdapter< V > implements ContextListener< V >, ContextProvider< V >
	{
		private final String contextProviderName;

		private final ArrayList< ContextListener< V > > listeners;

		private Context< V > context;

		public BdvContextAdapter( final String contextProviderName )
		{
			this.contextProviderName = contextProviderName;
			listeners = new ArrayList<>();
		}

		@Override
		public String getContextProviderName()
		{
			return contextProviderName;
		}

		@Override
		public synchronized boolean addContextListener( final ContextListener< V > l )
		{
			if ( !listeners.contains( l ) )
			{
				listeners.add( l );
				l.contextChanged( context );
				return true;
			}
			return false;
		}

		@Override
		public synchronized boolean removeContextListener( final ContextListener< V > l )
		{
			return listeners.remove( l );
		}

		@Override
		public synchronized void contextChanged( final Context< V > context )
		{
			this.context = context;
			for ( final ContextListener< V > l : listeners )
				l.contextChanged( context );
		}
	}

	private final InputTriggerConfig keyconf;

	private final KeyPressedManager keyPressedManager;

	private final MamutAppModel appModel;

	/**
	 * All currently open BigDataViewer windows.
	 */
	private final List< BdvWindow > bdvWindows = new ArrayList<>();

	/**
	 * The {@link ContextProvider}s of all currently open BigDataViewer windows.
	 */
	private final List< ContextProvider< Spot > > contextProviders = new ArrayList<>();

	/**
	 * All currently open TrackScheme windows.
	 */
	private final List< TsWindow > tsWindows = new ArrayList<>();

	public WindowManager(
			final String spimDataXmlFilename,
			final SpimDataMinimal spimData,
			final Model model,
			final InputTriggerConfig keyconf )
	{
		this.keyconf = keyconf;

		keyPressedManager = new KeyPressedManager();
		final RequestRepaint requestRepaint = () -> {
			for ( final BdvWindow w : bdvWindows )
				w.getViewerFrame().getViewerPanel().requestRepaint();
		};

		final ViewerOptions options = ViewerOptions.options()
				.inputTriggerConfig( keyconf )
				.shareKeyPressedEvents( keyPressedManager );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData( spimDataXmlFilename, spimData, options, requestRepaint );

		appModel = new MamutAppModel( model, sharedBdvData );
	}

	private synchronized void addBdvWindow( final BdvWindow w )
	{
		w.getViewerFrame().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				removeBdvWindow( w );
			}
		} );
		bdvWindows.add( w );
		contextProviders.add( w.getContextProvider() );
		for ( final TsWindow tsw : tsWindows )
			tsw.getContextChooser().updateContextProviders( contextProviders );
	}

	private synchronized void removeBdvWindow( final BdvWindow w )
	{
		bdvWindows.remove( w );
		contextProviders.remove( w.getContextProvider() );
		for ( final TsWindow tsw : tsWindows )
			tsw.getContextChooser().updateContextProviders( contextProviders );
	}

	private synchronized void addTsWindow( final TsWindow w )
	{
		w.getTrackSchemeFrame().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				removeTsWindow( w );
			}
		} );
		tsWindows.add( w );
		w.getContextChooser().updateContextProviders( contextProviders );
	}

	private synchronized void removeTsWindow( final TsWindow w )
	{
		tsWindows.remove( w );
		w.getContextChooser().updateContextProviders( new ArrayList<>() );
	}

	// TODO
	private int bdvName = 1;

	public void createBigDataViewer()
	{
		final Model model = appModel.getModel();
		final BoundingSphereRadiusStatistics radiusStats = appModel.getRadiusStats();
		final HighlightModel< Spot, Link > highlightModel = appModel.getHighlightModel();
		final FocusModel< Spot, Link > focusModel = appModel.getFocusModel();
		final SelectionModel< Spot, Link > selectionModel = appModel.getSelectionModel();
		final SharedBigDataViewerData sharedBdvData = appModel.getSharedBdvData();
		final GroupManager groupManager = appModel.getGroupManager();

		final GroupHandle bdvGroupHandle = groupManager.createGroupHandle();
		final TimepointModel timepointModel = bdvGroupHandle.getModel( TIMEPOINT );
		final NavigationHandler< Spot, Link > navigation = bdvGroupHandle.getModel( NAVIGATION );

		final OverlayGraphWrapper< Spot, Link > overlayGraph = new OverlayGraphWrapper<>(
				model.getGraph(),
				model.getGraphIdBimap(),
				model.getSpatioTemporalIndex(),
				new ModelOverlayProperties( model.getGraph(), radiusStats ) );
		final RefBimap< Spot, OverlayVertexWrapper< Spot, Link > > vertexMap = new OverlayVertexWrapperBimap<>( overlayGraph );
		final RefBimap< Link, OverlayEdgeWrapper< Spot, Link > > edgeMap = new OverlayEdgeWrapperBimap<>( overlayGraph );

		final HighlightModel< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayHighlight = new HighlightModelAdapter<>( highlightModel, vertexMap, edgeMap );
		final FocusModel< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayFocus = new FocusModelAdapter<>( focusModel, vertexMap, edgeMap );
		final SelectionModel< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlaySelection = new SelectionModelAdapter<>( selectionModel, vertexMap, edgeMap );
		final NavigationHandler< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayNavigationHandler = new NavigationHandlerAdapter<>( navigation, vertexMap, edgeMap );
		final String windowTitle = "BigDataViewer " + (bdvName++); // TODO: use JY naming scheme
		final BigDataViewerMaMuT bdv = BigDataViewerMaMuT.open( sharedBdvData, windowTitle, bdvGroupHandle );
		final ViewerFrame viewerFrame = bdv.getViewerFrame();
		final ViewerPanel viewer = bdv.getViewer();


		// TODO: It's ok to create the wrappers here, but wiring up Listeners should be done elsewhere


//		if ( !bdv.tryLoadSettings( bdvFile ) ) // TODO
//			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewer(), bdv.getSetupAssignments() );

		viewer.setTimepoint( timepointModel.getTimepoint() );
		final OverlayGraphRenderer< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > tracksOverlay = new OverlayGraphRenderer<>(
				overlayGraph,
				overlayHighlight,
				overlayFocus,
				overlaySelection );
		viewer.getDisplay().addOverlayRenderer( tracksOverlay );
		viewer.addRenderTransformListener( tracksOverlay );
		viewer.addTimePointListener( tracksOverlay );
		overlayHighlight.listeners().add( new HighlightListener()
		{
			@Override
			public void highlightChanged()
			{
				viewer.getDisplay().repaint();
			}
		} );
		overlayFocus.listeners().add( new FocusListener()
		{
			@Override
			public void focusChanged()
			{
				viewer.getDisplay().repaint();
			}
		} );
		model.getGraph().addGraphChangeListener( new GraphChangeListener()
		{
			@Override
			public void graphChanged()
			{
				viewer.getDisplay().repaint();
			}
		} );
		model.getGraph().addVertexPositionListener( ( v ) -> viewer.getDisplay().repaint() );
		overlaySelection.listeners().add( new SelectionListener()
		{
			@Override
			public void selectionChanged()
			{
				viewer.getDisplay().repaint();
			}
		} );
		// TODO: remember those listeners and remove them when the BDV window is closed!!!

		final OverlayNavigation< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayNavigation = new OverlayNavigation<>( viewer, overlayGraph );
		overlayNavigationHandler.listeners().add( overlayNavigation );

		final BdvHighlightHandler< ?, ? > highlightHandler = new BdvHighlightHandler<>( overlayGraph, tracksOverlay, overlayHighlight );
		viewer.getDisplay().addHandler( highlightHandler );
		viewer.addRenderTransformListener( highlightHandler );

		final BdvSelectionBehaviours< ?, ? > selectionBehaviours = new BdvSelectionBehaviours<>( overlayGraph, tracksOverlay, overlaySelection, overlayNavigationHandler );
		selectionBehaviours.installBehaviourBindings( viewerFrame.getTriggerbindings(), keyconf );

		final OverlayContext< OverlayVertexWrapper< Spot, Link > > overlayContext = new OverlayContext<>( overlayGraph, tracksOverlay );
		viewer.addRenderTransformListener( overlayContext );
		final BdvContextAdapter< Spot > contextProvider = new BdvContextAdapter<>( windowTitle );
		final OverlayContextWrapper< Spot, Link > overlayContextWrapper = new OverlayContextWrapper<>(
				overlayContext,
				contextProvider );

		UndoActions.installActionBindings( viewerFrame.getKeybindings(), model, keyconf );
		EditBehaviours.installActionBindings( viewerFrame.getTriggerbindings(), keyconf, overlayGraph, tracksOverlay, model );
		EditSpecialBehaviours.installActionBindings( viewerFrame.getTriggerbindings(), keyconf, viewerFrame.getViewerPanel(), overlayGraph, tracksOverlay, model );
		HighlightBehaviours.installActionBindings(
				viewerFrame.getTriggerbindings(),
				keyconf,
				new String[] { "bdv" },
				model.getGraph(),
				model.getGraph(),
				highlightModel,
				model );
		SelectionActions.installActionBindings(
				viewerFrame.getKeybindings(),
				keyconf,
				new String[] { "bdv" },
				model.getGraph(),
				model.getGraph(),
				selectionModel,
				model );

		viewer.addTimePointListener( new TimePointListener()
		{
			@Override
			public void timePointChanged( final int timePointIndex )
			{
				timepointModel.setTimepoint( timePointIndex );
			}
		} );
		timepointModel.listeners().add( new TimepointListener()
		{
			@Override
			public void timepointChanged()
			{
				viewer.setTimepoint( timepointModel.getTimepoint() );
			}
		} );

		// TODO revise
		// RenderSettingsDialog triggered by "R"
		final RenderSettings renderSettings = new RenderSettings(); // TODO should be in overlay eventually
		final String RENDER_SETTINGS = "render settings";
		final RenderSettingsDialog renderSettingsDialog = new RenderSettingsDialog( viewerFrame, renderSettings );
		final ActionMap actionMap = new ActionMap();
		new ToggleDialogAction( RENDER_SETTINGS, renderSettingsDialog ).put( actionMap );
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder a = keyconf.keyStrokeAdder( inputMap, "mamut" );
		a.put( RENDER_SETTINGS, "R" );
		viewerFrame.getKeybindings().addActionMap( "mamut", actionMap );
		viewerFrame.getKeybindings().addInputMap( "mamut", inputMap );
		renderSettings.addUpdateListener( new UpdateListener()
		{
			@Override
			public void renderSettingsChanged()
			{
				tracksOverlay.setRenderSettings( renderSettings );
				// TODO: less hacky way of triggering repaint and context update
				viewer.repaint();
				overlayContextWrapper.contextChanged( overlayContext );
			}
		} );

		final BdvWindow bdvWindow = new BdvWindow( viewerFrame, tracksOverlay, bdvGroupHandle, contextProvider );
		addBdvWindow( bdvWindow );
	}

	public void createTrackScheme()
	{
		final Model model = appModel.getModel();
		final BoundingSphereRadiusStatistics radiusStats = appModel.getRadiusStats();
		final HighlightModel< Spot, Link > highlightModel = appModel.getHighlightModel();
		final FocusModel< Spot, Link > focusModel = appModel.getFocusModel();
		final SelectionModel< Spot, Link > selectionModel = appModel.getSelectionModel();
		final SharedBigDataViewerData sharedBdvData = appModel.getSharedBdvData();
		final int maxTimepoint = appModel.getMaxTimepoint();
		final int minTimepoint = appModel.getMinTimepoint();
		final GroupManager groupManager = appModel.getGroupManager();

		final ListenableReadOnlyGraph< Spot, Link > graph = model.getGraph();
		final GraphIdBimap< Spot, Link > idmap = model.getGraphIdBimap();

		/*
		 * TrackSchemeGraph listening to model
		 */
		final ModelGraphProperties< Spot, Link > properties = new DefaultModelGraphProperties<>();
		final TrackSchemeGraph< Spot, Link > trackSchemeGraph = new TrackSchemeGraph<>( graph, idmap, properties );
		final RefBimap< Spot, TrackSchemeVertex > vertexMap = new TrackSchemeVertexBimap<>( idmap, trackSchemeGraph );
		final RefBimap< Link, TrackSchemeEdge > edgeMap = new TrackSchemeEdgeBimap<>( idmap, trackSchemeGraph );

		/*
		 * TrackSchemeHighlight wrapping HighlightModel
		 */
		final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > trackSchemeHighlight = new HighlightModelAdapter<>( highlightModel, vertexMap, edgeMap );

		/*
		 * TrackScheme selectionModel
		 */
		final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > trackSchemeSelection = new SelectionModelAdapter<>( selectionModel, vertexMap, edgeMap );

		/*
		 * TrackScheme GroupHandle
		 */
		final GroupHandle groupHandle = groupManager.createGroupHandle();

		/*
		 * TimepointModel forwarding to current group
		 */
		final TimepointModel timepointModel = groupHandle.getModel( TIMEPOINT );


		/*
		 * TrackScheme navigation
		 */
		final NavigationHandler< Spot, Link > navigation = groupHandle.getModel( NAVIGATION );
		final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > trackSchemeNavigation = new NavigationHandlerAdapter<>( navigation, vertexMap, edgeMap );

		/*
		 * TrackScheme focus
		 */
		final FocusModel< TrackSchemeVertex, TrackSchemeEdge > trackSchemeFocus = new FocusModelAdapter<>( focusModel, vertexMap, edgeMap );

		/*
		 * TrackScheme ContextChooser
		 */
		final TrackSchemeContextListener< Spot > contextListener = new TrackSchemeContextListener<>(
				idmap,
				trackSchemeGraph );
		final ContextChooser< Spot > contextChooser = new ContextChooser<>( contextListener );

		/*
		 * show TrackSchemeFrame
		 */
		final TrackSchemeOptions options = TrackSchemeOptions.options()
				.inputTriggerConfig( keyconf )
				.shareKeyPressedEvents( keyPressedManager );
		final TrackSchemeFrame frame = new TrackSchemeFrame(
				trackSchemeGraph,
				trackSchemeHighlight,
				trackSchemeFocus,
				timepointModel,
				trackSchemeSelection,
				trackSchemeNavigation,
				model,
				groupHandle,
				contextChooser,
				options );
		frame.getTrackschemePanel().setTimepointRange( minTimepoint, maxTimepoint );
		frame.getTrackschemePanel().graphChanged();
		contextListener.setContextListener( frame.getTrackschemePanel() );
		frame.setVisible( true );

		UndoActions.installActionBindings( frame.getKeybindings(), model, keyconf );
		HighlightBehaviours.installActionBindings(
				frame.getTriggerbindings(),
				keyconf,
				new String[] { "ts" },
				model.getGraph(),
				model.getGraph(),
				highlightModel,
				model );
		SelectionActions.installActionBindings(
				frame.getKeybindings(),
				keyconf,
				new String[] { "ts" },
				model.getGraph(),
				model.getGraph(),
				selectionModel,
				model );
		TrackSchemeEditBehaviours.installActionBindings(
				frame.getTriggerbindings(),
				keyconf,
				frame.getTrackschemePanel(),
				trackSchemeGraph,
				frame.getTrackschemePanel().getGraphOverlay(),
				model.getGraph(),
				model.getGraph().getGraphIdBimap(),
				model );

		// TrackSchemeStyleDialog triggered by "R"
		final String TRACK_SCHEME_STYLE_SETTINGS = "render settings";
		final TrackSchemeStyleChooser styleChooser = new TrackSchemeStyleChooser( frame, frame.getTrackschemePanel() );
		final JDialog styleDialog = styleChooser.getDialog();
		final ActionMap actionMap = new ActionMap();
		new ToggleDialogAction( TRACK_SCHEME_STYLE_SETTINGS, styleDialog ).put( actionMap );
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder a = keyconf.keyStrokeAdder( inputMap, "mamut" );
		a.put( TRACK_SCHEME_STYLE_SETTINGS, "R" );
		frame.getKeybindings().addActionMap( "mamut", actionMap );
		frame.getKeybindings().addInputMap( "mamut", inputMap );

		final TsWindow tsWindow = new TsWindow( frame, groupHandle, contextChooser );
		addTsWindow( tsWindow );
		frame.getTrackschemePanel().repaint();
	}

	public void closeAllWindows()
	{
		final ArrayList< JFrame > frames = new ArrayList<>();
		for ( final BdvWindow w : bdvWindows )
			frames.add( w.getViewerFrame() );
		for ( final TsWindow w : tsWindows )
			frames.add( w.getTrackSchemeFrame() );
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				for ( final JFrame f : frames )
					f.dispatchEvent( new WindowEvent( f, WindowEvent.WINDOW_CLOSING ) );
			}
		} );
	}

	public Model getModel()
	{
		return appModel.getModel();
	}

	public AbstractSpimData< ? > getSpimData()
	{
		return appModel.getSharedBdvData().getSpimData();
	}


	// TODO: move somewhere else. make bdvWindows, tsWindows accessible.
	public static class DumpInputConfig
	{
		private static List< InputTriggerDescription > buildDescriptions( final WindowManager wm ) throws IOException
		{
			final InputTriggerDescriptionsBuilder builder = new InputTriggerDescriptionsBuilder();

			final ViewerFrame viewerFrame = wm.bdvWindows.get( 0 ).viewerFrame;
			builder.addMap( viewerFrame.getKeybindings().getConcatenatedInputMap(), "bdv" );
			builder.addMap( viewerFrame.getTriggerbindings().getConcatenatedInputTriggerMap(), "bdv" );

			final TrackSchemeFrame trackschemeFrame = wm.tsWindows.get( 0 ).trackSchemeFrame;
			builder.addMap( trackschemeFrame.getKeybindings().getConcatenatedInputMap(), "ts" );
			builder.addMap( trackschemeFrame.getTriggerbindings().getConcatenatedInputTriggerMap(), "ts" );

			return builder.getDescriptions();
		}

		public static boolean mkdirs( final String fileName )
		{
			final File dir = new File( fileName ).getParentFile();
			return dir == null ? false : dir.mkdirs();
		}

		public static void writeToYaml( final String fileName, final WindowManager wm ) throws IOException
		{
			mkdirs( fileName );
			YamlConfigIO.write( buildDescriptions( wm ), fileName );
		}
	}
}

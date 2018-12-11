/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.AbstractMapEventLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mobi.maptrek.data.MapObject;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.data.style.MarkerStyle;
import mobi.maptrek.data.style.TrackStyle;
import mobi.maptrek.fragments.About;
import mobi.maptrek.fragments.AmenityInformation;
import mobi.maptrek.fragments.AmenitySetupDialog;
import mobi.maptrek.fragments.BaseMapDownload;
import mobi.maptrek.fragments.CrashReport;
import mobi.maptrek.fragments.DataExport;
import mobi.maptrek.fragments.DataList;
import mobi.maptrek.fragments.DataSourceList;
import mobi.maptrek.fragments.FragmentHolder;
import mobi.maptrek.fragments.LocationInformation;
import mobi.maptrek.fragments.LocationShareDialog;
import mobi.maptrek.fragments.MapList;
import mobi.maptrek.fragments.MapSelection;
import mobi.maptrek.fragments.MarkerInformation;
import mobi.maptrek.fragments.OnBackPressedListener;
import mobi.maptrek.fragments.OnFeatureActionListener;
import mobi.maptrek.fragments.OnLocationListener;
import mobi.maptrek.fragments.OnMapActionListener;
import mobi.maptrek.fragments.OnTrackActionListener;
import mobi.maptrek.fragments.OnWaypointActionListener;
import mobi.maptrek.fragments.PanelMenuFragment;
import mobi.maptrek.fragments.PanelMenuItem;
import mobi.maptrek.fragments.Settings;
import mobi.maptrek.fragments.TextSearchFragment;
import mobi.maptrek.fragments.TrackInformation;
import mobi.maptrek.fragments.TrackProperties;
import mobi.maptrek.fragments.WaypointInformation;
import mobi.maptrek.fragments.WaypointProperties;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.TrackManager;
import mobi.maptrek.layers.CrosshairLayer;
import mobi.maptrek.layers.CurrentTrackLayer;
import mobi.maptrek.layers.LocationOverlay;
import mobi.maptrek.layers.MapCoverageLayer;
import mobi.maptrek.layers.MapEventLayer;
import mobi.maptrek.layers.MapObjectLayer;
import mobi.maptrek.layers.NavigationLayer;
import mobi.maptrek.layers.TrackLayer;
import mobi.maptrek.layers.building.BuildingLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerLayer;
import mobi.maptrek.layers.marker.MarkerSymbol;
import mobi.maptrek.location.BaseLocationService;
import mobi.maptrek.location.BaseNavigationService;
import mobi.maptrek.location.ILocationListener;
import mobi.maptrek.location.ILocationService;
import mobi.maptrek.location.INavigationService;
import mobi.maptrek.location.LocationService;
import mobi.maptrek.location.NavigationService;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.maps.MapIndex;
import mobi.maptrek.maps.MapService;
import mobi.maptrek.maps.Themes;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.LabelTileLoaderHook;
import mobi.maptrek.maps.maptrek.MapTrekTileSource;
import mobi.maptrek.maps.maptrek.Tags;
import mobi.maptrek.provider.ExportProvider;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.MathUtils;
import mobi.maptrek.util.Osm;
import mobi.maptrek.util.OsmcSymbolFactory;
import mobi.maptrek.util.ProgressHandler;
import mobi.maptrek.util.ShieldFactory;
import mobi.maptrek.util.StringFormatter;
import mobi.maptrek.util.SunriseSunset;
import mobi.maptrek.view.Gauge;
import mobi.maptrek.view.GaugePanel;

public class MainActivity extends BasePluginActivity implements ILocationListener,
        DataHolder,
        MapHolder,
        Map.InputListener,
        Map.UpdateListener,
        GestureListener,
        FragmentHolder,
        WaypointProperties.OnWaypointPropertiesChangedListener,
        TrackProperties.OnTrackPropertiesChangedListener,
        OnLocationListener,
        OnWaypointActionListener,
        OnTrackActionListener,
        OnMapActionListener,
        OnFeatureActionListener,
        ItemizedLayer.OnItemGestureListener<MarkerItem>,
        PopupMenu.OnMenuItemClickListener,
        LoaderManager.LoaderCallbacks<List<FileDataSource>>,
        FragmentManager.OnBackStackChangedListener,
        MapTrekTileSource.OnDataMissingListener, AmenitySetupDialog.AmenitySetupDialogCallback {
    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private static final int MAP_EVENTS = 1;
    private static final int MAP_BASE = 2;
    private static final int MAP_MAPS = 3;
    private static final int MAP_MAP_OVERLAYS = 4;
    private static final int MAP_3D = 5;
    private static final int MAP_LABELS = 6;
    private static final int MAP_DATA = 7;
    private static final int MAP_3D_DATA = 8;
    private static final int MAP_POSITIONAL = 9;
    private static final int MAP_OVERLAYS = 10;

    public static final int MAP_POSITION_ANIMATION_DURATION = 500;
    public static final int MAP_BEARING_ANIMATION_DURATION = 300;
    public static final int MAP_ZOOM_ANIMATION_DURATION = 100;

    private static final int NIGHT_CHECK_PERIOD = 180000; // 3 minutes
    private static final int TRACK_ROTATION_DELAY = 1000; // 1 second

    public static final String NEW_APPLICATION_STORAGE = "-=MOVE_TO_APP=-";
    public static final String NEW_EXTERNAL_STORAGE = "-=MOVE_TO_ROOT=-";
    public static final String NEW_SD_STORAGE = "-=MOVE_TO_SD=-";

    public enum TRACKING_STATE {
        DISABLED,
        PENDING,
        TRACKING
    }

    private enum PANEL_STATE {
        NONE,
        LOCATION,
        RECORD,
        PLACES,
        MAPS,
        MORE
    }

    private enum NIGHT_MODE_STATE {
        AUTO,
        DAY,
        NIGHT
    }

    private float mFingerTipSize;
    private int mStatusBarHeight;
    private int mColorAccent;
    private int mColorPrimaryDark;
    private int mPanelSolidBackground;
    private int mPanelBackground;
    private int mPanelExtendedBackground;

    private ProgressHandler mProgressHandler;

    private ILocationService mLocationService = null;
    private boolean mIsLocationBound = false;
    private INavigationService mNavigationService = null;
    private boolean mIsNavigationBound = false;
    private LocationState mLocationState;
    private LocationState mSavedLocationState;
    private LocationState mPreviousLocationState;
    private TRACKING_STATE mTrackingState;
    private MapPosition mMapPosition = new MapPosition();
    private GeoPoint mSelectedPoint;
    private boolean mPositionLocked = false;
    private int mMovingOffset = 0;
    private int mTrackingOffset = 0;
    private double mTrackingOffsetFactor = 1;
    private long mTrackingDelay;
    private float mAutoTilt;
    private boolean mAutoTiltSet;
    private boolean mAutoTiltShouldSet;
    private boolean mBuildingsLayerEnabled = true;
    private boolean mHideMapObjects = true;
    private int mBitmapMapTransparency = 0;

    protected Map mMap;
    protected MapView mMapView;
    private GaugePanel mGaugePanel;
    private TextView mSatellitesText;
    private View mMapButtonHolder;
    private ImageButton mLocationButton;
    private ImageButton mRecordButton;
    private ImageButton mPlacesButton;
    private ImageButton mMapsButton;
    private ImageButton mMoreButton;
    private Button mMapDownloadButton;
    private View mCompassView;
    private View mNavigationArrowView;
    private ViewGroup mExtendPanel;
    private TextView mLicense;
    private ProgressBar mProgressBar;
    private FloatingActionButton mActionButton;
    private FloatingActionButton mListActionButton;
    private CoordinatorLayout mCoordinatorLayout;
    private View mPopupAnchor;
    private boolean mVerticalOrientation;
    private int mSlideGravity;

    private long mStartTime;
    private long mLastLocationMilliseconds = 0L;
    private int mMovementAnimationDuration = BaseLocationService.LOCATION_DELAY;
    private float mAveragedBearing = 0f;

    private SunriseSunset mSunriseSunset;
    private NIGHT_MODE_STATE mNightModeState;
    private boolean mNightMode = false;
    private long mNextNightCheck = 0L;

    private VectorDrawable mNavigationNorthDrawable;
    private VectorDrawable mNavigationTrackDrawable;
    private VectorDrawable mMyLocationDrawable;
    private VectorDrawable mLocationSearchingDrawable;

    private MapEventLayer mMapEventLayer;
    private VectorTileLayer mBaseLayer;
    private BitmapTileLayer mHillshadeLayer;
    private BuildingLayer mBuildingsLayer;
    private MapScaleBarLayer mMapScaleBarLayer;
    private LabelTileLoaderHook mLabelTileLoaderHook;
    private LabelLayer mLabelsLayer;
    private TileGridLayer mGridLayer;
    private NavigationLayer mNavigationLayer;
    private CurrentTrackLayer mCurrentTrackLayer;
    private ItemizedLayer<MarkerItem> mMarkerLayer;
    private CrosshairLayer mCrosshairLayer;
    private LocationOverlay mLocationOverlay;
    private MapCoverageLayer mMapCoverageLayer;
    private MarkerItem mActiveMarker;

    private FragmentManager mFragmentManager;
    private DataFragment mDataFragment;
    private PANEL_STATE mPanelState;
    private boolean secondBack;
    private Toast mBackToast;

    private MapIndex mMapIndex;
    private Index mNativeMapIndex;
    private MapTrekTileSource mNativeTileSource;
    private MapFile mBitmapLayerMap;
    private WaypointDbDataSource mWaypointDbDataSource;
    private List<FileDataSource> mData = new ArrayList<>();
    private Waypoint mEditedWaypoint;
    private Track mEditedTrack;
    private int mTotalDataItems = 0;
    private boolean mFirstMove = true;
    private boolean mBaseMapWarningShown = false;
    private ShieldFactory mShieldFactory;
    private OsmcSymbolFactory mOsmcSymbolFactory;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler mMainHandler;

    private WaypointBroadcastReceiver mWaypointBroadcastReceiver;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate()");

        logger.error("ES: {}", Configuration.getExternalStorage());
        logger.error("New ES: {}", Configuration.getNewExternalStorage());
        if (Configuration.getNewExternalStorage() != null) {
            startActivity(new Intent(this, DataMoveActivity.class));
            finish();
            return;
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_main);

        MapTrek application = MapTrek.getApplication();

        Resources resources = getResources();
        Resources.Theme theme = getTheme();
        mColorAccent = resources.getColor(R.color.colorAccent, theme);
        mColorPrimaryDark = resources.getColor(R.color.colorPrimaryDark, theme);
        mPanelBackground = resources.getColor(R.color.panelBackground, theme);
        mPanelSolidBackground = resources.getColor(R.color.panelSolidBackground, theme);
        mPanelExtendedBackground = resources.getColor(R.color.panelExtendedBackground, theme);
        mStatusBarHeight = getStatusBarHeight();

        mMainHandler = new Handler(Looper.getMainLooper());
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.setPriority(Thread.MIN_PRIORITY);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        // Estimate finger tip height (0.1 inch is obtained from experiments)
        mFingerTipSize = (float) (MapTrek.ydpi * 0.1);

        mSunriseSunset = new SunriseSunset();
        //noinspection ConstantConditions
        //mNightModeState = BuildConfig.FULL_VERSION ? NIGHT_MODE_STATE.values()[Configuration.getNightModeState()] : NIGHT_MODE_STATE.DAY;
        mNightModeState = NIGHT_MODE_STATE.DAY;

        // Apply default styles at start
        TrackStyle.DEFAULT_COLOR = resources.getColor(R.color.trackColor, theme);
        TrackStyle.DEFAULT_WIDTH = resources.getInteger(R.integer.trackWidth);

        // find the retained fragment on activity restarts
        mFragmentManager = getFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        mDataFragment = (DataFragment) mFragmentManager.findFragmentByTag("data");

        File mapsDir = application.getExternalDir("maps");

        // Provide application context so that maps can be cached on rotation
        mNativeMapIndex = application.getMapIndex();

        if (mDataFragment == null) {
            // add the fragment
            mDataFragment = new DataFragment();
            mFragmentManager.beginTransaction().add(mDataFragment, "data").commit();

            // Provide application context so that maps can be cached on rotation
            mMapIndex = new MapIndex(getApplicationContext(), mapsDir);
            if (BuildConfig.FULL_VERSION) {
                initializePlugins();
                mMapIndex.initializeOnlineMapProviders();
            }

            mBitmapLayerMap = mMapIndex.getMap(Configuration.getBitmapMap());

            String language = Configuration.getLanguage();
            if (language == null) {
                if (BuildConfig.RUSSIAN_EDITION) {
                    language = "ru";
                } else {
                    language = resources.getConfiguration().locale.getLanguage();
                    if (!Arrays.asList(new String[]{"en", "de", "ru"}).contains(language))
                        language = "none";
                }
                Configuration.setLanguage(language);
            }

            mShieldFactory = new ShieldFactory();
            mOsmcSymbolFactory = new OsmcSymbolFactory();
        } else {
            mMapIndex = mDataFragment.getMapIndex();
            mEditedWaypoint = mDataFragment.getEditedWaypoint();
            mBitmapLayerMap = mDataFragment.getBitmapLayerMap();
            mShieldFactory = mDataFragment.getShieldFactory();
            mOsmcSymbolFactory = mDataFragment.getOsmcSymbolFactory();
        }

        mLocationState = LocationState.DISABLED;
        mSavedLocationState = LocationState.DISABLED;

        mPanelState = PANEL_STATE.NONE;

        mCoordinatorLayout = findViewById(R.id.coordinatorLayout);
        mActionButton = findViewById(R.id.actionButton);
        mListActionButton = findViewById(R.id.listActionButton);
        mLocationButton = findViewById(R.id.locationButton);
        mRecordButton = findViewById(R.id.recordButton);
        mPlacesButton = findViewById(R.id.placesButton);
        mMapsButton = findViewById(R.id.mapsButton);
        mMoreButton = findViewById(R.id.moreButton);
        mMapDownloadButton = findViewById(R.id.mapDownloadButton);
        mLicense = findViewById(R.id.license);
        mLicense.setClickable(true);
        mLicense.setMovementMethod(LinkMovementMethod.getInstance());
        mPopupAnchor = findViewById(R.id.popupAnchor);

        mGaugePanel = findViewById(R.id.gaugePanel);
        mGaugePanel.setTag(Boolean.TRUE);
        mGaugePanel.setMapHolder(this);

        mSatellitesText = findViewById(R.id.satellites);
        mMapButtonHolder = findViewById(R.id.mapButtonHolder);
        mCompassView = findViewById(R.id.compass);
        mNavigationArrowView = findViewById(R.id.navigationArrow);
        mNavigationArrowView.setOnClickListener(v -> {
            MapObject mapObject = mNavigationService.getWaypoint();
            setMapLocation(mapObject.coordinates);
        });
        mNavigationArrowView.setOnLongClickListener(v -> {
            showNavigationMenu();
            return true;
        });
        mExtendPanel = findViewById(R.id.extendPanel);
        mProgressBar = findViewById(R.id.progressBar);

        mExtendPanel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = v.getWidth();
            int height = v.getHeight();
            logger.debug("onLayoutChange({}, {})", width, height);
            if (width == 0 || height == 0) {
                v.setTranslationX(0f);
                v.setTranslationY(0f);
                return;
            }
            int rootWidth = mCoordinatorLayout.getWidth();
            int rootHeight = mCoordinatorLayout.getHeight();
            switch (mPanelState) {
                case RECORD:
                    if (mVerticalOrientation) {
                        int cWidth = (int) (mRecordButton.getWidth() + mRecordButton.getX());
                        if (width < cWidth)
                            v.setTranslationX(cWidth - width);
                    }
                    break;
                case PLACES:
                    if (mVerticalOrientation) {
                        int cWidth = (int) (mPlacesButton.getWidth() + mPlacesButton.getX());
                        if (width < cWidth)
                            v.setTranslationX(cWidth - width);
                    }
                    break;
                case MAPS:
                    if (mVerticalOrientation) {
                        int cWidth = (int) (rootWidth - mMapsButton.getX());
                        if (width < cWidth)
                            v.setTranslationX(mMapsButton.getX());
                        else
                            v.setTranslationX(rootWidth - width);
                    } else {
                        v.setTranslationY(rootHeight - height);
                    }
                    break;
                case MORE:
                    if (mVerticalOrientation) {
                        v.setTranslationX(rootWidth - width);
                    } else {
                        v.setTranslationY(rootHeight - height);
                    }
            }
        });

        mExtendPanel.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                if (mVerticalOrientation)
                    return;
                switch (mPanelState) {
                    case RECORD:
                        child.setMinimumHeight((int) (mRecordButton.getHeight() + mRecordButton.getY()));
                        break;
                    case PLACES:
                        child.setMinimumHeight((int) (mPlacesButton.getHeight() + mPlacesButton.getY()));
                        break;
                    case MAPS:
                        child.setMinimumHeight((int) (mCoordinatorLayout.getHeight() - mMapsButton.getY()));
                        break;
                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });

        int lastIntroduction = Configuration.getLastSeenIntroduction();

        mMapView = findViewById(R.id.mapView);
        mMap = mMapView.map();
        if (lastIntroduction == 0) {
            if (BuildConfig.RUSSIAN_EDITION) {
                mMap.setMapPosition(56.4, 39, 1 << 5);
            } else {
                // Set initial location based on device language
                switch (resources.getConfiguration().locale.getLanguage()) {
                    case "de":
                        mMap.setMapPosition(50.8, 10.45, (1 << 6) * 1.5);
                        break;
                    case "ru":
                        mMap.setMapPosition(56.4, 39, 1 << 5);
                        break;
                    default:
                        mMap.setMapPosition(-19, -12, 1 << 2);
                }
            }
        } else {
            mMap.setMapPosition(Configuration.getPosition());
        }
        mAutoTilt = Configuration.getAutoTilt();

        mNavigationNorthDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_north, theme);
        mNavigationTrackDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_navigation_track, theme);
        mMyLocationDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_my_location, theme);
        mLocationSearchingDrawable = (VectorDrawable) resources.getDrawable(R.drawable.ic_location_searching, theme);

        Layers layers = mMap.layers();
        layers.addGroup(MAP_EVENTS);
        layers.addGroup(MAP_BASE);

        mNativeTileSource = new MapTrekTileSource(application.getDetailedMapDatabase());
        mNativeTileSource.setContoursEnabled(Configuration.getContoursEnabled());

        mBaseLayer = new OsmTileLayer(mMap);
        mBaseLayer.setTileSource(mNativeTileSource);

        mMap.setBaseMap(mBaseLayer); // will go to base group
        mNativeTileSource.setOnDataMissingListener(this);

        // setBaseMap does not operate with layer groups so we add remaining groups later
        layers.addGroup(MAP_MAPS);
        layers.addGroup(MAP_MAP_OVERLAYS);
        layers.addGroup(MAP_3D);
        layers.addGroup(MAP_LABELS);
        layers.addGroup(MAP_DATA);
        layers.addGroup(MAP_3D_DATA);
        layers.addGroup(MAP_POSITIONAL);
        layers.addGroup(MAP_OVERLAYS);

        if (Configuration.getHillshadesEnabled())
            showHillShade();

        mGridLayer = new TileGridLayer(mMap, MapTrek.density * .75f);
        if (Configuration.getGridLayerEnabled())
            layers.add(mGridLayer, MAP_OVERLAYS);

        mBuildingsLayerEnabled = Configuration.getBuildingsLayerEnabled();
        if (mBuildingsLayerEnabled) {
            mBuildingsLayer = new BuildingLayer(mMap, mBaseLayer);
            layers.add(mBuildingsLayer, MAP_3D);
        }

        mLabelTileLoaderHook = new LabelTileLoaderHook(mShieldFactory, mOsmcSymbolFactory);
        String language = Configuration.getLanguage();
        if (!"none".equals(language))
            mLabelTileLoaderHook.setPreferredLanguage(language);
        mLabelsLayer = new LabelLayer(mMap, mBaseLayer, mLabelTileLoaderHook);
        layers.add(mLabelsLayer, MAP_LABELS);

        DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap, MapTrek.density * .75f);
        mMapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
        mCrosshairLayer = new CrosshairLayer(mMap, MapTrek.density);
        mLocationOverlay = new LocationOverlay(mMap, MapTrek.density);
        layers.add(mMapScaleBarLayer, MAP_OVERLAYS);
        layers.add(mCrosshairLayer, MAP_OVERLAYS);
        layers.add(mLocationOverlay, MAP_POSITIONAL);

        layers.add(new MapObjectLayer(mMap, MapTrek.density), MAP_3D_DATA);

        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this));
        MarkerSymbol symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER);
        mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<>(), symbol, MapTrek.density, this);
        layers.add(mMarkerLayer, MAP_3D_DATA);

        // Load waypoints
        mWaypointDbDataSource = application.getWaypointDbDataSource();
        mWaypointDbDataSource.open();
        for (Waypoint waypoint : mWaypointDbDataSource.getWaypoints()) {
            if (mEditedWaypoint != null && mEditedWaypoint._id == waypoint._id)
                mEditedWaypoint = waypoint;
            addWaypointMarker(waypoint);
            mTotalDataItems++;
        }
        mWaypointBroadcastReceiver = new WaypointBroadcastReceiver();
        registerReceiver(mWaypointBroadcastReceiver, new IntentFilter(WaypointDbDataSource.BROADCAST_WAYPOINTS_MODIFIED));
        registerReceiver(mWaypointBroadcastReceiver, new IntentFilter(WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED));
        registerReceiver(mWaypointBroadcastReceiver, new IntentFilter(WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN));

        mHideMapObjects = Configuration.getHideMapObjects();
        mBitmapMapTransparency = Configuration.getBitmapMapTransparency();
        if (mBitmapLayerMap != null)
            showBitmapMap(mBitmapLayerMap, false);

        setNightMode(false);
        //setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT ||
        //        savedInstanceState != null && savedInstanceState.getBoolean("nightMode"));

        //if (BuildConfig.DEBUG)
        //    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

        mBackToast = Toast.makeText(this, R.string.msgBackQuit, Toast.LENGTH_SHORT);
        mProgressHandler = new ProgressHandler(mProgressBar);

        // Initialize UI event handlers
        mLocationButton.setOnClickListener(v -> onLocationClicked());
        mLocationButton.setOnLongClickListener(v -> {
            onLocationLongClicked();
            return true;
        });
        mRecordButton.setOnClickListener(v -> onRecordClicked());
        mRecordButton.setOnLongClickListener(v -> {
            onRecordLongClicked();
            return true;
        });
        mPlacesButton.setOnClickListener(v -> onPlacesClicked());
        mPlacesButton.setOnLongClickListener(v -> {
            onPlacesLongClicked();
            return true;
        });
        mMapsButton.setOnClickListener(v -> {
            if (BuildConfig.FULL_VERSION) {
                onMapsClicked();
            } else {
                onMapsLongClicked();
            }
        });
        mMapsButton.setOnLongClickListener(v -> {
            onMapsLongClicked();
            return true;
        });
        mMoreButton.setOnClickListener(v -> onMoreClicked());
        mMoreButton.setOnLongClickListener(v -> {
            onMoreLongClicked();
            return true;
        });
        mMapDownloadButton.setOnClickListener(v -> onMapDownloadClicked());

        // Resume state
        int state = Configuration.getLocationState();
        if (state >= LocationState.NORTH.ordinal())
            mSavedLocationState = LocationState.values()[state];
        state = Configuration.getPreviousLocationState();
        mPreviousLocationState = LocationState.values()[state];
        state = Configuration.getTrackingState();
        mTrackingState = TRACKING_STATE.values()[state];

        mGaugePanel.initializeGauges(Configuration.getGauges());
        showActionPanel(Configuration.getActionPanelState(), false);

        boolean visible = Configuration.getZoomButtonsVisible();
        mCoordinatorLayout.findViewById(R.id.mapZoomHolder).setVisibility(visible ? View.VISIBLE : View.GONE);

        // Resume navigation
        MapObject mapObject = Configuration.getNavigationPoint();
        if (mapObject != null)
            startNavigation(mapObject);

        // Initialize data loader
        getLoaderManager();

        // Remove splash from background
        window.setBackgroundDrawable(new ColorDrawable(resources.getColor(R.color.colorBackground, theme)));
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() ^ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Get back to full screen mode after edge swipe
        /*
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        boolean hide = Configuration.getHideSystemUI();
                        if (hide && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            final Message m = Message.obtain(mMainHandler, new Runnable() {
                                @Override
                                public void run() {
                                    hideSystemUI();
                                }
                            });
                            m.what = R.id.msgHideSystemUI;
                            mMainHandler.sendMessageDelayed(m, 5000);
                        }
                    }
                });
        */

        /*
        mCoordinatorLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                final int statusBar = insets.getSystemWindowInsetTop();
                final int navigationBar = insets.getSystemWindowInsetBottom();
                //mStatusBarHeight = statusBar;
                logger.debug("setOnApplyWindowInsetsListener({}, {})", statusBar, navigationBar);
                FrameLayout.MarginLayoutParams p = (FrameLayout.MarginLayoutParams) v.getLayoutParams();
                //p.topMargin = mStatusBarHeight;
                //v.requestLayout();
                return insets.consumeSystemWindowInsets();
            }
        });
        */

        mStartTime = SystemClock.uptimeMillis();

        onNewIntent(getIntent());

        if (lastIntroduction < IntroductionActivity.CURRENT_INTRODUCTION)
            startActivity(new Intent(this, IntroductionActivity.class));
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        logger.debug("New intent: {}", action);
        String scheme = intent.getScheme();
        if ("mobi.maptrek.action.CENTER_ON_COORDINATES".equals(action)) {
            MapPosition position = mMap.getMapPosition();
            double lat = intent.getDoubleExtra("lat", position.getLatitude());
            double lon = intent.getDoubleExtra("lon", position.getLongitude());
            position.setPosition(lat, lon);
            setMapLocation(position.getGeoPoint());
        } else if ("mobi.maptrek.action.NAVIGATE_TO_OBJECT".equals(action)) {
            startNavigation(intent.getLongExtra(NavigationService.EXTRA_ID, 0L));
        } else if ("mobi.maptrek.action.MOVE_DATA".equals(action)) {
            final AtomicInteger selected = new AtomicInteger(0);
            boolean hasSDCard = MapTrek.getApplication().hasSDCard();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.titleMoveData);
            CharSequence[] items = new CharSequence[hasSDCard ? 2 : 1];
            final String[] storageVariants = new String[hasSDCard ? 2 : 1];
            String externalStorage = Configuration.getExternalStorage();
            if (externalStorage != null) {
                items[0] = getString(R.string.msgMoveDataToApplicationStorage);
                storageVariants[0] = NEW_APPLICATION_STORAGE;
            } else {
                items[0] = getString(R.string.msgMoveDataToExternalStorage);
                storageVariants[0] = NEW_EXTERNAL_STORAGE;
            }
            if (hasSDCard) {
                if (MapTrek.getApplication().getSDCardDirectory().getAbsolutePath().equals(externalStorage)) {
                    items[1] = getString(R.string.msgMoveDataToExternalStorage);
                    storageVariants[1] = NEW_EXTERNAL_STORAGE;
                } else {
                    items[1] = getString(R.string.msgMoveDataToSDCard);
                    storageVariants[1] = NEW_SD_STORAGE;
                }
            }
            builder.setSingleChoiceItems(items, selected.get(), (dialog, which) -> selected.set(which));
            builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
                final int item = selected.get();
                if (!NEW_APPLICATION_STORAGE.equals(storageVariants[item])) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setTitle(R.string.actionMoveData);
                    if (NEW_EXTERNAL_STORAGE.equals(storageVariants[item]))
                        builder1.setMessage(R.string.msgMoveDataToExternalStorageExplanation);
                    else
                        builder1.setMessage(R.string.msgMoveDataToSDCardExplanation);
                    builder1.setPositiveButton(R.string.actionContinue, (dialog1, which1) -> {
                        Configuration.setNewExternalStorage(storageVariants[item]);
                        MapTrek.getApplication().restart(MainActivity.this, DataMoveActivity.class);
                    });
                    builder1.setNegativeButton(R.string.cancel, null);
                    AlertDialog nextDialog = builder1.create();
                    nextDialog.show();
                } else {
                    Configuration.setNewExternalStorage(storageVariants[item]);
                    MapTrek.getApplication().restart(MainActivity.this, DataMoveActivity.class);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if ("mobi.maptrek.action.RESET_ADVICES".equals(action)) {
            mBackgroundHandler.postDelayed(Configuration::resetAdviceState, 10000); // Delay reset so that advices are not shown immediately after reset
            Snackbar snackbar = Snackbar.make(mCoordinatorLayout, R.string.msgAdvicesReset, Snackbar.LENGTH_LONG);
            snackbar.show();
        } else if ("geo".equals(scheme)) {
            Uri uri = intent.getData();
            if (uri == null)
                return;
            logger.debug("   {}", uri);
            String data = uri.getSchemeSpecificPart();
            String query = uri.getQuery();
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom
            // geo:0,0?q=lat,lng(label)
            // geo:0,0?q=lat, lng - buggy Instagram (with space)
            int zoom = 0;
            if (query != null) {
                data = data.substring(0, data.indexOf(query) - 1);
                if (query.startsWith("z="))
                    try {
                        zoom = Integer.parseInt(query.substring(2));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
            }
            try {
                String[] ll = data.split(",");
                String marker = null;
                double lat = Double.parseDouble(ll[0]);
                double lon = Double.parseDouble(ll[1]);
                if (lat == 0d && lon == 0d && query != null) {
                    // Parse query string
                    int bracket = query.indexOf("(");
                    if (bracket > -1)
                        data = query.substring(2, query.indexOf("("));
                    else
                        data = query.substring(2);
                    ll = data.split(",\\s*");
                    lat = Double.parseDouble(ll[0]);
                    lon = Double.parseDouble(ll[1]);
                    if (bracket > -1) {
                        marker = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
                    }
                }
                MapPosition position = mMap.getMapPosition();
                position.setPosition(lat, lon);
                if (zoom > 0)
                    position.setZoomLevel(zoom);
                mMap.setMapPosition(position);
                showMarkerInformation(position.getGeoPoint(), marker);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            // http://wiki.openstreetmap.org/wiki/Shortlink
            Uri uri = intent.getData();
            if (uri == null)
                return;
            logger.debug("   {}", uri);
            List<String> path = uri.getPathSegments();
            if ("go".equals(path.get(0))) {
                MapPosition position = Osm.decodeShortLink(path.get(1));
                String marker = uri.getQueryParameter("m");
                mMap.setMapPosition(position);
                showMarkerInformation(position.getGeoPoint(), marker);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.debug("onStart()");

        // Start loading user data
        DataLoader loader = (DataLoader) getLoaderManager().initLoader(0, null, this);
        loader.setProgressHandler(mProgressHandler);

        registerReceiver(mBroadcastReceiver, new IntentFilter(MapService.BROADCAST_MAP_ADDED));
        registerReceiver(mBroadcastReceiver, new IntentFilter(MapService.BROADCAST_MAP_REMOVED));
        registerReceiver(mBroadcastReceiver, new IntentFilter(BaseLocationService.BROADCAST_TRACK_SAVE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATUS));
        registerReceiver(mBroadcastReceiver, new IntentFilter(NavigationService.BROADCAST_NAVIGATION_STATE));

        MapTrek.isMainActivityRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.debug("onResume()");

        if (mSavedLocationState != LocationState.DISABLED)
            askForPermission();
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            enableTracking();
            startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_BACKGROUND_TRACK));
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(NavigationService.PREF_NAVIGATION_BACKGROUND, false)) {
            startService(new Intent(getApplicationContext(), NavigationService.class).setAction(BaseNavigationService.DISABLE_BACKGROUND_NAVIGATION));
            enableNavigation();
        }

        mVerticalOrientation = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        mSlideGravity = mVerticalOrientation ? Gravity.BOTTOM : Gravity.END;

        mMapEventLayer = new MapEventLayer(mMap, this);
        mMap.layers().add(mMapEventLayer, MAP_EVENTS);
        mMap.events.bind(this);
        mMap.input.bind(this);
        mMapView.onResume();
        updateLocationDrawable();
        adjustCompass(mMap.getMapPosition().bearing);

        mLicense.setText(Html.fromHtml(getString(R.string.osmLicense)));
        mLicense.setVisibility(View.VISIBLE);
        final Message m = Message.obtain(mMainHandler,
                () -> mLicense.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLicense.setVisibility(View.GONE);
                        mLicense.animate().setListener(null);
                    }
                }));
        m.what = R.id.msgRemoveLicense;
        mMainHandler.sendMessageDelayed(m, 10000);

        String userNotification = MapTrek.getApplication().getUserNotification();
        if (userNotification != null)
            HelperUtils.showError(userNotification, mCoordinatorLayout);

        if (MapTrek.getApplication().hasPreviousRunsExceptions()) {
            Fragment fragment = Fragment.instantiate(this, CrashReport.class.getName());
            fragment.setEnterTransition(new Slide());
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "crashReport");
            ft.addToBackStack("crashReport");
            ft.commit();
        } else if (!mBaseMapWarningShown && mNativeMapIndex.getBaseMapVersion() == 0) {
            BaseMapDownload fragment = (BaseMapDownload) Fragment.instantiate(this, BaseMapDownload.class.getName());
            fragment.setMapIndex(mNativeMapIndex);
            fragment.setEnterTransition(new Slide());
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "baseMapDownload");
            ft.addToBackStack("baseMapDownload");
            ft.commit();
            mBaseMapWarningShown = true;
        }

        if (Configuration.getHideSystemUI())
            hideSystemUI();

        updateMapViewArea();
        mMap.updateMap(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.debug("onPause()");

        if (mLocationState != LocationState.SEARCHING)
            mSavedLocationState = mLocationState;

        mMapView.onPause();
        mMap.events.unbind(this);
        mMap.layers().remove(mMapEventLayer);
        mMapEventLayer = null;
        mGaugePanel.onVisibilityChanged(false);

        // save the map position and state
        Configuration.setPosition(mMap.getMapPosition());
        Configuration.setBitmapMap(mBitmapLayerMap);
        Configuration.setLocationState(mSavedLocationState.ordinal());
        Configuration.setPreviousLocationState(mPreviousLocationState.ordinal());
        Configuration.setTrackingState(mTrackingState.ordinal());
        Configuration.setGauges(mGaugePanel.getGaugeSettings());

        if (!isChangingConfigurations()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            if (mTrackingState == TRACKING_STATE.TRACKING)
                startService(intent.setAction(BaseLocationService.ENABLE_BACKGROUND_TRACK));
            else
                stopService(intent);

            if (mNavigationService != null) {
                intent = new Intent(getApplicationContext(), NavigationService.class);
                if (mNavigationService.isNavigating())
                    startService(intent.setAction(BaseNavigationService.ENABLE_BACKGROUND_NAVIGATION));
                else
                    stopService(intent);
            }
            disableNavigation();
            disableLocations();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        logger.debug("onStop()");

        MapTrek.isMainActivityRunning = false;

        unregisterReceiver(mBroadcastReceiver);

        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null) {
            ((DataLoader) loader).setProgressHandler(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.debug("onDestroy()");

        long runningTime = (SystemClock.uptimeMillis() - mStartTime) / 60000;
        Configuration.updateRunningTime(runningTime);

        if (mMap != null)
            mMap.destroy();
        //mMapScaleBar.destroy();

        for (FileDataSource source : mData)
            source.setVisible(false);

        if (mWaypointBroadcastReceiver != null) {
            unregisterReceiver(mWaypointBroadcastReceiver);
            mWaypointBroadcastReceiver = null;
        }
        if (mWaypointDbDataSource != null)
            mWaypointDbDataSource.close();

        mProgressHandler = null;

        if (mBackgroundThread != null) {
            logger.debug("  stopping threads...");
            mBackgroundThread.interrupt();
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundThread.quit();
            mBackgroundThread = null;
        }

        mMainHandler = null;

        if (isFinishing()) {
            if (mMapIndex != null)
                mMapIndex.clear();
            sendBroadcast(new Intent("mobi.maptrek.plugins.action.FINALIZE"));
            if (mShieldFactory != null)
                mShieldFactory.dispose();
            if (mOsmcSymbolFactory != null)
                mOsmcSymbolFactory.dispose();
        }

        mFragmentManager = null;

        Configuration.commit();
        logger.debug("  done!");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        logger.debug("onSaveInstanceState()");

        if (mLocationService != null)
            startService(new Intent(getApplicationContext(), LocationService.class));
        if (mNavigationService != null)
            startService(new Intent(getApplicationContext(), NavigationService.class));

        mDataFragment.setMapIndex(mMapIndex);
        mDataFragment.setEditedWaypoint(mEditedWaypoint);
        mDataFragment.setBitmapLayerMap(mBitmapLayerMap);
        mDataFragment.setShieldFactory(mShieldFactory);
        mDataFragment.setOsmcSymbolFactory(mOsmcSymbolFactory);

        savedInstanceState.putSerializable("savedLocationState", mSavedLocationState);
        savedInstanceState.putSerializable("previousLocationState", mPreviousLocationState);
        savedInstanceState.putLong("lastLocationMilliseconds", mLastLocationMilliseconds);
        savedInstanceState.putFloat("averagedBearing", mAveragedBearing);
        savedInstanceState.putInt("movementAnimationDuration", mMovementAnimationDuration);
        savedInstanceState.putBoolean("savedNavigationState", mNavigationService != null);
        if (mProgressBar.getVisibility() == View.VISIBLE)
            savedInstanceState.putInt("progressBar", mProgressBar.getMax());
        savedInstanceState.putSerializable("panelState", mPanelState);
        savedInstanceState.putBoolean("nightMode", mNightMode);
        savedInstanceState.putBoolean("autoTiltShouldSet", mAutoTiltShouldSet);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        logger.debug("onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        mSavedLocationState = (LocationState) savedInstanceState.getSerializable("savedLocationState");
        mPreviousLocationState = (LocationState) savedInstanceState.getSerializable("previousLocationState");
        mLastLocationMilliseconds = savedInstanceState.getLong("lastLocationMilliseconds");
        mAveragedBearing = savedInstanceState.getFloat("averagedBearing");
        mMovementAnimationDuration = savedInstanceState.getInt("movementAnimationDuration");
        if (savedInstanceState.getBoolean("savedNavigationState", false))
            enableNavigation();
        if (savedInstanceState.containsKey("progressBar")) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setMax(savedInstanceState.getInt("progressBar"));
        }
        mAutoTiltShouldSet = savedInstanceState.getBoolean("autoTiltShouldSet");
        setPanelState((PANEL_STATE) savedInstanceState.getSerializable("panelState"));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionNightMode: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionNightMode);
                builder.setItems(R.array.night_mode_array, (dialog, which) -> {
                    mNightModeState = NIGHT_MODE_STATE.values()[which];
                    if ((mNightModeState == NIGHT_MODE_STATE.NIGHT) != mNightMode) {
                        // With rule categories it became a long lasting operation
                        // so it has to be run in background
                        mBackgroundHandler.post(() -> setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT));
                        Configuration.setNightModeState(mNightModeState.ordinal());
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.actionStyle: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionStyle);
                builder.setItems(R.array.mapStyles, (dialog, which) -> {
                    Configuration.setMapStyle(which);
                    // With rule categories it became a long lasting operation
                    // so it has to be run in background
                    mBackgroundHandler.post(() -> {
                        //TODO Refactor
                        setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT);
                    });
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.actionFontSize: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionFontSize);
                builder.setItems(R.array.font_size_array, (dialog, which) -> {
                    Configuration.setMapFontSize(which);
                    // With rule categories it became a long lasting operation
                    // so it has to be run in background
                    mBackgroundHandler.post(() -> {
                        //TODO Refactor
                        setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT);
                    });
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.actionLanguage: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionLanguage);
                builder.setItems(R.array.language_array, (dialog, which) -> {
                    String[] languageCodes = getResources().getStringArray(R.array.language_code_array);
                    String language = languageCodes[which];
                    if ("none".equals(language)) {
                        mLabelTileLoaderHook.setPreferredLanguage(null);
                    } else {
                        mLabelTileLoaderHook.setPreferredLanguage(language);
                    }
                    mMap.clearMap();
                    Configuration.setLanguage(language);
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.actionAmenityZooms: {
                AmenitySetupDialog.Builder builder = new AmenitySetupDialog.Builder();
                AmenitySetupDialog dialog = builder.setCallback(this).create();
                dialog.show(getFragmentManager(), "amenitySetup");
                return true;
            }
            case R.id.actionOtherFeatures: {
                PanelMenuFragment fragment = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
                fragment.setMenu(R.menu.menu_map_features, menu -> {
                    menu.findItem(R.id.action3dBuildings).setChecked(mBuildingsLayerEnabled);
                    menu.findItem(R.id.actionHillshades).setChecked(Configuration.getHillshadesEnabled());
                    menu.findItem(R.id.actionContours).setChecked(Configuration.getContoursEnabled());
                    menu.findItem(R.id.actionGrid).setChecked(mMap.layers().contains(mGridLayer));
                });
                showExtendPanel(PANEL_STATE.MAPS, "mapFeaturesMenu", fragment);
                return true;
            }
            case R.id.actionActivity: {
                int activity = Configuration.getActivity();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionActivity);
                builder.setSingleChoiceItems(R.array.activities, activity, (dialog, which) -> {
                    dialog.dismiss();
                    Configuration.setActivity(which);
                    // With rule categories it became a long lasting operation
                    // so it has to be run in background
                    mBackgroundHandler.post(() -> {
                        //TODO Refactor
                        setNightMode(mNightModeState == NIGHT_MODE_STATE.NIGHT);
                    });
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
            case R.id.action3dBuildings: {
                mBuildingsLayerEnabled = item.isChecked();
                if (mBuildingsLayerEnabled) {
                    mBuildingsLayer = new BuildingLayer(mMap, mBaseLayer);
                    mMap.layers().add(mBuildingsLayer, MAP_3D);
                    // Let buildings be re-fetched from map layer
                    mMap.clearMap();
                } else {
                    mMap.layers().remove(mBuildingsLayer);
                    mBuildingsLayer = null;
                }
                Configuration.setBuildingsLayerEnabled(mBuildingsLayerEnabled);
                mMap.updateMap(true);
                return true;
            }
            case R.id.actionHillshades: {
                // layer is managed in event subscription as it can be configured in other places
                Configuration.setHillshadesEnabled(item.isChecked());
                return true;
            }
            case R.id.actionContours: {
                mNativeTileSource.setContoursEnabled(item.isChecked());
                mMap.clearMap();
                Configuration.setContoursEnabled(item.isChecked());
                return true;
            }
            case R.id.actionGrid: {
                if (item.isChecked()) {
                    mMap.layers().add(mGridLayer, MAP_OVERLAYS);
                } else {
                    mMap.layers().remove(mGridLayer);
                }
                Configuration.setGridLayerEnabled(item.isChecked());
                mMap.updateMap(true);
                return true;
            }
            case R.id.actionAutoTilt: {
                mMap.getMapPosition(mMapPosition);
                if (item.isChecked()) {
                    Configuration.setAutoTilt(65f);
                    mAutoTilt = 65f;
                } else {
                    Configuration.setAutoTilt(-1f);
                    mAutoTilt = -1f;
                    if (mAutoTiltSet) {
                        mMapPosition.setTilt(0f);
                        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
                        mAutoTiltSet = false;
                    }
                }
                return true;
            }
            case R.id.actionOverviewRoute: {
                if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                    mLocationState = LocationState.ENABLED;
                    updateLocationDrawable();
                }
                BoundingBox box = new BoundingBox();
                mMap.getMapPosition(mMapPosition);
                box.extend(mMapPosition.getLatitude(), mMapPosition.getLongitude());
                MapObject mapObject = mNavigationService.getWaypoint();
                box.extend(mapObject.coordinates.getLatitude(), mapObject.coordinates.getLongitude());
                box.extendBy(0.05);
                mMap.animator().animateTo(box);
                return true;
            }
            case R.id.actionStopNavigation: {
                stopNavigation();
                return true;
            }
            case R.id.actionManageMaps: {
                startMapSelection(true);
                return true;
            }
            case R.id.actionHideSystemUI: {
                if (Configuration.getHideSystemUI())
                    showSystemUI();
                else
                    hideSystemUI();
                return true;
            }
            case R.id.actionAddGauge: {
                mGaugePanel.onLongClick(mGaugePanel);
                return true;
            }
            case R.id.actionRate: {
                Snackbar snackbar = Snackbar
                        .make(mCoordinatorLayout, R.string.msgRateApplication, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.iamin, view -> {
                            String packageName = getPackageName();
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                            } catch (ActivityNotFoundException ignore) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                            }
                        }).addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                Configuration.setRatingActionPerformed();
                            }
                        });
                TextView snackbarTextView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                snackbarTextView.setMaxLines(99);
                snackbar.show();
                return true;
            }
            case R.id.actionSettings: {
                Bundle args = new Bundle(1);
                args.putBoolean(Settings.ARG_HILLSHADES_AVAILABLE, mNativeMapIndex.hasHillshades());
                Fragment fragment = Fragment.instantiate(this, Settings.class.getName(), args);
                fragment.setEnterTransition(new Slide(mSlideGravity));
                fragment.setReturnTransition(new Slide(mSlideGravity));
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "settings");
                ft.addToBackStack("settings");
                ft.commit();
                return true;
            }
            case R.id.actionSearch: {
                Bundle args = new Bundle(2);
                if (mLocationState != LocationState.DISABLED && mLocationService != null) {
                    Location location = mLocationService.getLocation();
                    args.putDouble(DataList.ARG_LATITUDE, location.getLatitude());
                    args.putDouble(DataList.ARG_LONGITUDE, location.getLongitude());
                } else {
                    MapPosition position = mMap.getMapPosition();
                    args.putDouble(DataList.ARG_LATITUDE, position.getLatitude());
                    args.putDouble(DataList.ARG_LONGITUDE, position.getLongitude());
                }
                if (mFragmentManager.getBackStackEntryCount() > 0) {
                    popAll();
                }
                Fragment fragment = Fragment.instantiate(this, TextSearchFragment.class.getName(), args);
                showExtendPanel(PANEL_STATE.MORE, "search", fragment);
                return true;
            }
            case R.id.actionAbout: {
                Fragment fragment = Fragment.instantiate(this, About.class.getName());
                fragment.setEnterTransition(new Slide(mSlideGravity));
                fragment.setReturnTransition(new Slide(mSlideGravity));
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.contentPanel, fragment, "about");
                ft.addToBackStack("about");
                ft.commit();
                return true;
            }
            case R.id.actionShareCoordinates: {
                removeMarker();
                shareLocation(mSelectedPoint, null);
                return true;
            }
            case R.id.actionAddWaypointHere: {
                removeMarker();
                String name = getString(R.string.place_name, Configuration.getPointsCounter());
                onWaypointCreate(mSelectedPoint, name, false, true);
                return true;
            }
            case R.id.actionNavigateHere: {
                removeMarker();
                MapObject mapObject = new MapObject(mSelectedPoint.getLatitude(), mSelectedPoint.getLongitude());
                mapObject.name = getString(R.string.selectedLocation);
                startNavigation(mapObject);
                return true;
            }
            case R.id.actionRememberScale: {
                HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_REMEMBER_SCALE, R.string.advice_remember_scale, mPopupAnchor, true);
                removeMarker();
                mMap.getMapPosition(mMapPosition);
                Configuration.setRememberedScale((float) mMapPosition.getScale());
                return true;
            }
            case R.id.actionRememberTilt: {
                removeMarker();
                mMap.getMapPosition(mMapPosition);
                mAutoTilt = mMapPosition.getTilt();
                Configuration.setAutoTilt(mAutoTilt);
                mAutoTiltSet = true;
                mAutoTiltShouldSet = true;
                return true;
            }
            default: {
                Intent intent = item.getIntent();
                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onLocationChanged() {
        if (mLocationState == LocationState.SEARCHING) {
            mLocationState = mSavedLocationState;
            //TODO Change from center to location pivot (see zooming)
            mMap.getEventLayer().setFixOnCenter(true);
            updateLocationDrawable();
            mLocationOverlay.setEnabled(true);
            mMap.updateMap(true);
        }

        Location location = mLocationService.getLocation();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float bearing = location.getBearing();
        if (bearing < mAveragedBearing - 180f)
            mAveragedBearing -= 360f;
        else if (mAveragedBearing < bearing - 180f)
            mAveragedBearing += 360f;
        mAveragedBearing = (float) movingAverage(bearing, mAveragedBearing);
        if (mAveragedBearing < 0f)
            mAveragedBearing += 360f;
        if (mAveragedBearing >= 360f)
            mAveragedBearing -= 360f;

        updateGauges();

        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            long time = SystemClock.uptimeMillis();
            // Adjust map movement animation to location acquisition period to make movement smoother
            long locationDelay = time - mLastLocationMilliseconds;
            double duration = Math.min(1500, locationDelay); // 1.5 seconds maximum
            mMovementAnimationDuration = (int) movingAverage(duration, mMovementAnimationDuration);
            // Update map position
            mMap.getMapPosition(mMapPosition);

            boolean rotate = mLocationState == LocationState.TRACK && mTrackingDelay < time;
            double offset;
            if (rotate) {
                offset = mTrackingOffset / mTrackingOffsetFactor;
                if (mAutoTilt > 0f && !mAutoTiltSet && mAutoTiltShouldSet)
                    mMapPosition.setTilt(mAutoTilt);
            } else {
                offset = mMovingOffset;
            }
            offset = offset / (mMapPosition.scale * Tile.SIZE);

            double rad = Math.toRadians(mAveragedBearing);
            double dx = offset * Math.sin(rad);
            double dy = offset * Math.cos(rad);

            if (!mPositionLocked) {
                mMapPosition.setX(MercatorProjection.longitudeToX(lon) + dx);
                mMapPosition.setY(MercatorProjection.latitudeToY(lat) - dy);
                mMapPosition.setBearing(-mAveragedBearing);
                //FIXME VTM
                mMap.animator().animateTo(mMovementAnimationDuration, mMapPosition, rotate);
            }
        }

        mLocationOverlay.setPosition(lat, lon, bearing);
        if (mNavigationLayer != null)
            mNavigationLayer.setPosition(lat, lon);
        mLastLocationMilliseconds = SystemClock.uptimeMillis();
        //if (mNightModeState == NIGHT_MODE_STATE.AUTO)
        //    checkNightMode(location);

        for (WeakReference<LocationChangeListener> weakRef : mLocationChangeListeners) {
            LocationChangeListener locationChangeListener = weakRef.get();
            if (locationChangeListener != null) {
                locationChangeListener.onLocationChanged(location);
            }
        }
    }

    @Override
    public void onGpsStatusChanged() {
        logger.debug("onGpsStatusChanged()");
        if (mLocationService.getStatus() == LocationService.GPS_SEARCHING) {
            int satellites = mLocationService.getSatellites();
            mSatellitesText.setText(String.format(Locale.getDefault(), "%d / %s", satellites >> 7, satellites & 0x7f));
            if (mLocationState != LocationState.SEARCHING) {
                mSavedLocationState = mLocationState;
                mLocationState = LocationState.SEARCHING;
                mMap.getEventLayer().setFixOnCenter(false);
                mLocationOverlay.setEnabled(false);
                updateLocationDrawable();
            }
        }
        updateNavigationUI();
    }

    private void onLocationClicked() {
        switch (mLocationState) {
            case DISABLED:
                askForPermission();
                break;
            case SEARCHING:
                mLocationState = LocationState.DISABLED;
                disableLocations();
                break;
            case ENABLED:
                mLocationState = mPreviousLocationState;
                mPreviousLocationState = LocationState.NORTH;
                mMap.getEventLayer().setFixOnCenter(true);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setPosition(mLocationService.getLocation().getLatitude(), mLocationService.getLocation().getLongitude());
                mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mMapPosition);
                break;
            case NORTH:
                mLocationState = LocationState.TRACK;
                mMap.getEventLayer().enableRotation(false);
                mMap.getEventLayer().setFixOnCenter(true);
                mTrackingDelay = SystemClock.uptimeMillis() + TRACK_ROTATION_DELAY;
                mAutoTiltShouldSet = mMapPosition.getTilt() == 0f;
                break;
            case TRACK:
                mLocationState = LocationState.ENABLED;
                mMap.getEventLayer().enableRotation(true);
                mMap.getEventLayer().setFixOnCenter(false);
                mMap.getMapPosition(mMapPosition);
                mMapPosition.setBearing(0);
                long duration = MAP_BEARING_ANIMATION_DURATION;
                if (mAutoTiltSet) {
                    mMapPosition.setTilt(0f);
                    mAutoTiltSet = false;
                    duration = MAP_POSITION_ANIMATION_DURATION;
                }
                mAutoTiltShouldSet = false;
                mMap.animator().animateTo(duration, mMapPosition);
                break;
        }
        updateLocationDrawable();
    }

    private void onLocationLongClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(2);
        args.putDouble(LocationInformation.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(LocationInformation.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(LocationInformation.ARG_ZOOM, mMapPosition.getZoomLevel());
        Fragment fragment = Fragment.instantiate(this, LocationInformation.class.getName(), args);
        showExtendPanel(PANEL_STATE.LOCATION, "locationInformation", fragment);
    }

    private void onRecordClicked() {
        if (HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_RECORD_TRACK, R.string.advice_record_track, mRecordButton, false))
            return;

        if (mLocationState == LocationState.DISABLED) {
            mTrackingState = TRACKING_STATE.PENDING;
            askForPermission();
            return;
        }
        if (mTrackingState == TRACKING_STATE.TRACKING) {
            Track currentTrack = mCurrentTrackLayer.getTrack();
            if (currentTrack.points.size() == 0)
                disableTracking();
            else
                onTrackDetails(currentTrack, true);
        } else {
            enableTracking();
        }
    }

    private void onRecordLongClicked() {
        Bundle args = new Bundle(1);
        args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, true);
        Fragment fragment = Fragment.instantiate(this, DataSourceList.class.getName(), args);
        showExtendPanel(PANEL_STATE.RECORD, "nativeTrackList", fragment);
    }

    private void onPlacesClicked() {
        boolean hasExtraSources = false;
        if (BuildConfig.FULL_VERSION) {
            for (FileDataSource source : mData) {
                if (!source.isNativeTrack()) {
                    hasExtraSources = true;
                    break;
                }
            }
        }
        //noinspection ConstantConditions
        if (hasExtraSources) {
            Bundle args = new Bundle(1);
            args.putBoolean(DataSourceList.ARG_NATIVE_TRACKS, false);
            Fragment fragment = Fragment.instantiate(this, DataSourceList.class.getName(), args);
            showExtendPanel(PANEL_STATE.PLACES, "dataSourceList", fragment);
        } else {
            Bundle args = new Bundle(3);
            if (mLocationState != LocationState.DISABLED && mLocationService != null) {
                Location location = mLocationService.getLocation();
                args.putDouble(DataList.ARG_LATITUDE, location.getLatitude());
                args.putDouble(DataList.ARG_LONGITUDE, location.getLongitude());
            } else {
                MapPosition position = mMap.getMapPosition();
                args.putDouble(DataList.ARG_LATITUDE, position.getLatitude());
                args.putDouble(DataList.ARG_LONGITUDE, position.getLongitude());
            }
            args.putBoolean(DataList.ARG_NO_EXTRA_SOURCES, BuildConfig.FULL_VERSION);
            DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
            fragment.setDataSource(mWaypointDbDataSource);
            showExtendPanel(PANEL_STATE.PLACES, "dataList", fragment);
        }
    }

    private void onPlacesLongClicked() {
        GeoPoint geoPoint;
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            Point point = mLocationOverlay.getPosition();
            geoPoint = new GeoPoint(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
        } else {
            geoPoint = mMap.getMapPosition().getGeoPoint();
        }
        String name = getString(R.string.place_name, Configuration.getPointsCounter());
        onWaypointCreate(geoPoint, name, false, true);
    }

    private void onMapsClicked() {
        mMap.getMapPosition(mMapPosition);
        Bundle args = new Bundle(5);
        args.putDouble(MapList.ARG_LATITUDE, mMapPosition.getLatitude());
        args.putDouble(MapList.ARG_LONGITUDE, mMapPosition.getLongitude());
        args.putInt(MapList.ARG_ZOOM_LEVEL, mMapPosition.getZoomLevel());
        args.putBoolean(MapList.ARG_HIDE_OBJECTS, mHideMapObjects);
        args.putInt(MapList.ARG_TRANSPARENCY, mBitmapMapTransparency);
        MapList fragment = (MapList) Fragment.instantiate(this, MapList.class.getName(), args);
        fragment.setMaps(mMapIndex.getMaps(), mBitmapLayerMap);
        showExtendPanel(PANEL_STATE.MAPS, "mapsList", fragment);
    }

    private void onMapsLongClicked() {
        PanelMenuFragment fragment = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
        fragment.setMenu(R.menu.menu_map, menu -> {
            Resources resources = getResources();
            MenuItem item = menu.findItem(R.id.actionNightMode);
            String[] nightModes = resources.getStringArray(R.array.night_mode_array);
            ((TextView) item.getActionView()).setText(nightModes[mNightModeState.ordinal()]);
            item = menu.findItem(R.id.actionStyle);
            String[] mapStyles = resources.getStringArray(R.array.mapStyles);
            ((TextView) item.getActionView()).setText(mapStyles[Configuration.getMapStyle()]);
            item = menu.findItem(R.id.actionFontSize);
            String[] fontSizes = resources.getStringArray(R.array.font_size_array);
            ((TextView) item.getActionView()).setText(fontSizes[Configuration.getMapFontSize()]);
            item = menu.findItem(R.id.actionLanguage);
            ((TextView) item.getActionView()).setText(Configuration.getLanguage());
            menu.findItem(R.id.actionAutoTilt).setChecked(mAutoTilt != -1f);
            //if (!BuildConfig.FULL_VERSION) {
            menu.removeItem(R.id.actionNightMode);
            //}
        });
        showExtendPanel(PANEL_STATE.MAPS, "mapMenu", fragment);
    }

    private void onMoreClicked() {
        if (mLocationButton.getVisibility() == View.VISIBLE) {
            PanelMenuFragment fragment = (PanelMenuFragment) Fragment.instantiate(this, PanelMenuFragment.class.getName());
            fragment.setMenu(R.menu.menu_main, menu -> {
                Resources resources = getResources();
                MenuItem item = menu.findItem(R.id.actionActivity);
                String[] activities = resources.getStringArray(R.array.activities);
                int activity = Configuration.getActivity();
                if (activity > 0)
                    ((TextView) item.getActionView()).setText(activities[activity]);
                if (BuildConfig.FULL_VERSION) {
                    menu.findItem(R.id.actionHideSystemUI).setChecked(Configuration.getHideSystemUI());
                } else {
                    menu.removeItem(R.id.actionHideSystemUI);
                }
                if (Configuration.ratingActionPerformed() ||
                        (Configuration.getRunningTime() < 120 &&
                                mWaypointDbDataSource.getWaypointsCount() < 3 &&
                                mData.size() == 0 &&
                                mMapIndex.getMaps().size() == 0)) {
                    menu.removeItem(R.id.actionRate);
                }
                if (mGaugePanel.hasVisibleGauges() || (mLocationState != LocationState.NORTH && mLocationState != LocationState.TRACK))
                    menu.removeItem(R.id.actionAddGauge);
                java.util.Map<String, Pair<Drawable, Intent>> tools = getPluginsTools();
                String[] toolNames = tools.keySet().toArray(new String[0]);
                Arrays.sort(toolNames, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
                for (String toolName : toolNames) {
                    Pair<Drawable, Intent> tool = tools.get(toolName);
                    item = menu.add(PanelMenuItem.HEADER_ID_UNDEFINED, 0, toolName);
                    //item.setIcon(tool.first);
                    item.setIntent(tool.second);
                }
            });
            showExtendPanel(PANEL_STATE.MORE, "panelMenu", fragment);
        } else {
            showActionPanel(true, true);
        }
    }

    private void onMoreLongClicked() {
        boolean show = mLocationButton.getVisibility() == View.INVISIBLE;
        showActionPanel(show, true);
        if (BuildConfig.FULL_VERSION && !show && !Configuration.getHideSystemUI())
            hideSystemUI();
    }

    private void onMapDownloadClicked() {
        mMapDownloadButton.setVisibility(View.GONE);
        startMapSelection(false);
    }

    public void onZoomInClicked(View view) {
        zoomMap(2.0);
    }

    public void onZoomOutClicked(View view) {
        if (mLocationOverlay.isEnabled()) {
            Point out = new Point();
            mMap.viewport().toScreenPoint(mLocationOverlay.getX(), mLocationOverlay.getY(), true, out);
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION >> 2, 0.5, (float) out.x, (float) out.y);
        } else {
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION, 0.5, 0.0f, 0.0f);
        }
    }

    public void onCompassClicked(View view) {
        if (mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.NORTH;
            updateLocationDrawable();
            mMap.getEventLayer().enableRotation(true);
        }
        mMap.getMapPosition(mMapPosition);
        mMapPosition.setBearing(0);
        mMap.animator().animateTo(MAP_BEARING_ANIMATION_DURATION, mMapPosition);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        Object uid = item.getUid();
        if (uid != null)
            onWaypointDetails((Waypoint) uid, false);
        return uid != null;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (mLocationState != LocationState.DISABLED && mLocationState != LocationState.ENABLED)
            return false;
        Object uid = item.getUid();
        if (uid != null) {
            Waypoint waypoint = (Waypoint) uid;
            if (waypoint.locked) {
                Toast.makeText(this, R.string.msgPlaceLocked, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        mActiveMarker = item;
        // For better experience get delta from marker position and finger press
        // and consider it when moving marker
        Point point = new Point();
        mMap.viewport().toScreenPoint(item.getPoint(), point);
        deltaX = (float) (downX - point.x);
        deltaY = (float) (downY - point.y);
        // Shift map to reveal marker tip position
        mMap.getEventLayer().enableMove(false);
        mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(mMap.getWidth() / 2, mMap.getHeight() / 2 + 3 * mFingerTipSize), 1, true);
        return true;
    }

    private void enableLocations() {
        mIsLocationBound = bindService(new Intent(getApplicationContext(), LocationService.class), mLocationConnection, BIND_AUTO_CREATE);
        mLocationState = LocationState.SEARCHING;
        if (mSavedLocationState == LocationState.DISABLED) {
            mSavedLocationState = mPreviousLocationState;
            mPreviousLocationState = LocationState.NORTH;
        }
        if (mTrackingState == TRACKING_STATE.PENDING)
            enableTracking();
        updateLocationDrawable();
    }

    @Override
    public void disableLocations() {
        if (mLocationService != null) {
            mLocationService.unregisterLocationCallback(this);
            mLocationService.setProgressListener(null);
        }
        if (mIsLocationBound) {
            unbindService(mLocationConnection);
            mIsLocationBound = false;
            mLocationOverlay.setEnabled(false);
            mMap.updateMap(true);
        }
        mLocationState = LocationState.DISABLED;
        updateLocationDrawable();
    }

    @Override
    public void setMapLocation(@NonNull GeoPoint point) {
        if (mSavedLocationState == LocationState.NORTH || mSavedLocationState == LocationState.TRACK) {
            mSavedLocationState = LocationState.ENABLED;
        }
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        MapPosition mapPosition = mMap.getMapPosition();
        if (mapPosition.scale > (2 << 7)) {
            mMap.animator().animateTo(point);
        } else {
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, point, 2 << 14, false);
        }
    }

    private MarkerItem mMarker;

    @Override
    public void showMarker(@NonNull GeoPoint point, String name) {
        // There can be only one marker at a time
        removeMarker();
        mMarker = new MarkerItem(name, null, point);
        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, R.drawable.round_marker, mColorAccent));
        mMarker.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER));
        mMarkerLayer.addItem(mMarker);
        mMap.updateMap(true);
    }

    @Override
    public void removeMarker() {
        if (mMarker == null)
            return;
        mMarkerLayer.removeItem(mMarker);
        mMap.updateMap(true);
        mMarker.getMarker().getBitmap().recycle();
        mMarker = null;
    }

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            logger.debug("onServiceConnected: LocationService");
            mLocationService = (ILocationService) binder;
            mLocationService.registerLocationCallback(MainActivity.this);
            mLocationService.setProgressListener(mProgressHandler);
            updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            logger.debug("onServiceDisconnected: LocationService");
            mLocationService = null;
            updateNavigationUI();
        }
    };

    private void enableNavigation() {
        logger.debug("enableNavigation");
        mIsNavigationBound = bindService(new Intent(getApplicationContext(), NavigationService.class), mNavigationConnection, BIND_AUTO_CREATE);
    }

    private void disableNavigation() {
        logger.debug("disableNavigation");
        if (mIsNavigationBound) {
            unbindService(mNavigationConnection);
            mIsNavigationBound = false;
        }
        updateNavigationUI();
    }

    private ServiceConnection mNavigationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            logger.debug("onServiceConnected: NavigationService");
            mNavigationService = (INavigationService) binder;
            updateNavigationUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            logger.debug("onServiceDisconnected: NavigationService");
            mNavigationService = null;
            updateNavigationUI();
        }
    };

    private void startNavigation(MapObject mapObject) {
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_TO_POINT);
        i.putExtra(NavigationService.EXTRA_NAME, mapObject.name);
        i.putExtra(NavigationService.EXTRA_LATITUDE, mapObject.coordinates.getLatitude());
        i.putExtra(NavigationService.EXTRA_LONGITUDE, mapObject.coordinates.getLongitude());
        i.putExtra(NavigationService.EXTRA_PROXIMITY, mapObject.proximity);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission();
    }

    private void startNavigation(long id) {
        if (MapTrek.getMapObject(id) == null)
            return;
        enableNavigation();
        Intent i = new Intent(this, NavigationService.class).setAction(NavigationService.NAVIGATE_TO_OBJECT);
        i.putExtra(NavigationService.EXTRA_ID, id);
        startService(i);
        if (mLocationState == LocationState.DISABLED)
            askForPermission();
    }

    @Override
    public void stopNavigation() {
        startService(new Intent(this, NavigationService.class).setAction(NavigationService.STOP_NAVIGATION));
    }

    private void enableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.ENABLE_TRACK));
        if (mCurrentTrackLayer == null) {
            mCurrentTrackLayer = new CurrentTrackLayer(mMap, getApplicationContext());
            mMap.layers().add(mCurrentTrackLayer, MAP_DATA);
            mMap.updateMap(true);
            Fragment fragment = mFragmentManager.findFragmentByTag("trackInformation");
            if (fragment != null && ((TrackInformation) fragment).hasCurrentTrack()) {
                ((TrackInformation) fragment).setTrack(mCurrentTrackLayer.getTrack(), true);
            }
        }
        mTrackingState = TRACKING_STATE.TRACKING;
        updateLocationDrawable();
    }

    @Override
    public void disableTracking() {
        startService(new Intent(getApplicationContext(), LocationService.class).setAction(BaseLocationService.DISABLE_TRACK));
        mMap.layers().remove(mCurrentTrackLayer);
        if (mCurrentTrackLayer != null) // Can be null if called by intent
            mCurrentTrackLayer.onDetach();
        mCurrentTrackLayer = null;
        mMap.updateMap(true);
        mTrackingState = TRACKING_STATE.DISABLED;
        updateLocationDrawable();
    }

    @Override
    public void navigateTo(@NonNull GeoPoint coordinates, @Nullable String name) {
        startNavigation(new MapObject(name, coordinates));
    }

    @Override
    public boolean isNavigatingTo(@NonNull GeoPoint coordinates) {
        if (mNavigationService == null)
            return false;
        if (!mNavigationService.isNavigating())
            return false;
        MapObject mapObject = mNavigationService.getWaypoint();
        return mapObject.coordinates.equals(coordinates);
    }

    private final Set<WeakReference<LocationStateChangeListener>> mLocationStateChangeListeners = new HashSet<>();

    @Override
    public void addLocationStateChangeListener(LocationStateChangeListener listener) {
        mLocationStateChangeListeners.add(new WeakReference<>(listener));
        listener.onLocationStateChanged(mLocationState);
    }

    @Override
    public void removeLocationStateChangeListener(LocationStateChangeListener listener) {
        for (Iterator<WeakReference<LocationStateChangeListener>> iterator = mLocationStateChangeListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<LocationStateChangeListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    private final Set<WeakReference<LocationChangeListener>> mLocationChangeListeners = new HashSet<>();

    @Override
    public void addLocationChangeListener(LocationChangeListener listener) {
        mLocationChangeListeners.add(new WeakReference<>(listener));
    }

    @Override
    public void removeLocationChangeListener(LocationChangeListener listener) {
        for (Iterator<WeakReference<LocationChangeListener>> iterator = mLocationChangeListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<LocationChangeListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    private float downX, downY, deltaX, deltaY;

    @Override
    public void onInputEvent(Event e, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = motionEvent.getX() - mMap.getWidth() / 2;
            downY = motionEvent.getY() - mMap.getHeight() / 2;
        }
        if (mActiveMarker == null)
            return;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Update corresponding waypoint
            Waypoint waypoint = (Waypoint) mActiveMarker.getUid();
            waypoint.setCoordinates(mActiveMarker.getPoint());
            onWaypointSave(waypoint);
            mActiveMarker = null;
            // Unshift map to its original position
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION / 2, mMap.viewport().fromScreenPoint(mMap.getWidth() / 2, mMap.getHeight() / 2 - mFingerTipSize), 1, true);
            mMap.getEventLayer().enableMove(true);
        } else if (action == MotionEvent.ACTION_MOVE) {
            float eventX = motionEvent.getX() - deltaX;
            float eventY = motionEvent.getY() - deltaY - 3 * mFingerTipSize;
            mActiveMarker.setPoint(mMap.viewport().fromScreenPoint(eventX, eventY));
            mMarkerLayer.updateItems();
            mMap.updateMap(true);
        }
    }

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {
        if (e == Map.POSITION_EVENT) {
            mTrackingOffsetFactor = Math.cos(Math.toRadians(mapPosition.tilt) * 0.85);
            if (mCompassView.getVisibility() == View.GONE && mapPosition.bearing != 0f && mLocationState != LocationState.TRACK) {
                if (Math.abs(mapPosition.bearing) < 1.5f) {
                    mapPosition.setBearing(0f);
                    mMap.setMapPosition(mapPosition);
                }
            }
            adjustCompass(mapPosition.bearing);
            if (mAutoTiltSet) {
                if (mAutoTilt != mapPosition.tilt) {
                    mAutoTiltSet = false;
                    mAutoTiltShouldSet = false;
                }
            } else {
                if (mAutoTiltShouldSet)
                    mAutoTiltSet = mapPosition.tilt == mAutoTilt;
            }
        }
        if (e == Map.MOVE_EVENT) {
            if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
                mPreviousLocationState = mLocationState;
                mLocationState = LocationState.ENABLED;
                updateLocationDrawable();
            }
            if (mFirstMove) {
                mFirstMove = false;
                mPopupAnchor.setX(mMap.getWidth() - 32 * MapTrek.density);
                mPopupAnchor.setY(mStatusBarHeight + 8 * MapTrek.density);
                HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_LOCK_MAP_POSITION, R.string.advice_lock_map_position, mPopupAnchor, R.drawable.ic_volume_down);
            }
        }
        if (mMapDownloadButton.getVisibility() != View.GONE) {
            if (mapPosition.zoomLevel < 8) {
                mMapDownloadButton.setVisibility(View.GONE);
                mMapDownloadButton.setTag(null);
            } else if (e == Map.MOVE_EVENT) {
                final Message m = Message.obtain(mMainHandler, () -> {
                    mMapDownloadButton.setVisibility(View.GONE);
                    mMapDownloadButton.setTag(null);
                });
                m.what = R.id.msgRemoveMapDownloadButton;
                mMainHandler.sendMessageDelayed(m, 1000);
            }
        }
    }

    @Override
    public boolean onGesture(Gesture gesture, MotionEvent event) {
        mMap.getMapPosition(mMapPosition);
        // override default behavior to adjust pivot point
        if (gesture == Gesture.DOUBLE_TAP) {
            zoomMap(2.0);
            return true;
        } else if (gesture == Gesture.TWO_FINGER_TAP) {
            zoomMap(0.5);
            return true;
        } else if (gesture == Gesture.TRIPLE_TAP) {
            float scale = Configuration.getRememberedScale();
            double scaleBy = scale / mMapPosition.getScale();
            zoomMap(scaleBy);
            return true;
        } else if (gesture == Gesture.LONG_PRESS) {
            if (!mMap.getEventLayer().moveEnabled())
                return true;
            mPopupAnchor.setX(event.getX() + mFingerTipSize);
            mPopupAnchor.setY(event.getY() - mFingerTipSize);
            mSelectedPoint = mMap.viewport().fromScreenPoint(event.getX(), event.getY());
            showMarker(mSelectedPoint, null);
            PopupMenu popup = new PopupMenu(this, mPopupAnchor);
            popup.inflate(R.menu.context_menu_map);
            Menu popupMenu = popup.getMenu();
            if ((int) Configuration.getRememberedScale() == (int) mMapPosition.getScale())
                popupMenu.removeItem(R.id.actionRememberScale);
            if (mLocationState != LocationState.TRACK || mAutoTilt == -1f || MathUtils.equals(mAutoTilt, mMapPosition.getTilt()))
                popupMenu.removeItem(R.id.actionRememberTilt);
            popup.setOnMenuItemClickListener(this);
            popup.setOnDismissListener(menu -> removeMarker());
            popup.show();
            return true;
        }
        return false;
    }

    private void adjustCompass(float bearing) {
        if (mCompassView.getRotation() == bearing)
            return;
        mCompassView.setRotation(bearing);
        if (Math.abs(bearing) < 1f && mCompassView.getAlpha() == 1f) {
            mCompassView.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCompassView.setVisibility(View.GONE);
                }
            });
        } else if (mCompassView.getVisibility() == View.GONE) {
            mCompassView.setAlpha(0f);
            mCompassView.setVisibility(View.VISIBLE);
            mCompassView.animate().alpha(1f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(null);
        }
    }

    private void adjustNavigationArrow(float turn) {
        if (mNavigationArrowView.getRotation() == turn)
            return;
        mNavigationArrowView.setRotation(turn);
    }

    private void showNavigationMenu() {
        PopupMenu popup = new PopupMenu(this, mMapButtonHolder);
        popup.inflate(R.menu.context_menu_navigation);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private void updateLocationDrawable() {
        logger.debug("updateLocationDrawable()");
        if (mRecordButton.getTag() != mTrackingState) {
            int recordColor = mTrackingState == TRACKING_STATE.TRACKING ? mColorAccent : mColorPrimaryDark;
            mRecordButton.getDrawable().setTint(recordColor);
            mRecordButton.setTag(mTrackingState);
        }
        if (mLocationButton.getTag() == mLocationState)
            return;
        if (mLocationButton.getTag() == LocationState.SEARCHING) {
            mLocationButton.clearAnimation();
            mSatellitesText.animate().translationY(-200);
        }
        final ViewPropertyAnimator gaugePanelAnimator = mGaugePanel.animate();
        gaugePanelAnimator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mLocationState == LocationState.NORTH)
                    HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_MORE_GAUGES, R.string.advice_more_gauges, mGaugePanel, true);
                //HelperUtils.showAdvice(Configuration.ADVICE_MORE_GAUGES, R.string.advice_more_gauges, mCoordinatorLayout);
                if (mLocationState == LocationState.SEARCHING)
                    mSatellitesText.animate().translationY(8);
                gaugePanelAnimator.setListener(null);
                updateMapViewArea();
            }
        });
        switch (mLocationState) {
            case DISABLED:
                mNavigationNorthDrawable.setTint(mColorPrimaryDark);
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                mCrosshairLayer.setEnabled(true);
                if (mGaugePanel.getWidth() > 0) {
                    gaugePanelAnimator.translationX(-mGaugePanel.getWidth());
                    mGaugePanel.onVisibilityChanged(false);
                }
                break;
            case SEARCHING:
                mLocationSearchingDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mLocationSearchingDrawable);
                Animation rotation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotation.setInterpolator(new LinearInterpolator());
                rotation.setRepeatCount(Animation.INFINITE);
                rotation.setDuration(1000);
                mLocationButton.startAnimation(rotation);
                if (mGaugePanel.getVisibility() == View.INVISIBLE) {
                    mSatellitesText.animate().translationY(8);
                } else {
                    gaugePanelAnimator.translationX(-mGaugePanel.getWidth());
                    mGaugePanel.onVisibilityChanged(false);
                }
                break;
            case ENABLED:
                mMyLocationDrawable.setTint(mColorPrimaryDark);
                mLocationButton.setImageDrawable(mMyLocationDrawable);
                mCrosshairLayer.setEnabled(true);
                gaugePanelAnimator.translationX(-mGaugePanel.getWidth());
                mGaugePanel.onVisibilityChanged(false);
                break;
            case NORTH:
                mNavigationNorthDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mNavigationNorthDrawable);
                mCrosshairLayer.setEnabled(false);
                gaugePanelAnimator.translationX(0);
                mGaugePanel.onVisibilityChanged(true);
                break;
            case TRACK:
                mNavigationTrackDrawable.setTint(mColorAccent);
                mLocationButton.setImageDrawable(mNavigationTrackDrawable);
                mCrosshairLayer.setEnabled(false);
                gaugePanelAnimator.translationX(0);
                mGaugePanel.onVisibilityChanged(true);
        }
        mLocationButton.setTag(mLocationState);
        for (WeakReference<LocationStateChangeListener> weakRef : mLocationStateChangeListeners) {
            LocationStateChangeListener locationStateChangeListener = weakRef.get();
            if (locationStateChangeListener != null) {
                locationStateChangeListener.onLocationStateChanged(mLocationState);
            }
        }
    }

    private void updateGauges() {
        Location location = mLocationService.getLocation();
        mGaugePanel.setValue(Gauge.TYPE_SPEED, location.getSpeed());
        mGaugePanel.setValue(Gauge.TYPE_TRACK, location.getBearing());
        mGaugePanel.setValue(Gauge.TYPE_ALTITUDE, (float) location.getAltitude());
    }

    //TODO Logic of calling this is a total mess! Think out proper event mechanism
    private void updateNavigationUI() {
        logger.debug("updateNavigationUI()");
        boolean enabled = mLocationService != null && mLocationService.getStatus() == BaseLocationService.GPS_OK &&
                mNavigationService != null && mNavigationService.isNavigating();
        boolean changed = mGaugePanel.setNavigationMode(enabled);
        if (enabled) {
            if (mNavigationArrowView.getVisibility() == View.GONE) {
                mNavigationArrowView.setAlpha(0f);
                mNavigationArrowView.setVisibility(View.VISIBLE);
                mNavigationArrowView.animate().alpha(1f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(null);
            }
            GeoPoint destination = mNavigationService.getWaypoint().coordinates;
            if (mNavigationLayer == null) {
                mNavigationLayer = new NavigationLayer(mMap, 0x66ffff00, 8);
                mNavigationLayer.setDestination(destination);
                Point point = mLocationOverlay.getPosition();
                mNavigationLayer.setPosition(MercatorProjection.toLatitude(point.y), MercatorProjection.toLongitude(point.x));
                mMap.layers().add(mNavigationLayer, MAP_POSITIONAL);
            } else {
                GeoPoint current = mNavigationLayer.getDestination();
                if (!destination.equals(current)) {
                    mNavigationLayer.setDestination(destination);
                }
            }
        } else {
            if (mNavigationArrowView.getAlpha() == 1f) {
                mNavigationArrowView.animate().alpha(0f).setDuration(MAP_POSITION_ANIMATION_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mNavigationArrowView.setVisibility(View.GONE);
                    }
                });
            }
            if (mNavigationLayer != null) {
                mMap.layers().remove(mNavigationLayer);
                mNavigationLayer = null;
            }
        }
        if (changed)
            updateMapViewArea();
    }

    @Override
    public void showMarkerInformation(@NonNull GeoPoint point, @Nullable String name) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        Bundle args = new Bundle(3);
        args.putDouble(MarkerInformation.ARG_LATITUDE, point.getLatitude());
        args.putDouble(MarkerInformation.ARG_LONGITUDE, point.getLongitude());
        args.putString(MarkerInformation.ARG_NAME, name);
        Fragment fragment = Fragment.instantiate(this, MarkerInformation.class.getName(), args);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "markerInformation");
        ft.addToBackStack("markerInformation");
        ft.commit();
        updateMapViewArea();
    }

    private void onWaypointProperties(Waypoint waypoint) {
        mEditedWaypoint = waypoint;
        Bundle args = new Bundle(2);
        args.putString(WaypointProperties.ARG_NAME, mEditedWaypoint.name);
        args.putInt(WaypointProperties.ARG_COLOR, mEditedWaypoint.style.color);
        Fragment fragment = Fragment.instantiate(this, WaypointProperties.class.getName(), args);
        fragment.setEnterTransition(new Fade());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "waypointProperties");
        ft.addToBackStack("waypointProperties");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onWaypointCreate(GeoPoint point, String name, boolean locked, boolean customize) {
        final Waypoint waypoint = new Waypoint(name, point.getLatitude(), point.getLongitude());
        waypoint.date = new Date();
        waypoint.locked = locked;
        mWaypointDbDataSource.saveWaypoint(waypoint);
        MarkerItem marker = new MarkerItem(waypoint, name, null, point);
        mMarkerLayer.addItem(marker);
        mMap.updateMap(true);
        if (!customize)
            return;
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msgPlaceSaved, Snackbar.LENGTH_LONG)
                .setAction(R.string.actionCustomize, view -> onWaypointProperties(waypoint));
        snackbar.show();
    }

    @Override
    public void onWaypointView(Waypoint waypoint) {
        setMapLocation(waypoint.coordinates);
    }

    @Override
    public void onWaypointFocus(Waypoint waypoint) {
        if (waypoint != null)
            mMarkerLayer.setFocus(mMarkerLayer.getByUid(waypoint), waypoint.style.color);
        else
            mMarkerLayer.setFocus(null);
    }

    @Override
    public void onWaypointDetails(Waypoint waypoint, boolean fromList) {
        Bundle args = new Bundle(3);
        args.putBoolean(WaypointInformation.ARG_DETAILS, fromList);
        if (fromList || mLocationState != LocationState.DISABLED) {
            if (mLocationState != LocationState.DISABLED && mLocationService != null) {
                Location location = mLocationService.getLocation();
                args.putDouble(WaypointInformation.ARG_LATITUDE, location.getLatitude());
                args.putDouble(WaypointInformation.ARG_LONGITUDE, location.getLongitude());
            } else {
                MapPosition position = mMap.getMapPosition();
                args.putDouble(WaypointInformation.ARG_LATITUDE, position.getLatitude());
                args.putDouble(WaypointInformation.ARG_LONGITUDE, position.getLongitude());
            }
        }

        Fragment fragment = mFragmentManager.findFragmentByTag("waypointInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, WaypointInformation.class.getName(), args);
            Slide slide = new Slide(Gravity.BOTTOM);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "waypointInformation");
            ft.addToBackStack("waypointInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((WaypointInformation) fragment).setWaypoint(waypoint);
        mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(mExtendPanel.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    @Override
    public void onWaypointNavigate(Waypoint waypoint) {
        startNavigation(waypoint);
    }

    @Override
    public void onWaypointShare(Waypoint waypoint) {
        shareLocation(waypoint.coordinates, waypoint.name);
    }

    @Override
    public void onWaypointSave(final Waypoint waypoint) {
        if (waypoint.source instanceof WaypointDbDataSource) {
            mWaypointDbDataSource.saveWaypoint(waypoint);
        } else {
            Manager.save((FileDataSource) waypoint.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(() -> waypoint.source.notifyListeners());
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }, mProgressHandler);
        }
        // Markers are immutable so simply recreate it
        removeWaypointMarker(waypoint);
        addWaypointMarker(waypoint);
        mMap.updateMap(true);
    }

    @Override
    public void onWaypointDelete(final Waypoint waypoint) {
        // Remove marker to indicate action to user
        removeWaypointMarker(waypoint);
        mMap.updateMap(true);

        // Show undo snackbar
        //noinspection deprecation
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msgPlaceDeleted, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoint
                        if (waypoint.source instanceof WaypointDbDataSource) {
                            mWaypointDbDataSource.deleteWaypoint(waypoint);
                        } else {
                            ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                            Manager.save((FileDataSource) waypoint.source, new Manager.OnSaveListener() {
                                @Override
                                public void onSaved(FileDataSource source) {
                                    mMainHandler.post(() -> waypoint.source.notifyListeners());
                                }

                                @Override
                                public void onError(FileDataSource source, Exception e) {
                                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                                }
                            }, mProgressHandler);
                        }
                        mTotalDataItems--;
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the marker
                    addWaypointMarker(waypoint);
                    mMap.updateMap(true);
                });
        snackbar.show();
    }

    @Override
    public void onWaypointsDelete(final Set<Waypoint> waypoints) {
        // Remove markers to indicate action to user
        for (Waypoint waypoint : waypoints) {
            removeWaypointMarker(waypoint);
        }
        mMap.updateMap(true);

        // Show undo snackbar
        int count = waypoints.size();
        String msg = getResources().getQuantityString(R.plurals.placesDeleted, count, count);
        //noinspection deprecation
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoints
                        HashSet<FileDataSource> sources = new HashSet<>();
                        for (Waypoint waypoint : waypoints) {
                            if (waypoint.source instanceof WaypointDbDataSource) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            } else {
                                ((FileDataSource) waypoint.source).waypoints.remove(waypoint);
                                sources.add((FileDataSource) waypoint.source);
                            }
                            mTotalDataItems--;
                        }
                        for (FileDataSource source : sources) {
                            Manager.save(source, new Manager.OnSaveListener() {
                                @Override
                                public void onSaved(final FileDataSource source) {
                                    mMainHandler.post(source::notifyListeners);
                                }

                                @Override
                                public void onError(FileDataSource source, Exception e) {
                                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                                }
                            }, mProgressHandler);
                        }
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the marker
                    for (Waypoint waypoint : waypoints) {
                        addWaypointMarker(waypoint);
                    }
                    mMap.updateMap(true);
                });
        snackbar.show();
    }

    @Override
    public void onWaypointPropertiesChanged(String name, int color) {
        boolean colorChanged = mEditedWaypoint.style.color != color;
        mEditedWaypoint.name = name;
        mEditedWaypoint.style.color = color;
        MarkerItem item = mMarkerLayer.getByUid(mEditedWaypoint);
        item.title = name;
        if (colorChanged) {
            AndroidBitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, color));
            item.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
        }
        mMarkerLayer.updateItems();
        mMap.updateMap(true);
        // This event is relevant only to internal data source
        mWaypointDbDataSource.saveWaypoint(mEditedWaypoint);
        mEditedWaypoint = null;
    }

    private void onTrackProperties(String path) {
        logger.debug("onTrackProperties({})", path);
        //TODO Think of better way to find appropriate track
        for (FileDataSource source : mData) {
            if (source.path.equals(path)) {
                mEditedTrack = source.tracks.get(0);
                break;
            }
        }
        if (mEditedTrack == null)
            return;

        Bundle args = new Bundle(2);
        args.putString(TrackProperties.ARG_NAME, mEditedTrack.name);
        args.putInt(TrackProperties.ARG_COLOR, mEditedTrack.style.color);
        Fragment fragment = Fragment.instantiate(this, TrackProperties.class.getName(), args);
        fragment.setEnterTransition(new Fade());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "trackProperties");
        ft.addToBackStack("trackProperties");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onTrackPropertiesChanged(String name, int color) {
        mEditedTrack.name = name;
        mEditedTrack.style.color = color;
        onTrackSave(mEditedTrack);
        mEditedTrack = null;
    }

    @Override
    public void onTrackView(Track track) {
        if (mLocationState == LocationState.NORTH || mLocationState == LocationState.TRACK) {
            mLocationState = LocationState.ENABLED;
            updateLocationDrawable();
        }
        BoundingBox box = track.getBoundingBox();
        box.extendBy(0.05);
        mMap.animator().animateTo(box);
    }

    @Override
    public void onTrackDetails(Track track) {
        onTrackDetails(track, false);
    }

    private void onTrackDetails(Track track, boolean current) {
        Fragment fragment = mFragmentManager.findFragmentByTag("trackInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, TrackInformation.class.getName());
            Slide slide = new Slide(mSlideGravity);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "trackInformation");
            ft.addToBackStack("trackInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((TrackInformation) fragment).setTrack(track, current);
        mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(mExtendPanel.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    @Override
    public void onTrackShare(final Track track) {
        final AtomicInteger selected = new AtomicInteger(0);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(R.array.track_format_array, selected.get(), (dialog, which) -> selected.set(which));
        builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
            DataExport.Builder builder12 = new DataExport.Builder();
            @DataExport.ExportFormat int format = selected.get();
            DataExport dataExport = builder12.setTrack(track).setFormat(format).create();
            dataExport.show(mFragmentManager, "trackExport");
        });
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            String msgNative = getString(R.string.msgNativeFormatExplanation);
            String msgOther = getString(R.string.msgOtherFormatsExplanation);
            builder1.setMessage(msgNative + " " + msgOther);
            builder1.setPositiveButton(R.string.ok, null);
            AlertDialog dialog1 = builder1.create();
            dialog1.show();
        });
    }

    @Override
    public void onTrackSave(final Track track) {
        FileDataSource fileSource = (FileDataSource) track.source;
        Manager manager = Manager.getDataManager(fileSource.path);
        if (manager instanceof TrackManager) {
            // Use optimized save for native track
            try {
                ((TrackManager) manager).saveProperties(fileSource);
                // Rename file if name changed
                File thisFile = new File(fileSource.path);
                File thatFile = new File(thisFile.getParent(), FileUtils.sanitizeFilename(track.name) + TrackManager.EXTENSION);
                if (!thisFile.equals(thatFile)) {
                    Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
                    if (loader != null) {
                        // Let loader do the task if it is available
                        ((DataLoader) loader).renameSource(fileSource, thatFile);
                        // otherwise do it manually (this normally should not happen)
                    } else if (thisFile.renameTo(thatFile)) {
                        fileSource.path = thatFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                HelperUtils.showSaveError(this, mCoordinatorLayout, e);
                e.printStackTrace();
            }
        } else {
            // Save hole data source
            Manager.save((FileDataSource) track.source, new Manager.OnSaveListener() {
                @Override
                public void onSaved(FileDataSource source) {
                    mMainHandler.post(() -> track.source.notifyListeners());
                }

                @Override
                public void onError(FileDataSource source, Exception e) {
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }, mProgressHandler);
        }
        // Update track layer
        for (Layer layer : mMap.layers()) {
            if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(track)) {
                ((TrackLayer) layer).setColor(track.style.color);
            }
        }
        mMap.updateMap(true);
    }

    @Override
    public void onTrackDelete(final Track track) {
        // Remove track layer to indicate action to user
        for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if (layer instanceof TrackLayer && ((TrackLayer) layer).getTrack().equals(track)) {
                i.remove();
                layer.onDetach();
                break;
            }
        }
        mMap.updateMap(true);

        // Show undo snackbar
        //noinspection deprecation
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.msgTrackDeleted, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove track
                        // Native tracks can not be deleted through this procedure
                        ((FileDataSource) track.source).tracks.remove(track);
                        Manager.save((FileDataSource) track.source, new Manager.OnSaveListener() {
                            @Override
                            public void onSaved(FileDataSource source) {
                                mMainHandler.post(() -> track.source.notifyListeners());
                            }

                            @Override
                            public void onError(FileDataSource source, Exception e) {
                                HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                            }
                        }, mProgressHandler);
                        mTotalDataItems--;
                    }
                })
                .setAction(R.string.actionUndo, view -> {
                    // If undo pressed, restore the track on map
                    TrackLayer trackLayer = new TrackLayer(mMap, track);
                    mMap.layers().add(trackLayer, MAP_DATA);
                    mMap.updateMap(true);
                });
        snackbar.show();
    }

    @Override
    public void onTracksDelete(final Set<Track> tracks) {
        // Remove markers to indicate action to user
        /*
        for (Waypoint waypoint : waypoints) {
            MarkerItem marker = mMarkerLayer.getByUid(waypoint);
            mMarkerLayer.removeItem(marker);
        }
        mMap.updateMap(true);
        */

        // Show undo snackbar
        /*
        int count = waypoints.size();
        String msg = getResources().getQuantityString(R.plurals.waypointsDeleted, count, count);
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_ACTION)
                            return;
                        // If dismissed, actually remove waypoint
                        if (mWaypointDbDataSource.isOpen()) {
                            for (Waypoint waypoint : waypoints) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            }
                        } else {
                            // We need this when screen is rotated but snackbar is still shown
                            mWaypointDbDataSource.open();
                            for (Waypoint waypoint : waypoints) {
                                mWaypointDbDataSource.deleteWaypoint(waypoint);
                            }
                            mWaypointDbDataSource.close();
                        }
                    }
                })
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // If undo pressed, restore the marker
                        for (Waypoint waypoint : waypoints) {
                            GeoPoint point = new GeoPoint(waypoint.latitude, waypoint.longitude);
                            MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, point);
                            mMarkerLayer.addItem(marker);
                        }
                        mMap.updateMap(true);
                    }
                });
        snackbar.show();
        */
    }

    @Override
    public void onFeatureDetails(long id) {
        Bundle args = new Bundle(3);
        if (mLocationState != LocationState.DISABLED && mLocationService != null) {
            Location location = mLocationService.getLocation();
            args.putDouble(AmenityInformation.ARG_LATITUDE, location.getLatitude());
            args.putDouble(AmenityInformation.ARG_LONGITUDE, location.getLongitude());
        } else {
            MapPosition position = mMap.getMapPosition();
            args.putDouble(AmenityInformation.ARG_LATITUDE, position.getLatitude());
            args.putDouble(AmenityInformation.ARG_LONGITUDE, position.getLongitude());
        }

        Fragment fragment = mFragmentManager.findFragmentByTag("amenityInformation");
        if (fragment == null) {
            fragment = Fragment.instantiate(this, AmenityInformation.class.getName(), args);
            Slide slide = new Slide(Gravity.BOTTOM);
            // Required to sync with FloatingActionButton
            slide.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            fragment.setEnterTransition(slide);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.replace(R.id.contentPanel, fragment, "amenityInformation");
            ft.addToBackStack("amenityInformation");
            ft.commit();
            updateMapViewArea();
        }
        ((AmenityInformation) fragment).setPreferredLanguage(Configuration.getLanguage());
        ((AmenityInformation) fragment).setAmenity(id);
        mExtendPanel.setForeground(getDrawable(R.drawable.dim));
        mExtendPanel.getForeground().setAlpha(0);
        ObjectAnimator anim = ObjectAnimator.ofInt(mExtendPanel.getForeground(), "alpha", 0, 255);
        anim.setDuration(500);
        anim.start();
    }

    @Override
    public void shareLocation(@NonNull GeoPoint coordinates, @Nullable String name) {
        LocationShareDialog dialogFragment = new LocationShareDialog();
        Bundle args = new Bundle();
        args.putDouble(LocationShareDialog.ARG_LATITUDE, coordinates.getLatitude());
        args.putDouble(LocationShareDialog.ARG_LONGITUDE, coordinates.getLongitude());
        args.putInt(LocationShareDialog.ARG_ZOOM, mMap.getMapPosition().getZoomLevel());
        if (name != null)
            args.putString(LocationShareDialog.ARG_NAME, name);
        dialogFragment.setArguments(args);
        dialogFragment.show(mFragmentManager, "locationShare");
    }

    private void showHideMapObjects(boolean hasBitmapMap) {
        Layers layers = mMap.layers();
        if (hasBitmapMap && mHideMapObjects && layers.contains(mLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.remove(mBuildingsLayer);
            layers.remove(mLabelsLayer);
        }
        if ((!hasBitmapMap || !mHideMapObjects) && !layers.contains(mLabelsLayer)) {
            if (mBuildingsLayerEnabled)
                layers.add(mBuildingsLayer, MAP_3D);
            layers.add(mLabelsLayer, MAP_LABELS);
        }
    }

    private void startMapSelection(boolean zoom) {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }
        if (zoom) {
            MapPosition mapPosition = mMap.getMapPosition();
            mapPosition.setScale(MapCoverageLayer.TEXT_MIN_SCALE + 5f);
            mapPosition.setBearing(0f);
            mapPosition.setTilt(0f);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        }
        MapSelection fragment = (MapSelection) Fragment.instantiate(this, MapSelection.class.getName());
        fragment.setMapIndex(mNativeMapIndex);
        fragment.setEnterTransition(new Slide());
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.contentPanel, fragment, "mapSelection");
        ft.addToBackStack("mapSelection");
        ft.commit();
        updateMapViewArea();
    }

    @Override
    public void onMapSelected(MapFile mapFile) {
        if (mBitmapLayerMap != null) {
            mMap.layers().remove(mBitmapLayerMap.tileLayer);
            mBitmapLayerMap.tileSource.close();
            if (mapFile == mBitmapLayerMap) {
                showHideMapObjects(false);
                mMap.updateMap(true);
                mBitmapLayerMap = null;
                return;
            }
        }
        showBitmapMap(mapFile, true);
    }

    @Override
    public void onMapShare(MapFile mapFile) {
        String filename = mapFile.tileSource.getOption("path");
        File exportFile = new File(filename);
        Uri contentUri = ExportProvider.getUriForFile(this, exportFile);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType("application/octet-stream");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_map_intent_title)));
    }

    @Override
    public void onMapDelete(MapFile mapFile) {
        if (mBitmapLayerMap != null && mapFile == mBitmapLayerMap) {
            mMap.layers().remove(mBitmapLayerMap.tileLayer);
            mBitmapLayerMap.tileSource.close();
            showHideMapObjects(false);
            mMap.updateMap(true);
            mBitmapLayerMap = null;
        }
        mMapIndex.removeMap(mapFile);
        String filename = mapFile.tileSource.getOption("path");
        File file = new File(filename);
        if (!file.delete())
            HelperUtils.showError(getString(R.string.msgMapDeleteFailed), mCoordinatorLayout);
    }

    @Override
    public void onHideMapObjects(boolean hide) {
        mHideMapObjects = hide;
        showHideMapObjects(mBitmapLayerMap != null);
        mMap.updateMap(true);
        Configuration.setHideMapObjects(hide);
    }

    @Override
    public void onTransparencyChanged(int transparency) {
        mBitmapMapTransparency = transparency;
        if (mBitmapLayerMap != null && mBitmapLayerMap.tileLayer instanceof BitmapTileLayer)
            ((BitmapTileLayer) mBitmapLayerMap.tileLayer).setBitmapAlpha(1 - mBitmapMapTransparency * 0.01f);
        Configuration.setBitmapMapTransparency(transparency);
    }

    @Override
    public void onBeginMapManagement() {
        mMapCoverageLayer = new MapCoverageLayer(getApplicationContext(), mMap, mNativeMapIndex, MapTrek.density);
        mMap.layers().add(mMapCoverageLayer, MAP_OVERLAYS);
        MapPosition mapPosition = mMap.getMapPosition();
        if (mapPosition.zoomLevel > 8) {
            mapPosition.setZoomLevel(8);
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, mapPosition);
        } else {
            mMap.updateMap(true);
        }
        int[] xy = (int[]) mMapDownloadButton.getTag();
        if (xy != null)
            mNativeMapIndex.selectNativeMap(xy[0], xy[1], Index.ACTION.DOWNLOAD);
    }

    @Override
    public void onFinishMapManagement() {
        mMap.layers().remove(mMapCoverageLayer);
        mMapCoverageLayer.onDetach();
        mMap.updateMap(true);
        mNativeMapIndex.clearSelections();
        mMapCoverageLayer = null;
    }

    @Override
    public void onManageNativeMaps(boolean hillshadesEnabled) {
        mNativeMapIndex.manageNativeMaps(hillshadesEnabled);
    }

    private void showHillShade() {
        SQLiteTileSource hillShadeTileSource = MapTrek.getApplication().getHillShadeTileSource();
        if (hillShadeTileSource != null) {
            int transparency = Configuration.getHillshadesTransparency();
            mHillshadeLayer = new BitmapTileLayer(mMap, hillShadeTileSource, 1 - transparency * 0.01f);
            mMap.layers().add(mHillshadeLayer, MAP_MAP_OVERLAYS);
            mMap.updateMap(true);
        }
    }

    private void hideHillShade() {
        mMap.layers().remove(mHillshadeLayer);
        mHillshadeLayer.onDetach();
        mMap.updateMap(true);
        mHillshadeLayer = null;
    }

    private void showBitmapMap(MapFile mapFile, boolean reposition) {
        logger.debug("showBitmapMap({})", mapFile.name);
        showHideMapObjects(true);
        mapFile.tileSource.open();
        if ("vtm".equals(mapFile.tileSource.getOption("format"))) {
            OsmTileLayer layer = new OsmTileLayer(mMap);
            layer.setTileSource(mapFile.tileSource);
            layer.setRenderTheme(ThemeLoader.load(Themes.MAPTREK));
            mapFile.tileLayer = layer;
        } else {
            mapFile.tileLayer = new BitmapTileLayer(mMap, mapFile.tileSource, 1 - mBitmapMapTransparency * 0.01f);
        }
        mMap.layers().add(mapFile.tileLayer, MAP_MAPS);
        mBitmapLayerMap = mapFile;
        if (!reposition)
            return;

        MapPosition position = mMap.getMapPosition();
        boolean positionChanged = false;
        if (!mapFile.boundingBox.contains(position.getGeoPoint())) {
            position.setPosition(mapFile.boundingBox.getCenterPoint());
            positionChanged = true;
        }
        if (position.getZoomLevel() > mapFile.tileSource.getZoomLevelMax()) {
            position.setScale((1 << mapFile.tileSource.getZoomLevelMax()) - 5);
            positionChanged = true;
        }
        int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
        if (mapFile.tileSource instanceof SQLiteTileSource) {
            minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
        }
        double minScale = (1 << minZoomLevel) * 0.7 + (1 << (minZoomLevel + 1)) * 0.3 + 5;
        if (position.getScale() < minScale) {
            position.setScale(minScale);
            positionChanged = true;
        }
        if (positionChanged)
            mMap.animator().animateTo(MAP_POSITION_ANIMATION_DURATION, position);
        else
            //TODO Bitmap layer should respond to update map (see TileLayer)
            mMap.clearMap();
    }

    private void showActionPanel(boolean show, boolean animate) {
        Configuration.setActionPanelState(show);
        final int duration = 30;
        final View mAPB = findViewById(R.id.actionPanelBackground);

        // If this is interactive action hide all open panels
        if (animate && mFragmentManager.getBackStackEntryCount() > 0) {
            popAll();
        }

        if (animate)
            mMoreButton.animate().rotationBy(180).setDuration(duration * 5).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMoreButton.setRotation(0f);
                }
            });
        if (show) {
            mAPB.setVisibility(View.VISIBLE);
            if (animate)
                mAPB.animate().setDuration(duration * 5).alpha(1f);
            else
                mAPB.setAlpha(1f);
            mMapsButton.setVisibility(View.VISIBLE);
            if (animate) {
                mMapsButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPlacesButton.setVisibility(View.VISIBLE);
                        mPlacesButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mRecordButton.setVisibility(View.VISIBLE);
                                mRecordButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mLocationButton.setVisibility(View.VISIBLE);
                                        mLocationButton.animate().alpha(1f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                mExtendPanel.postInvalidate();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                mMapsButton.setAlpha(1f);
                mPlacesButton.setVisibility(View.VISIBLE);
                mPlacesButton.setAlpha(1f);
                mRecordButton.setVisibility(View.VISIBLE);
                mRecordButton.setAlpha(1f);
                mLocationButton.setVisibility(View.VISIBLE);
                mLocationButton.setAlpha(1f);
            }
        } else {
            if (animate) {
                mAPB.animate().alpha(0f).setDuration(duration * 5);
                mLocationButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLocationButton.setVisibility(View.INVISIBLE);
                        mRecordButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mRecordButton.setVisibility(View.INVISIBLE);
                                mPlacesButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mPlacesButton.setVisibility(View.INVISIBLE);
                                        mMapsButton.animate().alpha(0f).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                mMapsButton.setVisibility(View.INVISIBLE);
                                                mAPB.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                mAPB.setAlpha(0f);
                mLocationButton.setAlpha(0f);
                mLocationButton.setVisibility(View.INVISIBLE);
                mRecordButton.setAlpha(0f);
                mRecordButton.setVisibility(View.INVISIBLE);
                mPlacesButton.setAlpha(0f);
                mPlacesButton.setVisibility(View.INVISIBLE);
                mMapsButton.setAlpha(0f);
                mMapsButton.setVisibility(View.INVISIBLE);
                mAPB.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showExtendPanel(PANEL_STATE panel, String name, Fragment fragment) {
        if (mPanelState != PANEL_STATE.NONE && mFragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
            //TODO Make it properly work without "immediate" - that is why exit transitions do not work
            mFragmentManager.popBackStackImmediate(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (name.equals(bse.getName())) {
                setPanelState(PANEL_STATE.NONE);
                return;
            }
        }
        mExtendPanel.setForeground(null);

        FragmentTransaction ft = mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new TransitionSet().addTransition(new Slide(mSlideGravity)).addTransition(new Visibility() {
            @Override
            public Animator onAppear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelBackground), getColor(R.color.panelSolidBackground));
            }
        }));
        fragment.setReturnTransition(new TransitionSet().addTransition(new Slide(mSlideGravity)).addTransition(new Visibility() {
            @Override
            public Animator onDisappear(ViewGroup sceneRoot, final View v, TransitionValues startValues, TransitionValues endValues) {
                return ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), getColor(R.color.panelSolidBackground), getColor(R.color.panelBackground));
            }
        }));
        ft.replace(R.id.extendPanel, fragment, name);
        ft.addToBackStack(name);
        ft.commit();

        setPanelState(panel);

        if ("dataList".equals(name) || "dataSourceList".equals(name))
            HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_ADDING_PLACE, R.string.advice_adding_place, mPlacesButton, false);
    }

    private void setPanelState(PANEL_STATE state) {
        if (mPanelState == state)
            return;

        View mLBB = findViewById(R.id.locationButtonBackground);
        View mRBB = findViewById(R.id.recordButtonBackground);
        View mPBB = findViewById(R.id.placesButtonBackground);
        View mOBB = findViewById(R.id.mapsButtonBackground);
        View mMBB = findViewById(R.id.moreButtonBackground);

        // View that gains active state
        final View thisView;
        final ArrayList<View> otherViews = new ArrayList<>();

        if (mPanelState == PANEL_STATE.NONE || state == PANEL_STATE.NONE) {
            otherViews.add(mLBB);
            otherViews.add(mRBB);
            otherViews.add(mPBB);
            otherViews.add(mOBB);
            otherViews.add(mMBB);
        } else {
            // If switching from one view to another animate only that view
            switch (mPanelState) {
                case LOCATION:
                    otherViews.add(mLBB);
                    break;
                case RECORD:
                    otherViews.add(mRBB);
                    break;
                case PLACES:
                    otherViews.add(mPBB);
                    break;
                case MAPS:
                    otherViews.add(mOBB);
                    break;
                case MORE:
                    otherViews.add(mMBB);
                    break;
            }
        }

        PANEL_STATE thisState = state == PANEL_STATE.NONE ? mPanelState : state;
        switch (thisState) {
            case LOCATION:
                thisView = mLBB;
                break;
            case RECORD:
                thisView = mRBB;
                break;
            case PLACES:
                thisView = mPBB;
                break;
            case MAPS:
                thisView = mOBB;
                break;
            case MORE:
                thisView = mMBB;
                break;
            default:
                return;
        }
        otherViews.remove(thisView);

        int thisFrom, thisTo, otherFrom, otherTo;
        if (state == PANEL_STATE.NONE) {
            thisFrom = mPanelSolidBackground;
            thisTo = mPanelBackground;
            otherFrom = mPanelExtendedBackground;
            otherTo = mPanelBackground;
        } else {
            if (mPanelState == PANEL_STATE.NONE)
                thisFrom = mPanelBackground;
            else
                thisFrom = mPanelExtendedBackground;
            thisTo = mPanelSolidBackground;
            if (mPanelState == PANEL_STATE.NONE)
                otherFrom = mPanelBackground;
            else
                otherFrom = mPanelSolidBackground;
            otherTo = mPanelExtendedBackground;
        }
        ValueAnimator otherColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), otherFrom, otherTo);
        ValueAnimator thisColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), thisFrom, thisTo);
        thisColorAnimation.addUpdateListener(animator -> {
            int color = (Integer) animator.getAnimatedValue();
            thisView.setBackgroundColor(color);
        });
        otherColorAnimation.addUpdateListener(animator -> {
            int color = (Integer) animator.getAnimatedValue();
            for (View otherView : otherViews)
                otherView.setBackgroundColor(color);
        });
        AnimatorSet s = new AnimatorSet();
        s.play(thisColorAnimation).with(otherColorAnimation);
        s.start();

        mPanelState = state;
        updateMapViewArea();
    }

    private final Set<WeakReference<OnBackPressedListener>> mBackListeners = new HashSet<>();

    @Override
    public FloatingActionButton enableActionButton() {
        if (mListActionButton.getVisibility() == View.VISIBLE)
            mListActionButton.setVisibility(View.INVISIBLE);
        TransitionManager.beginDelayedTransition(mCoordinatorLayout, new Fade());
        mActionButton.setVisibility(View.VISIBLE);
        return mActionButton;
    }

    @Override
    public void disableActionButton() {
        mActionButton.setVisibility(View.GONE);
        if (mListActionButton.getVisibility() == View.INVISIBLE)
            mListActionButton.setVisibility(View.VISIBLE);
    }

    @Override
    public FloatingActionButton enableListActionButton() {
        TransitionManager.beginDelayedTransition(mCoordinatorLayout, new Fade());
        mListActionButton.setVisibility(View.VISIBLE);
        return mListActionButton;
    }

    @Override
    public void disableListActionButton() {
        mListActionButton.setVisibility(View.GONE);
    }

    @Override
    public void addBackClickListener(OnBackPressedListener listener) {
        mBackListeners.add(new WeakReference<>(listener));
    }

    @Override
    public void removeBackClickListener(OnBackPressedListener listener) {
        for (Iterator<WeakReference<OnBackPressedListener>> iterator = mBackListeners.iterator();
             iterator.hasNext(); ) {
            WeakReference<OnBackPressedListener> weakRef = iterator.next();
            if (weakRef.get() == listener) {
                iterator.remove();
            }
        }
    }

    @Override
    public void popCurrent() {
        logger.debug("popCurrent()");
        int count = mFragmentManager.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
            String fragmentName = bse.getName();
            if ("baseMapDownload".equals(fragmentName)) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_ENABLE_LOCATIONS, R.string.advice_enable_locations, mLocationButton, false);
                }
            } else if ("trackProperties".equals(fragmentName)) {
                HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_RECORDED_TRACKS, R.string.advice_recorded_tracks, mRecordButton, false);
            }
        }
        mFragmentManager.popBackStack();
    }

    @Override
    public void popAll() {
        logger.debug("popAll()");
        FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(0);
        mFragmentManager.popBackStack(bse.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public CoordinatorLayout getCoordinatorLayout() {
        return mCoordinatorLayout;
    }

    private boolean backKeyIntercepted() {
        boolean intercepted = false;
        for (WeakReference<OnBackPressedListener> weakRef : mBackListeners) {
            OnBackPressedListener onBackClickListener = weakRef.get();
            if (onBackClickListener != null) {
                boolean isFragIntercept = onBackClickListener.onBackClick();
                if (!intercepted)
                    intercepted = isFragIntercept;
            }
        }
        return intercepted;
    }

    final Handler mBackHandler = new Handler();

    @Override
    public void onBackPressed() {
        logger.debug("onBackPressed()");
        if (backKeyIntercepted())
            return;

        int count = mFragmentManager.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
            if (BuildConfig.FULL_VERSION && "settings".equals(bse.getName()))
                HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_MAP_SETTINGS, R.string.advice_map_settings, mMapsButton, false);
            if ("trackProperties".equals(bse.getName()))
                HelperUtils.showTargetedAdvice(this, Configuration.ADVICE_RECORDED_TRACKS, R.string.advice_recorded_tracks, mRecordButton, false);
            super.onBackPressed();
            if (count == 1 && mPanelState != PANEL_STATE.NONE)
                setPanelState(PANEL_STATE.NONE);
            return;
        }

        if (count == 0 || secondBack) {
            //mBackToast.cancel();
            finish();
        } else {
            secondBack = true;
            mBackToast.show();
            mBackHandler.postDelayed(() -> secondBack = false, 2000);
        }
    }

    @Override
    public void onBackStackChanged() {
        logger.debug("onBackStackChanged()");
        int count = mFragmentManager.getBackStackEntryCount();
        if (count == 0) {
            if (mPanelState != PANEL_STATE.NONE)
                setPanelState(PANEL_STATE.NONE);
            return;
        }
        FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
        Fragment f = mFragmentManager.findFragmentByTag(bse.getName());
        if (f == null)
            return;
        View v = f.getView();
        if (v == null)
            return;
        final ViewGroup p = (ViewGroup) v.getParent();
        if (p.getForeground() != null) {
            p.setForeground(getDrawable(R.drawable.dim));
            p.getForeground().setAlpha(0);
            ObjectAnimator anim = ObjectAnimator.ofInt(p.getForeground(), "alpha", 255, 0);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    p.setForeground(null);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    p.setForeground(null);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            anim.setDuration(500);
            anim.start();
        }
    }

    // Called by tile manager, so it's on separate thread - do not block and do not update UI
    @Override
    public void onDataMissing(final int x, final int y, byte zoom) {
        // Do not check "intermediate" maps - TODO: Should we consider movement when locked to location?
        if (mMap.animator().isActive())
            return;

        // Do not show button if we are already choosing maps
        if (mMapCoverageLayer != null)
            return;

        // Do not show button if this map is already downloading
        if (mNativeMapIndex.isDownloading(x, y))
            return;

        // Do not show button if there is no map for that area
        if (mNativeMapIndex.hasDownloadSizes() && mNativeMapIndex.getNativeMap(x, y).downloadSize == 0L)
            return;

        // Do not show button if custom map is shown
        mMap.getMapPosition(mMapPosition);
        if (mBitmapLayerMap != null && mBitmapLayerMap.contains(mMapPosition.getX(), mMapPosition.getY()))
            return;

        runOnUiThread(() -> {
            if (mMapDownloadButton.getVisibility() == View.GONE) {
                mMapDownloadButton.setText(R.string.mapDownloadText);
                mMapDownloadButton.setVisibility(View.VISIBLE);
            }
            mMapDownloadButton.setTag(new int[]{x, y});
            mMainHandler.removeMessages(R.id.msgRemoveMapDownloadButton);
        });
    }

    @Override
    public void updateMapViewArea() {
        logger.debug("updateMapViewArea()");
        final ViewTreeObserver vto = mMapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                logger.debug("onGlobalLayout()");

                FrameLayout.MarginLayoutParams p = (FrameLayout.MarginLayoutParams) mCoordinatorLayout.getLayoutParams();
                p.topMargin = mStatusBarHeight;

                if (mFragmentManager.getBackStackEntryCount() == 0) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_ENABLE_LOCATIONS, R.string.advice_enable_locations, mLocationButton, false);
                    } else if (mTotalDataItems > 5 && mPanelState == PANEL_STATE.NONE) {
                        mPopupAnchor.setX(mMap.getWidth() - 32 * MapTrek.density);
                        mPopupAnchor.setY(mStatusBarHeight + 8 * MapTrek.density);
                        HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_HIDE_MAP_OBJECTS, R.string.advice_hide_map_objects, mPopupAnchor, R.drawable.ic_volume_up);
                    }
                }

                if (Boolean.TRUE.equals(mGaugePanel.getTag())) {
                    mGaugePanel.setTranslationX(-mGaugePanel.getWidth());
                    mGaugePanel.setVisibility(View.VISIBLE);
                    mGaugePanel.setTag(null);
                }

                Rect area = new Rect();
                mMapView.getLocalVisibleRect(area);
                int mapWidth = area.width();
                int mapHeight = area.height();
                int pointerOffset = (int) (50 * MapTrek.density);

                area.top = mStatusBarHeight;
                if (mGaugePanel.getTranslationX() >= 0f) {
                    int h = mGaugePanel.getHeight();
                    if ((mapHeight >> 1) - h + pointerOffset < mapWidth >> 1)
                        area.left = (int) (mGaugePanel.getRight() + mGaugePanel.getTranslationX());
                }

                View v = findViewById(R.id.actionPanel);
                if (v != null) {
                    if (mVerticalOrientation)
                        area.bottom = v.getTop();
                    else
                        area.right = v.getLeft();
                }
                if (mPanelState != PANEL_STATE.NONE) {
                    if (mVerticalOrientation)
                        area.bottom = mExtendPanel.getTop();
                    else
                        area.right = mExtendPanel.getLeft();
                }

                // This part does not currently make sense as map center is not adjusted yet
                int count = mFragmentManager.getBackStackEntryCount();
                if (count > 0) {
                    FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(count - 1);
                    View contentPanel = mCoordinatorLayout.findViewById(R.id.contentPanel);
                    if ("search".equals(bse.getName()))
                        if (mVerticalOrientation)
                            area.bottom = contentPanel.getTop();
                        else
                            area.right = contentPanel.getLeft();
                }

                if (!area.isEmpty()) {
                    int centerX = mapWidth / 2;
                    int centerY = mapHeight / 2;
                    mMovingOffset = Math.min(centerX - area.left, area.right - centerX);
                    mMovingOffset = Math.min(mMovingOffset, centerY - area.top);
                    mMovingOffset = Math.min(mMovingOffset, area.bottom - centerY);
                    mMovingOffset -= pointerOffset;
                    if (mMovingOffset < 0)
                        mMovingOffset = 0;

                    mTrackingOffset = area.bottom - mapHeight / 2 - 2 * pointerOffset;

                    BitmapRenderer renderer = mMapScaleBarLayer.getRenderer();
                    renderer.setOffset(area.left + 8 * MapTrek.density, area.top);
                }

                ViewTreeObserver ob;
                if (vto.isAlive())
                    ob = vto;
                else
                    ob = mMapView.getViewTreeObserver();

                ob.removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void askForPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        } else {
            enableLocations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocations();
                    //} else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    @Override
    public Loader<List<FileDataSource>> onCreateLoader(int id, Bundle args) {
        logger.debug("onCreateLoader({})", id);
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<FileDataSource>> loader, List<FileDataSource> data) {
        logger.debug("onLoadFinished()");
        if (data == null)
            return;
        mData = data;
        for (FileDataSource source : mData) {
            if (source.isLoaded() && source.isLoadable() && !source.isVisible()) {
                addSourceToMap(source);
                source.setVisible(true);
            }
        }
        Fragment dataSourceList = mFragmentManager.findFragmentByTag("dataSourceList");
        if (dataSourceList != null)
            ((DataSourceList) dataSourceList).updateData();
        Fragment nativeTrackList = mFragmentManager.findFragmentByTag("nativeTrackList");
        if (nativeTrackList != null)
            ((DataSourceList) nativeTrackList).updateData();
        mMap.updateMap(true);
    }

    @Override
    public void onLoaderReset(Loader<List<FileDataSource>> loader) {

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("Broadcast: {}", action);
            if (MapService.BROADCAST_MAP_ADDED.equals(action) || MapService.BROADCAST_MAP_REMOVED.equals(action)) {
                mMap.clearMap();
            }
            if (BaseLocationService.BROADCAST_TRACK_SAVE.equals(action)) {
                final Bundle extras = intent.getExtras();
                boolean saved = extras != null && extras.getBoolean("saved");
                if (saved) {
                    logger.debug("Track saved: {}", extras.getString("path"));
                    //noinspection deprecation
                    Snackbar snackbar = Snackbar
                            .make(mCoordinatorLayout, R.string.msgTrackSaved, Snackbar.LENGTH_LONG)
                            .setAction(R.string.actionCustomize, view -> onTrackProperties(extras.getString("path")))
                            .setCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    if (event != DISMISS_EVENT_ACTION)
                                        HelperUtils.showTargetedAdvice(MainActivity.this, Configuration.ADVICE_RECORDED_TRACKS, R.string.advice_recorded_tracks, mRecordButton, false);
                                }
                            });
                    snackbar.show();
                    return;
                }
                String reason = extras != null ? extras.getString("reason") : null;
                logger.warn("Track not saved: {}", reason);
                if ("period".equals(reason) || "distance".equals(reason)) {
                    int msg = "period".equals(reason) ? R.string.msgTrackNotSavedPeriod : R.string.msgTrackNotSavedDistance;
                    Snackbar snackbar = Snackbar
                            .make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG)
                            .setAction(R.string.actionSave, view -> mLocationService.saveTrack());
                    snackbar.show();
                } else {
                    Exception e = extras != null ? (Exception) extras.getSerializable("exception") : null;
                    if (e == null)
                        e = new RuntimeException("Unknown error");
                    HelperUtils.showSaveError(MainActivity.this, mCoordinatorLayout, e);
                }
            }
            if (BaseNavigationService.BROADCAST_NAVIGATION_STATE.equals(action)) {
                enableNavigation();
                updateNavigationUI();
            }
            if (BaseNavigationService.BROADCAST_NAVIGATION_STATUS.equals(action) && mNavigationService != null) {
                mGaugePanel.setValue(Gauge.TYPE_DISTANCE, mNavigationService.getDistance());
                mGaugePanel.setValue(Gauge.TYPE_BEARING, mNavigationService.getBearing());
                mGaugePanel.setValue(Gauge.TYPE_TURN, mNavigationService.getTurn());
                mGaugePanel.setValue(Gauge.TYPE_VMG, mNavigationService.getVmg());
                mGaugePanel.setValue(Gauge.TYPE_XTK, mNavigationService.getXtk());
                mGaugePanel.setValue(Gauge.TYPE_ETE, mNavigationService.getEte());
                adjustNavigationArrow(mNavigationService.getTurn());
                updateNavigationUI();
            }
        }
    };

    class WaypointBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("Broadcast: {}", action);
            if (WaypointDbDataSource.BROADCAST_WAYPOINTS_MODIFIED.equals(action)) {
                sendExplicitBroadcast(WaypointDbDataSource.BROADCAST_WAYPOINTS_MODIFIED);
            }
            if (WaypointDbDataSource.BROADCAST_WAYPOINTS_RESTORED.equals(action)) {
                for (Waypoint waypoint : mWaypointDbDataSource.getWaypoints())
                    removeWaypointMarker(waypoint);
                mWaypointDbDataSource.close();
            }
            if (WaypointDbDataSource.BROADCAST_WAYPOINTS_REWRITTEN.equals(action)) {
                mWaypointDbDataSource.open();
                mWaypointDbDataSource.notifyListeners();
                for (Waypoint waypoint : mWaypointDbDataSource.getWaypoints()) {
                    if (mEditedWaypoint != null && mEditedWaypoint._id == waypoint._id)
                        mEditedWaypoint = waypoint;
                    addWaypointMarker(waypoint);
                }
            }
        }
    }

    private void addSourceToMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            addWaypointMarker(waypoint);
            mTotalDataItems++;
        }
        for (Track track : source.tracks) {
            TrackLayer trackLayer = new TrackLayer(mMap, track);
            mMap.layers().add(trackLayer, MAP_DATA);
            mTotalDataItems++;
        }
    }

    private void removeSourceFromMap(FileDataSource source) {
        for (Waypoint waypoint : source.waypoints) {
            removeWaypointMarker(waypoint);
            mTotalDataItems--;
        }
        for (Iterator<Layer> i = mMap.layers().iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if (!(layer instanceof TrackLayer))
                continue;
            if (source.tracks.contains(((TrackLayer) layer).getTrack())) {
                i.remove();
                layer.onDetach();
                mTotalDataItems--;
            }
        }
    }

    private void addWaypointMarker(Waypoint waypoint) {
        MarkerItem marker = new MarkerItem(waypoint, waypoint.name, waypoint.description, waypoint.coordinates);
        if (waypoint.style.color != 0 && waypoint.style.color != MarkerStyle.DEFAULT_COLOR) {
            Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(this, waypoint.style.color));
            marker.setMarker(new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.BOTTOM_CENTER));
        }
        mMarkerLayer.addItem(marker);
    }

    private void removeWaypointMarker(Waypoint waypoint) {
        MarkerItem marker = mMarkerLayer.getByUid(waypoint);
        mMarkerLayer.removeItem(marker);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mMap.getEventLayer().moveEnabled()) {
                AbstractMapEventLayer eventLayer = mMap.getEventLayer();
                eventLayer.enableMove(false);
                eventLayer.enableRotation(false);
                eventLayer.enableTilt(false);
                eventLayer.enableZoom(false);
                mCrosshairLayer.lock(mColorAccent);
                mPositionLocked = true;
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            for (Layer layer : mMap.layers()) {
                if (layer instanceof TrackLayer || layer instanceof MapObjectLayer || layer instanceof MarkerLayer)
                    layer.setEnabled(false);
            }
            mMap.updateMap(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            AbstractMapEventLayer eventLayer = mMap.getEventLayer();
            eventLayer.enableMove(true);
            eventLayer.enableRotation(true);
            eventLayer.enableTilt(true);
            eventLayer.enableZoom(true);
            mCrosshairLayer.unlock();
            mPositionLocked = false;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            for (Layer layer : mMap.layers()) {
                if (layer instanceof TrackLayer || layer instanceof MapObjectLayer || layer instanceof MarkerLayer)
                    layer.setEnabled(true);
            }
            mMap.updateMap(true);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public Map getMap() {
        return mMap;
    }

    @NonNull
    @Override
    public WaypointDbDataSource getWaypointDataSource() {
        return mWaypointDbDataSource;
    }

    @NonNull
    @Override
    public List<FileDataSource> getData() {
        return mData;
    }

    @Override
    public void setDataSourceAvailability(FileDataSource source, boolean available) {
        if (available) {
            if (source.isLoaded()) {
                addSourceToMap(source);
            }
        } else {
            removeSourceFromMap(source);
        }
        source.setVisible(available); // Set visibility for UI response, it does not affect other parts as source is replaced by loader
        Loader<List<FileDataSource>> loader = getLoaderManager().getLoader(0);
        if (loader != null)
            ((DataLoader) loader).markDataSourceLoadable(source, available);
        mMap.updateMap(true);
    }

    @Override
    public void onDataSourceSelected(@NonNull DataSource source) {
        Bundle args = new Bundle(3);
        if (mLocationState != LocationState.DISABLED && mLocationService != null) {
            Location location = mLocationService.getLocation();
            args.putDouble(DataList.ARG_LATITUDE, location.getLatitude());
            args.putDouble(DataList.ARG_LONGITUDE, location.getLongitude());
        } else {
            MapPosition position = mMap.getMapPosition();
            args.putDouble(DataList.ARG_LATITUDE, position.getLatitude());
            args.putDouble(DataList.ARG_LONGITUDE, position.getLongitude());
        }
        args.putInt(DataList.ARG_HEIGHT, mExtendPanel.getHeight());
        DataList fragment = (DataList) Fragment.instantiate(this, DataList.class.getName(), args);
        fragment.setDataSource(source);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        fragment.setEnterTransition(new Fade());
        ft.add(R.id.extendPanel, fragment, "dataList");
        ft.addToBackStack("dataList");
        ft.commit();
    }

    @Override
    public void onDataSourceShare(@NonNull final DataSource dataSource) {
        final boolean askName = dataSource.name == null || dataSource instanceof WaypointDbDataSource;
        final AtomicInteger selected = new AtomicInteger(0);
        final EditText inputView = new EditText(this);
        final DialogInterface.OnClickListener exportAction = (dialog, which) -> {
            if (askName)
                dataSource.name = inputView.getText().toString();
            DataExport.Builder builder = new DataExport.Builder();
            @DataExport.ExportFormat int format = dataSource.isNativeTrack() ? selected.get() : selected.get() + 1;
            DataExport dataExport = builder.setDataSource(dataSource).setFormat(format).create();
            dataExport.show(mFragmentManager, "dataExport");
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_select_format);
        builder.setSingleChoiceItems(dataSource.isNativeTrack() ? R.array.track_format_array : R.array.data_format_array,
                selected.get(), (dialog, which) -> selected.set(which));
        if (askName) {
            builder.setPositiveButton(R.string.actionContinue, (dialogInterface, i) -> {
                AlertDialog.Builder nameBuilder = new AlertDialog.Builder(MainActivity.this);
                nameBuilder.setTitle(R.string.title_input_name);
                nameBuilder.setPositiveButton(R.string.actionContinue, null);
                final AlertDialog dialog = nameBuilder.create();
                if (dataSource.name != null)
                    inputView.setText(dataSource.name);
                int margin = getResources().getDimensionPixelOffset(R.dimen.dialogContentMargin);
                dialog.setView(inputView, margin, margin >> 1, margin, 0);
                Window window = dialog.getWindow();
                if (window != null)
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (!inputView.getText().toString().trim().isEmpty()) {
                        exportAction.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                        // Hide keyboard
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null)
                            imm.hideSoftInputFromWindow(inputView.getRootView().getWindowToken(), 0);
                        dialog.dismiss();
                    }
                });
            });
        } else {
            builder.setPositiveButton(R.string.actionContinue, exportAction);
        }
        builder.setNeutralButton(R.string.explain, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        // Workaround to prevent dialog dismissing
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            StringBuilder stringBuilder = new StringBuilder();
            if (dataSource.isNativeTrack()) {
                stringBuilder.append(getString(R.string.msgNativeFormatExplanation));
                stringBuilder.append(" ");
            }
            stringBuilder.append(getString(R.string.msgOtherFormatsExplanation));
            builder1.setMessage(stringBuilder.toString());
            builder1.setPositiveButton(R.string.ok, null);
            AlertDialog dialog1 = builder1.create();
            dialog1.show();
        });
    }

    @Override
    public void onDataSourceDelete(@NonNull final DataSource source) {
        if (!(source instanceof FileDataSource)) {
            HelperUtils.showError(getString(R.string.msgCannotDeleteNativeSource), mCoordinatorLayout);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.title_delete_permanently);
        builder.setMessage(R.string.msgDeleteSourcePermanently);
        builder.setPositiveButton(R.string.actionContinue, (dialog, which) -> {
            File sourceFile = new File(((FileDataSource) source).path);
            if (sourceFile.exists()) {
                if (sourceFile.delete()) {
                    removeSourceFromMap((FileDataSource) source);
                } else {
                    HelperUtils.showError(getString(R.string.msgDeleteFailed), mCoordinatorLayout);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onAmenityKindVisibilityChanged() {
        Configuration.saveKindZoomState();
        mMap.clearMap();
    }

    @Subscribe
    public void onConfigurationChanged(Configuration.ChangedEvent event) {
        switch (event.key) {
            case Configuration.PREF_SPEED_UNIT: {
                int unit = Configuration.getSpeedUnit();
                Resources resources = getResources();
                StringFormatter.speedFactor = Float.parseFloat(resources.getStringArray(R.array.speed_factors)[unit]);
                StringFormatter.speedAbbr = resources.getStringArray(R.array.speed_abbreviations)[unit];
                mGaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_DISTANCE_UNIT: {
                int unit = Configuration.getDistanceUnit();
                Resources resources = getResources();
                StringFormatter.distanceFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors)[unit]);
                StringFormatter.distanceAbbr = resources.getStringArray(R.array.distance_abbreviations)[unit];
                StringFormatter.distanceShortFactor = Double.parseDouble(resources.getStringArray(R.array.distance_factors_short)[unit]);
                StringFormatter.distanceShortAbbr = resources.getStringArray(R.array.distance_abbreviations_short)[unit];
                mGaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_ELEVATION_UNIT: {
                int unit = Configuration.getElevationUnit();
                Resources resources = getResources();
                StringFormatter.elevationFactor = Float.parseFloat(resources.getStringArray(R.array.elevation_factors)[unit]);
                StringFormatter.elevationAbbr = resources.getStringArray(R.array.elevation_abbreviations)[unit];
                mGaugePanel.refreshGauges();
                mMap.clearMap();
                break;
            }
            case Configuration.PREF_ANGLE_UNIT: {
                int unit = Configuration.getAngleUnit();
                Resources resources = getResources();
                StringFormatter.angleFactor = Double.parseDouble(resources.getStringArray(R.array.angle_factors)[unit]);
                StringFormatter.angleAbbr = resources.getStringArray(R.array.angle_abbreviations)[unit];
                mGaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_UNIT_PRECISION: {
                boolean precision = Configuration.getUnitPrecision();
                StringFormatter.precisionFormat = precision ? "%.1f" : "%.0f";
                mGaugePanel.refreshGauges();
                break;
            }
            case Configuration.PREF_ZOOM_BUTTONS_VISIBLE: {
                boolean visible = Configuration.getZoomButtonsVisible();
                mCoordinatorLayout.findViewById(R.id.mapZoomHolder).setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            case Configuration.PREF_MAP_HILLSHADES: {
                boolean enabled = Configuration.getHillshadesEnabled();
                if (enabled)
                    showHillShade();
                else
                    hideHillShade();
                mMap.clearMap();
                break;
            }
            case Configuration.PREF_HILLSHADES_TRANSPARENCY: {
                int transparency = Configuration.getHillshadesTransparency();
                if (mHillshadeLayer != null)
                    mHillshadeLayer.setBitmapAlpha(1 - transparency * 0.01f);
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    private void checkNightMode(Location location) {
        if (mNextNightCheck > mLastLocationMilliseconds)
            return;

        mSunriseSunset.setLocation(location.getLatitude(), location.getLongitude());
        final boolean isNightTime = !mSunriseSunset.isDaytime((location.getTime() * 1d / 3600000) % 24);

        if (isNightTime ^ mNightMode) {
            // With rule categories it became a long lasting operation
            // so it has to be run in background
            mBackgroundHandler.post(() -> setNightMode(isNightTime));
        }

        mNextNightCheck = mLastLocationMilliseconds + NIGHT_CHECK_PERIOD;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level > TRIM_MEMORY_MODERATE) {
            mShieldFactory.dispose();
            mOsmcSymbolFactory.dispose();
            mMap.clearMap();
        }
    }

    private void zoomMap(double scaleBy) {
        if (mLocationOverlay.isEnabled()) {
            Point out = new Point();
            mMap.viewport().toScreenPoint(mLocationOverlay.getX(), mLocationOverlay.getY(), true, out);
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION >> 2, scaleBy, (float) out.x, (float) out.y);
        } else {
            mMap.animator().animateZoom(MAP_ZOOM_ANIMATION_DURATION, scaleBy, 0.0f, 0.0f);
        }
    }

    private void setNightMode(final boolean night) {
        Configuration.loadKindZoomState();
        ThemeFile themeFile;
        switch (Configuration.getActivity()) {
            case 2:
                themeFile = Themes.WINTER;
                break;
            case 1:
                if (Tags.kindZooms[13] == 18)
                    Tags.kindZooms[13] = 14;
                themeFile = Themes.MAPTREK;
                break;
            case 0:
            default:
                themeFile = night ? Themes.NEWTRON : Themes.MAPTREK;
        }
        IRenderTheme theme = ThemeLoader.load(themeFile);
        float fontSize = Themes.MAP_FONT_SIZES[Configuration.getMapFontSize()];
        theme.scaleTextSize(fontSize);
        mMap.setTheme(theme, true);
        mShieldFactory.setFontSize(fontSize);
        mShieldFactory.dispose();
        mOsmcSymbolFactory.dispose();
        mNightMode = night;
    }

    private void hideSystemUI() {
        Configuration.setHideSystemUI(true);
        Configuration.accountFullScreen();
        mStatusBarHeight = 0;
        // Set the IMMERSIVE flag to make content appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        // for some reason visibility is not updated if application menu was previously shown
        decorView.invalidate();
        updateMapViewArea();
    }

    private void showSystemUI() {
        mMainHandler.removeMessages(R.id.msgHideSystemUI);
        Configuration.setHideSystemUI(false);
        mStatusBarHeight = getStatusBarHeight();
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        // for some reason visibility is not updated if application menu was previously shown
        decorView.invalidate();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            return getResources().getDimensionPixelSize(resourceId);
        else
            return 0;
    }

    @Override
    public String getStatsString() {
        return Configuration.getRunningTime() + "," +
                Configuration.getTrackingTime() + "," +
                mWaypointDbDataSource.getWaypointsCount() + "," +
                mData.size() + "," +
                mNativeMapIndex.getMapsCount() + "," +
                mMapIndex.getMaps().size() + "," +
                Configuration.getFullScreenTimes();
    }

    private double movingAverage(double current, double previous) {
        return 0.2 * previous + 0.8 * current;
    }
}
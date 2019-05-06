package com.google.codelab.currentplace;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private GoogleMap mMap;

    private static final String TAG = "MapsActivity";
    ListView lstPlaces;
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnownLocation;

    private final LatLng mDefaultLocation = new LatLng(41.234802, -77.020525);
    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    private static final int M_MAX_ENTRIES = 25;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;
    public String[] mLikelyPlaceNamesClean;

    private List<Place.Type> enabledTypes = new ArrayList<>();
    private Menu mainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lstPlaces = (ListView) findViewById(R.id.listPlaces);

        String apiKey = getString(R.string.google_maps_key);
        Places.initialize(getApplicationContext(), apiKey);
        mPlacesClient = Places.createClient(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        selectAllTypes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (menu instanceof MenuBuilder) ((MenuBuilder) menu).setOptionalIconsVisible(true);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);

        MenuItem item = menu.getItem(0);
        SpannableString s = new SpannableString("Select All");
        s.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, s.length(), 0);
        item.setTitle(s);

        item = menu.getItem(1);
        s = new SpannableString("Deselect All");
        s.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, s.length(), 0);
        item.setTitle(s);

        mainMenu = menu;

        return true;
    }

    public void selectAllTypes()
    {
        enabledTypes = new LinkedList<>(Arrays.asList
                (
                        Place.Type.ACCOUNTING, Place.Type.AIRPORT, Place.Type.AMUSEMENT_PARK, Place.Type.AQUARIUM, Place.Type.ART_GALLERY,
                        Place.Type.ATM, Place.Type.BAKERY, Place.Type.BANK, Place.Type.BAR, Place.Type.BEAUTY_SALON,
                        Place.Type.BICYCLE_STORE, Place.Type.BOOK_STORE, Place.Type.BOWLING_ALLEY, Place.Type.BUS_STATION, Place.Type.CAFE,
                        Place.Type.CAMPGROUND, Place.Type.CAR_DEALER, Place.Type.CAR_RENTAL, Place.Type.CAR_REPAIR, Place.Type.CAR_WASH,
                        Place.Type.CASINO, Place.Type.CEMETERY, Place.Type.CHURCH, Place.Type.CITY_HALL, Place.Type.CLOTHING_STORE,
                        Place.Type.CONVENIENCE_STORE, Place.Type.COURTHOUSE, Place.Type.DENTIST, Place.Type.DEPARTMENT_STORE, Place.Type.DOCTOR,
                        Place.Type.ELECTRICIAN, Place.Type.ELECTRONICS_STORE, Place.Type.EMBASSY, Place.Type.FIRE_STATION, Place.Type.FLORIST,
                        Place.Type.FUNERAL_HOME, Place.Type.FURNITURE_STORE, Place.Type.GAS_STATION, Place.Type.GYM, Place.Type.HAIR_CARE,
                        Place.Type.HARDWARE_STORE, Place.Type.HINDU_TEMPLE, Place.Type.HOME_GOODS_STORE, Place.Type.HOSPITAL, Place.Type.INSURANCE_AGENCY,
                        Place.Type.JEWELRY_STORE, Place.Type.LAUNDRY, Place.Type.LAWYER, Place.Type.LIBRARY, Place.Type.LIQUOR_STORE,
                        Place.Type.LOCAL_GOVERNMENT_OFFICE, Place.Type.LOCKSMITH, Place.Type.LODGING, Place.Type.MEAL_DELIVERY, Place.Type.MEAL_TAKEAWAY,
                        Place.Type.MOSQUE, Place.Type.MOVIE_RENTAL, Place.Type.MOVIE_THEATER, Place.Type.MOVING_COMPANY, Place.Type.MUSEUM,
                        Place.Type.NIGHT_CLUB, Place.Type.PAINTER, Place.Type.PARK, Place.Type.PARKING, Place.Type.PET_STORE,
                        Place.Type.PHARMACY, Place.Type.PHYSIOTHERAPIST, Place.Type.PLUMBER, Place.Type.POLICE, Place.Type.POST_OFFICE,
                        Place.Type.REAL_ESTATE_AGENCY, Place.Type.RESTAURANT, Place.Type.ROOFING_CONTRACTOR, Place.Type.RV_PARK, Place.Type.SCHOOL,
                        Place.Type.SHOE_STORE, Place.Type.SHOPPING_MALL, Place.Type.SPA, Place.Type.STADIUM, Place.Type.STORAGE,
                        Place.Type.STORE, Place.Type.SUBWAY_STATION, Place.Type.SUPERMARKET, Place.Type.SYNAGOGUE, Place.Type.TAXI_STAND,
                        Place.Type.TRAIN_STATION, Place.Type.TRANSIT_STATION, Place.Type.TRAVEL_AGENCY, Place.Type.VETERINARY_CARE, Place.Type.ZOO
                ));
    }

    public void deselectAllTypes() { enabledTypes = new LinkedList<>(); }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.select_all:
                selectAllTypes();
                for (int i = 2; i < mainMenu.size(); i++) mainMenu.getItem(i).setIcon(R.drawable.radio_button_selected);
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.deselect_all:
                deselectAllTypes();
                for (int i = 2; i < mainMenu.size(); i++) mainMenu.getItem(i).setIcon(R.drawable.radio_button);
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.accounting:
                if (enabledTypes.contains(Place.Type.ACCOUNTING)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ACCOUNTING); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ACCOUNTING); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.airport:
                if (enabledTypes.contains(Place.Type.AIRPORT)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.AIRPORT); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.AIRPORT); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.amusement_park:
                if (enabledTypes.contains(Place.Type.AMUSEMENT_PARK)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.AMUSEMENT_PARK); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.AMUSEMENT_PARK); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.aquarium:
                if (enabledTypes.contains(Place.Type.AQUARIUM)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.AQUARIUM); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.AQUARIUM); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.art_gallery:
                if (enabledTypes.contains(Place.Type.ART_GALLERY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ART_GALLERY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ART_GALLERY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.atm:
                if (enabledTypes.contains(Place.Type.ATM)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ATM); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ATM); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bakery:
                if (enabledTypes.contains(Place.Type.BAKERY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BAKERY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BAKERY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bank:
                if (enabledTypes.contains(Place.Type.BANK)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BANK); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BANK); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bar:
                if (enabledTypes.contains(Place.Type.BAR)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BAR); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BAR); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.beauty_salon:
                if (enabledTypes.contains(Place.Type.BEAUTY_SALON)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BEAUTY_SALON); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BEAUTY_SALON); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bicycle_store:
                if (enabledTypes.contains(Place.Type.BICYCLE_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BICYCLE_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BICYCLE_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.book_store:
                if (enabledTypes.contains(Place.Type.BOOK_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BOOK_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BOOK_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bowling_alley:
                if (enabledTypes.contains(Place.Type.BOWLING_ALLEY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BOWLING_ALLEY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BOWLING_ALLEY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.bus_station:
                if (enabledTypes.contains(Place.Type.BUS_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.BUS_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.BUS_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.cafe:
                if (enabledTypes.contains(Place.Type.CAFE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAFE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAFE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.campground:
                if (enabledTypes.contains(Place.Type.CAMPGROUND)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAMPGROUND); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAMPGROUND); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.car_dealer:
                if (enabledTypes.contains(Place.Type.CAR_DEALER)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAR_DEALER); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAR_DEALER); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.car_rental:
                if (enabledTypes.contains(Place.Type.CAR_RENTAL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAR_RENTAL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAR_RENTAL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.car_repair:
                if (enabledTypes.contains(Place.Type.CAR_REPAIR)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAR_REPAIR); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAR_REPAIR); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.car_wash:
                if (enabledTypes.contains(Place.Type.CAR_WASH)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CAR_WASH); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CAR_WASH); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.casino:
                if (enabledTypes.contains(Place.Type.CASINO)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CASINO); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CASINO); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.cemetery:
                if (enabledTypes.contains(Place.Type.CEMETERY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CEMETERY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CEMETERY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.church:
                if (enabledTypes.contains(Place.Type.CHURCH)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CHURCH); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CHURCH); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.city_hall:
                if (enabledTypes.contains(Place.Type.CITY_HALL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CITY_HALL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CITY_HALL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.clothing_store:
                if (enabledTypes.contains(Place.Type.CLOTHING_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CLOTHING_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CLOTHING_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.convenience_store:
                if (enabledTypes.contains(Place.Type.CONVENIENCE_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.CONVENIENCE_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.CONVENIENCE_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.courthouse:
                if (enabledTypes.contains(Place.Type.COURTHOUSE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.COURTHOUSE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.COURTHOUSE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.dentist:
                if (enabledTypes.contains(Place.Type.DENTIST)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.DENTIST); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.DENTIST); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.department_store:
                if (enabledTypes.contains(Place.Type.DEPARTMENT_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.DEPARTMENT_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.DEPARTMENT_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.doctor:
                if (enabledTypes.contains(Place.Type.DOCTOR)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.DOCTOR); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.DOCTOR); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.electrician:
                if (enabledTypes.contains(Place.Type.ELECTRICIAN)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ELECTRICIAN); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ELECTRICIAN); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.electronics_store:
                if (enabledTypes.contains(Place.Type.ELECTRONICS_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ELECTRONICS_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ELECTRONICS_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.embassy:
                if (enabledTypes.contains(Place.Type.EMBASSY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.EMBASSY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.EMBASSY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.fire_station:
                if (enabledTypes.contains(Place.Type.FIRE_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.FIRE_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.FIRE_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.florist:
                if (enabledTypes.contains(Place.Type.FLORIST)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.FLORIST); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.FLORIST); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.funeral_home:
                if (enabledTypes.contains(Place.Type.FUNERAL_HOME)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.FUNERAL_HOME); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.FUNERAL_HOME); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.furniture_store:
                if (enabledTypes.contains(Place.Type.FURNITURE_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.FURNITURE_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.FURNITURE_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.gas_station:
                if (enabledTypes.contains(Place.Type.GAS_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.GAS_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.GAS_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.gym:
                if (enabledTypes.contains(Place.Type.GYM)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.GYM); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.GYM); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.hair_care:
                if (enabledTypes.contains(Place.Type.HAIR_CARE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.HAIR_CARE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.HAIR_CARE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.hardware_store:
                if (enabledTypes.contains(Place.Type.HARDWARE_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.HARDWARE_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.HARDWARE_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.hindu_temple:
                if (enabledTypes.contains(Place.Type.HINDU_TEMPLE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.HINDU_TEMPLE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.HINDU_TEMPLE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.home_goods_store:
                if (enabledTypes.contains(Place.Type.HOME_GOODS_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.HOME_GOODS_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.HOME_GOODS_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.hospital:
                if (enabledTypes.contains(Place.Type.HOSPITAL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.HOSPITAL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.HOSPITAL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.insurance_agency:
                if (enabledTypes.contains(Place.Type.INSURANCE_AGENCY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.INSURANCE_AGENCY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.INSURANCE_AGENCY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.jewelry_store:
                if (enabledTypes.contains(Place.Type.JEWELRY_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.JEWELRY_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.JEWELRY_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.laundry:
                if (enabledTypes.contains(Place.Type.LAUNDRY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LAUNDRY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LAUNDRY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.lawyer:
                if (enabledTypes.contains(Place.Type.LAWYER)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LAWYER); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LAWYER); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.library:
                if (enabledTypes.contains(Place.Type.LIBRARY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LIBRARY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LIBRARY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.liquor_store:
                if (enabledTypes.contains(Place.Type.LIQUOR_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LIQUOR_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LIQUOR_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.local_government_office:
                if (enabledTypes.contains(Place.Type.LOCAL_GOVERNMENT_OFFICE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LOCAL_GOVERNMENT_OFFICE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LOCAL_GOVERNMENT_OFFICE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.locksmith:
                if (enabledTypes.contains(Place.Type.LOCKSMITH)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LOCKSMITH); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LOCKSMITH); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.lodging:
                if (enabledTypes.contains(Place.Type.LODGING)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.LODGING); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.LODGING); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.meal_delivery:
                if (enabledTypes.contains(Place.Type.MEAL_DELIVERY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MEAL_DELIVERY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MEAL_DELIVERY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.meal_takeaway:
                if (enabledTypes.contains(Place.Type.MEAL_TAKEAWAY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MEAL_TAKEAWAY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MEAL_TAKEAWAY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.mosque:
                if (enabledTypes.contains(Place.Type.MOSQUE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MOSQUE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MOSQUE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.movie_rental:
                if (enabledTypes.contains(Place.Type.MOVIE_RENTAL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MOVIE_RENTAL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MOVIE_RENTAL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.movie_theater:
                if (enabledTypes.contains(Place.Type.MOVIE_THEATER)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MOVIE_THEATER); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MOVIE_THEATER); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.moving_company:
                if (enabledTypes.contains(Place.Type.MOVING_COMPANY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MOVING_COMPANY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MOVING_COMPANY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.museum:
                if (enabledTypes.contains(Place.Type.MUSEUM)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.MUSEUM); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.MUSEUM); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.night_club:
                if (enabledTypes.contains(Place.Type.NIGHT_CLUB)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.NIGHT_CLUB); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.NIGHT_CLUB); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.painter:
                if (enabledTypes.contains(Place.Type.PAINTER)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PAINTER); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PAINTER); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.park:
                if (enabledTypes.contains(Place.Type.PARK)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PARK); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PARK); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.parking:
                if (enabledTypes.contains(Place.Type.PARKING)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PARKING); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PARKING); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.pet_store:
                if (enabledTypes.contains(Place.Type.PET_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PET_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PET_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.pharmacy:
                if (enabledTypes.contains(Place.Type.PHARMACY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PHARMACY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PHARMACY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.physiotherapist:
                if (enabledTypes.contains(Place.Type.PHYSIOTHERAPIST)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PHYSIOTHERAPIST); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PHYSIOTHERAPIST); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.plumber:
                if (enabledTypes.contains(Place.Type.PLUMBER)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.PLUMBER); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.PLUMBER); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.police:
                if (enabledTypes.contains(Place.Type.POLICE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.POLICE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.POLICE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.post_office:
                if (enabledTypes.contains(Place.Type.POST_OFFICE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.POST_OFFICE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.POST_OFFICE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.real_estate_agency:
                if (enabledTypes.contains(Place.Type.REAL_ESTATE_AGENCY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.REAL_ESTATE_AGENCY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.REAL_ESTATE_AGENCY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.restaurant:
                if (enabledTypes.contains(Place.Type.RESTAURANT)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.RESTAURANT); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.RESTAURANT); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.roofing_contractor:
                if (enabledTypes.contains(Place.Type.ROOFING_CONTRACTOR)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ROOFING_CONTRACTOR); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ROOFING_CONTRACTOR); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.rv_park:
                if (enabledTypes.contains(Place.Type.RV_PARK)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.RV_PARK); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.RV_PARK); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.school:
                if (enabledTypes.contains(Place.Type.SCHOOL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SCHOOL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SCHOOL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.shoe_store:
                if (enabledTypes.contains(Place.Type.SHOE_STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SHOE_STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SHOE_STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.shopping_mall:
                if (enabledTypes.contains(Place.Type.SHOPPING_MALL)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SHOPPING_MALL); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SHOPPING_MALL); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.spa:
                if (enabledTypes.contains(Place.Type.SPA)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SPA); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SPA); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.stadium:
                if (enabledTypes.contains(Place.Type.STADIUM)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.STADIUM); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.STADIUM); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.storage:
                if (enabledTypes.contains(Place.Type.STORAGE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.STORAGE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.STORAGE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.store:
                if (enabledTypes.contains(Place.Type.STORE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.STORE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.STORE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.subway_station:
                if (enabledTypes.contains(Place.Type.SUBWAY_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SUBWAY_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SUBWAY_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.supermarket:
                if (enabledTypes.contains(Place.Type.SUPERMARKET)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SUPERMARKET); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SUPERMARKET); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.synagogue:
                if (enabledTypes.contains(Place.Type.SYNAGOGUE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.SYNAGOGUE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.SYNAGOGUE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.taxi_stand:
                if (enabledTypes.contains(Place.Type.TAXI_STAND)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.TAXI_STAND); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.TAXI_STAND); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.train_station:
                if (enabledTypes.contains(Place.Type.TRAIN_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.TRAIN_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.TRAIN_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.transit_station:
                if (enabledTypes.contains(Place.Type.TRANSIT_STATION)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.TRANSIT_STATION); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.TRANSIT_STATION); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.travel_agency:
                if (enabledTypes.contains(Place.Type.TRAVEL_AGENCY)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.TRAVEL_AGENCY); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.TRAVEL_AGENCY); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.veterinary_care:
                if (enabledTypes.contains(Place.Type.VETERINARY_CARE)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.VETERINARY_CARE); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.VETERINARY_CARE); }
                getCurrentPlaceLikelihoods();
                return false;
            case R.id.zoo:
                if (enabledTypes.contains(Place.Type.ZOO)) { item.setIcon(R.drawable.radio_button); enabledTypes.remove(Place.Type.ZOO); }
                else { item.setIcon(R.drawable.radio_button_selected); enabledTypes.add(Place.Type.ZOO); }
                getCurrentPlaceLikelihoods();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        LatLng pct = new LatLng(41.233841, -77.025242);
        mMap.addMarker(new MarkerOptions().position(pct).title("Marker at Penn College"));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        getCurrentPlaceLikelihoods();
        getLocationPermission();
        getDeviceLocation(null);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pct));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        mMap.setOnMapLongClickListener(this::onMapLongClick);
    }

    private void getLocationPermission()
    {
        mLocationPermissionGranted = false;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            mLocationPermissionGranted = true;
            getDeviceLocation(null);
            getCurrentPlaceLikelihoods();
        }
        else
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        mLocationPermissionGranted = false;

        switch (requestCode)
        {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getDeviceLocation(findViewById(R.layout.activity_maps));
                    getCurrentPlaceLikelihoods();
                    mLocationPermissionGranted = true;
                    getDeviceLocation(findViewById(R.layout.activity_maps));
                }
            }
        }
    }

    public void getDeviceLocation(View view)
    {
        try
        {
            if (mLocationPermissionGranted)
            {
                @SuppressLint("MissingPermission") Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();

                locationResult.addOnSuccessListener(this, new OnSuccessListener<Location>()
                {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onSuccess(Location location)
                    {
                        if (location != null)
                        {
                            mLastKnownLocation = location;
                            Log.d(TAG, "Latitude: " + mLastKnownLocation.getLatitude());
                            Log.d(TAG, "Longitude: " + mLastKnownLocation.getLongitude());
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude())).title("Current location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            getCurrentPlaceLikelihoods();
                        }
                        else
                        {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        }

                        getCurrentPlaceLikelihoods();
                    }
                });
            }
        }
        catch (Exception e) { Log.e("Exception: %s", e.getMessage()); }
    }

    private void pickCurrentPlace()
    {
        if (mMap == null) return;

        if (mLocationPermissionGranted) getDeviceLocation(null);
        else
        {
            Log.i(TAG, "The user did not grant location permission.");

            mMap.addMarker(new MarkerOptions().title(getString(R.string.default_info_title)).position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            getLocationPermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getCurrentPlaceLikelihoods()
    {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES);

        @SuppressWarnings("MissingPermission") FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
        @SuppressLint("MissingPermission") Task<FindCurrentPlaceResponse> placeResponse = mPlacesClient.findCurrentPlace(request);

        placeResponse.addOnCompleteListener(task ->
        {
            if (task.isSuccessful())
            {
                FindCurrentPlaceResponse response = task.getResult();

                int count;
                assert response != null;
                if (response.getPlaceLikelihoods().size() < M_MAX_ENTRIES) count = response.getPlaceLikelihoods().size();
                else count = M_MAX_ENTRIES;

                int i = 0;
                mLikelyPlaceNames = new String[count];
                mLikelyPlaceAddresses = new String[count];
                mLikelyPlaceAttributions = new String[count];
                mLikelyPlaceLatLngs = new LatLng[count];
                Collection<Place.Type> typeCollection;
                Place currPlace;

                for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods())
                {
                    currPlace = placeLikelihood.getPlace();

                    List<Place.Type> intersect = enabledTypes.stream()
                            .filter(Objects.requireNonNull(currPlace.getTypes())::contains)
                            .collect(Collectors.toList());

                    if (intersect.size() > 0)
                    {
                        if (currPlace.getName() != null) mLikelyPlaceNames[i] = currPlace.getName();
                        else mLikelyPlaceNames[i] = " ";
                        mLikelyPlaceAddresses[i] = currPlace.getAddress();
                        mLikelyPlaceAttributions[i] = (currPlace.getAttributions() == null) ? null : String.join(" ", currPlace.getAttributions());
                        mLikelyPlaceLatLngs[i] = currPlace.getLatLng();
                        String currLatLng = (mLikelyPlaceLatLngs[i] == null) ? "" : mLikelyPlaceLatLngs[i].toString();
                        i++;
                    }
                }

                int nullCounter = 0;
                for (int n = 0; n < mLikelyPlaceNames.length; n++) if (mLikelyPlaceNames[n] != null) nullCounter++;
                mLikelyPlaceNamesClean = new String[nullCounter];
                int jkl = 0;

                for (int asdf = 0; asdf < mLikelyPlaceNames.length; asdf++)
                {
                    if (mLikelyPlaceNames[asdf] != null)
                    {
                        mLikelyPlaceNamesClean[jkl] = mLikelyPlaceNames[asdf];
                        jkl++;
                    }
                }

                fillPlacesList();
            }
            else
            {
                Exception exception = task.getException();
                if (exception instanceof ApiException)
                {
                    ApiException apiException = (ApiException) exception;
                    Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                }
            }
        });
    }

    private void fillPlacesList()
    {
        ArrayAdapter<String> placesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mLikelyPlaceNamesClean);
        lstPlaces.setAdapter(placesAdapter);
        lstPlaces.setOnItemClickListener(listClickedHandler);
    }

    private AdapterView.OnItemClickListener listClickedHandler = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView parent, View v, int position, long id)
        {
            mMap.clear();
            LatLng markerLatLng = mLikelyPlaceLatLngs[position];
            String markerSnippet = mLikelyPlaceAddresses[position];
            if (mLikelyPlaceAttributions[position] != null) markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[position];
            mMap.addMarker(new MarkerOptions().title(mLikelyPlaceNames[position]).position(markerLatLng).snippet(markerSnippet));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(markerLatLng));
        }
    };

    public void openOptionsMenu(View view) {
        this.openOptionsMenu();
    }

    public void onMapLongClick(LatLng point)
    {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(point).title("Current location"));

        Location location = new Location(LocationManager.GPS_PROVIDER);
        Double lat = Double.valueOf(point.latitude);
        location.setLatitude(lat);
        Double longi = Double.valueOf(point.longitude);
        location.setLongitude(longi);

        getCurrentPlaceLikelihoods();
    }
}
package wangdaye.com.geometricweather.background.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.service.quicksettings.Tile;

import androidx.annotation.RequiresApi;

import wangdaye.com.geometricweather.basic.models.Location;
import wangdaye.com.geometricweather.basic.models.weather.Weather;
import wangdaye.com.geometricweather.db.DatabaseHelper;
import wangdaye.com.geometricweather.resource.ResourceHelper;
import wangdaye.com.geometricweather.resource.providers.ResourcesProviderFactory;
import wangdaye.com.geometricweather.settings.SettingsOptionManager;
import wangdaye.com.geometricweather.utils.helpters.IntentHelper;
import wangdaye.com.geometricweather.utils.managers.TimeManager;

/**
 * Tile service.
 * */

@RequiresApi(api = Build.VERSION_CODES.N)
public class TileService extends android.service.quicksettings.TileService {

    @Override
    public void onTileAdded() {
        refreshTile(this, getQsTile());
    }

    @Override
    public void onTileRemoved() {
        // do nothing.
    }

    @Override
    public void onStartListening () {
        refreshTile(this, getQsTile());
    }

    @Override
    public void onStopListening () {
        refreshTile(this, getQsTile());
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onClick () {
        try {
            Object statusBarManager = getSystemService("statusbar");
            if (statusBarManager != null) {
                statusBarManager
                        .getClass()
                        .getMethod("collapsePanels")
                        .invoke(statusBarManager);
            }
        } catch (Exception ignored) {

        }
        IntentHelper.startMainActivity(this);
    }

    private static void refreshTile(Context context, Tile tile) {
        if (tile == null) {
            return;
        }
        Location location = DatabaseHelper.getInstance(context).readLocationList().get(0);
        Weather weather = DatabaseHelper.getInstance(context).readWeather(location);
        if (weather != null) {
            tile.setIcon(
                    ResourceHelper.getMinimalIcon(
                            ResourcesProviderFactory.getNewInstance(),
                            weather.getCurrent().getWeatherCode(),
                            TimeManager.getInstance(context).isDayTime()
                    )
            );
            tile.setLabel(
                    weather.getCurrent().getTemperature().getTemperature(
                            context,
                            SettingsOptionManager.getInstance(context).getTemperatureUnit())
            );
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}
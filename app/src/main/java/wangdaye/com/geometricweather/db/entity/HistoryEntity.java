package wangdaye.com.geometricweather.db.entity;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.greenrobot.greendao.annotation.Entity;

import wangdaye.com.geometricweather.basic.model.History;
import wangdaye.com.geometricweather.basic.model.Location;
import wangdaye.com.geometricweather.basic.model.weather.Weather;

import org.greenrobot.greendao.annotation.Id;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.greenrobot.greendao.annotation.Generated;

/**
 * History entity.
 * */

@Entity
public class HistoryEntity {

    @Id
    public Long id;

    public String cityId;
    public String city;
    public String date;

    public int maxiTemp;
    public int miniTemp;

    @Generated(hash = 1344990835)
    public HistoryEntity(Long id, String cityId, String city, String date, int maxiTemp, int miniTemp) {
        this.id = id;
        this.cityId = cityId;
        this.city = city;
        this.date = date;
        this.maxiTemp = maxiTemp;
        this.miniTemp = miniTemp;
    }

    @Generated(hash = 1235354573)
    public HistoryEntity() {
    }

    private static HistoryEntity buildHistoryEntity(History history) {
        HistoryEntity entity = new HistoryEntity();
        entity.cityId = history.cityId;
        entity.city = history.city;
        entity.date = history.date;
        entity.maxiTemp = history.maxiTemp;
        entity.miniTemp = history.miniTemp;
        return entity;
    }

    // insert.

    public static void insertTodayHistory(SQLiteDatabase database, Weather weather) {
        if (weather == null) {
            return;
        }

        History yesterday = searchYesterdayHistory(database, weather);
        clearLocationHistory(database, weather);

        HistoryEntityDao dao = new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao();
        if (yesterday != null) {
            dao.insert(buildHistoryEntity(yesterday));
        }
        dao.insert(buildHistoryEntity(
                new History(
                        weather.base.cityId,
                        weather.base.city,
                        weather.base.date,
                        weather.dailyList.get(0).temps[0],
                        weather.dailyList.get(0).temps[1])
                )
        );
    }

    public static void insertYesterdayHistory(SQLiteDatabase database, History history) {
        if (history == null) {
            return;
        }

        History today = searchTodayHistory(database, history);
        clearLocationHistory(database, history);

        HistoryEntityDao dao = new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao();
        dao.insert(buildHistoryEntity(history));
        if (today != null) {
            dao.insert(buildHistoryEntity(today));
        }
    }

    // delete.

    public static void clearLocationHistory(SQLiteDatabase database, Location location) {
        if (location == null) {
            return;
        }

        List<HistoryEntity> entityList = searchHistoryEntity(database, location);
        new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .deleteInTx(entityList);
    }

    private static void clearLocationHistory(SQLiteDatabase database, Weather weather) {
        if (weather == null) {
            return;
        }

        List<HistoryEntity> entityList = searchHistoryEntity(database, weather);
        new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .deleteInTx(entityList);
    }

    private static void clearLocationHistory(SQLiteDatabase database, History history) {
        if (history == null) {
            return;
        }

        List<HistoryEntity> entityList = searchHistoryEntity(database, history);
        new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .deleteInTx(entityList);
    }

    @SuppressLint("SimpleDateFormat")
    public static History searchYesterdayHistory(SQLiteDatabase database, Weather weather) {
        if (weather == null) {
            return null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date date = format.parse(weather.base.date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(Objects.requireNonNull(date));
            calendar.add(Calendar.DATE, -1);

            List<HistoryEntity> entityList = new DaoMaster(database)
                    .newSession()
                    .getHistoryEntityDao()
                    .queryBuilder()
                    .where(
                            HistoryEntityDao.Properties.Date.eq(format.format(calendar.getTime())),
                            HistoryEntityDao.Properties.CityId.eq(weather.base.cityId)
                    ).list();

            if (entityList == null || entityList.size() <= 0) {
                return null;
            } else {
                return entityList.get(0).toHistory();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("SimpleDateFormat")
    private static History searchTodayHistory(SQLiteDatabase database, History history) {
        if (history == null) {
            return null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date date = format.parse(history.date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(Objects.requireNonNull(date));

            List<HistoryEntity> entityList = new DaoMaster(database)
                    .newSession()
                    .getHistoryEntityDao()
                    .queryBuilder()
                    .where(
                            HistoryEntityDao.Properties.Date.eq(format.format(calendar.getTime())),
                            HistoryEntityDao.Properties.CityId.eq(history.cityId))
                    .list();
            if (entityList == null || entityList.size() <= 0) {
                return null;
            } else {
                return entityList.get(0).toHistory();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<HistoryEntity> searchHistoryEntity(SQLiteDatabase database, Location location) {
        if (location == null || TextUtils.isEmpty(location.cityId)) {
            return new ArrayList<>();
        }
        return new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .queryBuilder()
                .where(HistoryEntityDao.Properties.CityId.eq(location.cityId))
                .list();
    }

    private static List<HistoryEntity> searchHistoryEntity(SQLiteDatabase database, Weather weather) {
        if (weather == null) {
            return new ArrayList<>();
        }
        return new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .queryBuilder()
                .where(HistoryEntityDao.Properties.CityId.eq(weather.base.cityId))
                .list();
    }

    private static List<HistoryEntity> searchHistoryEntity(SQLiteDatabase database, History history) {
        if (history == null) {
            return new ArrayList<>();
        }

        return new DaoMaster(database)
                .newSession()
                .getHistoryEntityDao()
                .queryBuilder()
                .where(HistoryEntityDao.Properties.CityId.eq(history.cityId))
                .list();
    }

    private History toHistory() {
        return new History(cityId, city, date, maxiTemp, miniTemp);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCityId() {
        return this.cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getMaxiTemp() {
        return this.maxiTemp;
    }

    public void setMaxiTemp(int maxiTemp) {
        this.maxiTemp = maxiTemp;
    }

    public int getMiniTemp() {
        return this.miniTemp;
    }

    public void setMiniTemp(int miniTemp) {
        this.miniTemp = miniTemp;
    }
}

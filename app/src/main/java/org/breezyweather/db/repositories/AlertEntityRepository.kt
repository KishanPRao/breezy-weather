package org.breezyweather.db.repositories

import org.breezyweather.common.basic.models.options.provider.WeatherSource
import org.breezyweather.db.ObjectBox.boxStore
import org.breezyweather.db.converters.WeatherSourceConverter
import org.breezyweather.db.entities.AlertEntity_

object AlertEntityRepository {
    // insert.
    fun insertAlertList(entityList: List<org.breezyweather.db.entities.AlertEntity>) {
        boxStore.boxFor(org.breezyweather.db.entities.AlertEntity::class.java).put(entityList)
    }

    // delete.
    fun deleteAlertList(entityList: List<org.breezyweather.db.entities.AlertEntity>) {
        boxStore.boxFor(org.breezyweather.db.entities.AlertEntity::class.java).remove(entityList)
    }

    // search.
    fun selectLocationAlertEntity(cityId: String, source: WeatherSource): List<org.breezyweather.db.entities.AlertEntity> {
        val query = boxStore.boxFor(org.breezyweather.db.entities.AlertEntity::class.java)
            .query(
                AlertEntity_.cityId.equal(cityId)
                    .and(
                        AlertEntity_.weatherSource.equal(
                            WeatherSourceConverter().convertToDatabaseValue(source)
                        )
                    )
            ).build()
        val results = query.find()
        query.close()
        return results
    }
}
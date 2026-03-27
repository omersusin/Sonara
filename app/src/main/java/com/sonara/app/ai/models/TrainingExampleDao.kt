package com.sonara.app.ai.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingExampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(example: TrainingExample): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(examples: List<TrainingExample>)

    @Query("SELECT * FROM sonara_training_examples ORDER BY timestamp DESC")
    suspend fun getAll(): List<TrainingExample>

    @Query("SELECT COUNT(*) FROM sonara_training_examples")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM sonara_training_examples WHERE source != 'prototype'")
    suspend fun getLearnedCount(): Int

    @Query("SELECT COUNT(*) FROM sonara_training_examples WHERE source != 'prototype'")
    fun getLearnedCountFlow(): Flow<Int>

    @Query("UPDATE sonara_training_examples SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Long)

    @Query("DELETE FROM sonara_training_examples WHERE source = 'prototype'")
    suspend fun deletePrototypes()

    @Query("DELETE FROM sonara_training_examples WHERE source = 'community'")
    suspend fun deleteCommunity()

    @Query("DELETE FROM sonara_training_examples")
    suspend fun deleteAll()
}

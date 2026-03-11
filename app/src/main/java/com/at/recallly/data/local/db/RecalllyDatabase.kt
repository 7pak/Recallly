package com.at.recallly.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.at.recallly.data.local.db.dao.UserDao
import com.at.recallly.data.local.db.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RecalllyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

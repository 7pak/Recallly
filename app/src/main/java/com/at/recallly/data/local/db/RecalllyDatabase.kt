package com.at.recallly.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.at.recallly.data.local.db.dao.UserDao
import com.at.recallly.data.local.db.dao.UserProfileDao
import com.at.recallly.data.local.db.entity.UserEntity
import com.at.recallly.data.local.db.entity.UserProfileEntity

@Database(
    entities = [UserEntity::class, UserProfileEntity::class],
    version = 2,
    exportSchema = true
)
abstract class RecalllyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userProfileDao(): UserProfileDao
}

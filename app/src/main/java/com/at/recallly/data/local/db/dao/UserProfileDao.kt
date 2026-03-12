package com.at.recallly.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.at.recallly.data.local.db.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    suspend fun getByUid(uid: String): UserProfileEntity?

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

package com.at.recallly.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.at.recallly.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?
) {
    fun toDomainModel(): User = User(
        id = id,
        email = email,
        displayName = displayName
    )

    companion object {
        fun fromDomainModel(user: User): UserEntity = UserEntity(
            id = user.id,
            email = user.email,
            displayName = user.displayName
        )
    }
}

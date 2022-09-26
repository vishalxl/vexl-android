package cz.cleevio.cache.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["extId"], unique = true)])
data class MyOfferEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val extId: String,
	val adminId: String,
	val privateKey: String,
	val publicKey: String,
	val offerType: String,
	val isInboxCreated: Boolean,
	//version 2
	@ColumnInfo(defaultValue = "")
	val encryptedForKeys: String,
)

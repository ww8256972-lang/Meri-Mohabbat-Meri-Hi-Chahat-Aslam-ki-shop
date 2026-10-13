package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.MovementType
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus

class Converters {
    @TypeConverter
    fun fromMovementType(value: MovementType?): String? = value?.name

    @TypeConverter
    fun toMovementType(value: String?): MovementType? = value?.let { MovementType.valueOf(it) }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let { PaymentStatus.valueOf(it) }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode?): String? = value?.name

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode? = value?.let { PaymentMode.valueOf(it) }
}

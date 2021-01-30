package com.ahmed.receiver.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class User(@PrimaryKey val id: String, val name: String, val username: String, val email: String,
                @Embedded val address: Address, val phone: String, @Embedded val company: Company)

data class Address(val street: String, val suite: String, val city: String, val zipcode: String, @Embedded val geo: Geo)

data class Geo(val lat: String, val lng: String)

data class Company(@SerializedName("name") val companyName: String, val catchPhrase: String, val bs: String)
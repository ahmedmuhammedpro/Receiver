package com.ahmed.receiver.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.ahmed.receiver.database.User
import com.ahmed.receiver.database.UserDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception

class ReceiverViewModel(app: Application) : AndroidViewModel(app) {

    enum class DatabaseResult {
        OK, NOK
    }

    private val _insertingResult = MutableLiveData<DatabaseResult>()
    val insertingResult: LiveData<DatabaseResult> = _insertingResult
    private val _eventDatabaseError = MutableLiveData(false)
    val eventDatabaseError: LiveData<Boolean> = _eventDatabaseError
    private var userDatabase: UserDatabase = UserDatabase.getInstance(app)

    suspend fun saveUser(user: User) {
        withContext(Dispatchers.IO) {
            try {
                delay(4000)
                val result = userDatabase.userDao.insertUser(user)
                withContext(Dispatchers.Main) {
                    _eventDatabaseError.value = false
                    _insertingResult.value = if (result > 1) DatabaseResult.OK else DatabaseResult.NOK
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                withContext(Dispatchers.Main) {
                    _eventDatabaseError.value = true
                }
            }
        }
    }

    fun getUserById(id: String): LiveData<User> {
        return userDatabase.userDao.getUserById(id)
    }

    fun convertStringJsonToUser(json: String): User {
        return Gson().fromJson(json, User::class.java)
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReceiverViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReceiverViewModel(app) as T
            }

            throw IllegalArgumentException("Unable to construct viewmodel")
        }

    }

}
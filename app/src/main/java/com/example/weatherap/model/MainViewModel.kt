package com.example.weatherap.model

import androidx.browser.browseractions.BrowserServiceFileProvider.loadBitmap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherap.api.WeatherService
import com.example.weatherap.api.toForecast
import com.example.weatherap.api.toWeather
import com.example.weatherap.db.fb.FBCity
import com.example.weatherap.db.fb.FBDatabase
import com.example.weatherap.db.fb.FBUser
import com.example.weatherap.db.fb.toFBCity
import com.example.weatherap.ui.nav.Route
import com.google.android.gms.maps.model.LatLng

class MainViewModel (private val db: FBDatabase, private val service : WeatherService): ViewModel(),
    FBDatabase.Listener {
    private val _cities = mutableStateMapOf<String, City>()

    private val _forecast = mutableStateMapOf<String, List<Forecast>?>()

    private var _city = mutableStateOf<String?>(null)
    var city: String?
        get() = _city.value
        set(tmp) { _city.value = tmp }

    val cities : List<City>
        get() = _cities.values.toList().sortedBy { it.name }

    private val _weather = mutableStateMapOf<String, Weather>()
    private val _user = mutableStateOf<User?> (null)

    private var _page = mutableStateOf<Route>(Route.Home)
    var page: Route
        get() = _page.value
        set(tmp) { _page.value = tmp }


    val user : User?
        get() = _user.value
    init {
        db.setListener(this)
    }

    private fun loadForecast(name: String) {
        service.getForecast(name) { apiForecast ->
            apiForecast?.let {
                _forecast[name] = apiForecast.toForecast()
            }
        }
    }

    fun forecast (name: String) = _forecast.getOrPut(name) {
        loadForecast(name)
        emptyList() // return
    }

    fun remove(city: City) {
        db.remove(city.toFBCity())
    }
    fun addCity(name: String) {
        service.getLocation(name) { lat, lng ->
            if (lat != null && lng != null) {
                db.add(City(name=name, location=LatLng(lat, lng)).toFBCity())
            }
        }
    }
    fun addCity(location: LatLng) {
        service.getName(location.latitude, location.longitude) { name ->
            if (name != null) {
                db.add(City(name = name, location = location).toFBCity())
            }
        }
    }

    override fun onUserLoaded(user: FBUser) {
        _user.value = user.toUser()
    }
    override fun onUserSignOut() {
        //TODO("Not yet implemented")
    }
    override fun onCityAdded(city: FBCity) {
        _cities[city.name!!] = city.toCity()
    }
    override fun onCityUpdated(city: FBCity) {
        _cities.remove(city.name)
        _cities[city.name!!] = city.toCity()
    }
    override fun onCityRemoved(city: FBCity) {
        _cities.remove(city.name)
    }

    private fun loadWeather(name: String) {
        service.getWeather(name) { apiWeather ->
            apiWeather?.let {
                _weather[name] = apiWeather.toWeather()
                loadBitmap(name)
            }
        }
    }

    private fun loadBitmap(name: String) {
        _weather[name]?.let { weather ->
            service.getBitmap(weather.imgUrl) { bitmap ->
                _weather[name] = weather.copy(bitmap = bitmap)
            }
        }
    }


    fun weather (name: String) = _weather.getOrPut(name) {
        loadWeather(name)
        Weather.LOADING // retorno
    }

}
class MainViewModelFactory(private val db : FBDatabase, private val service : WeatherService) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(db, service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun getCities() = List(20) { i ->
    City(name = "Cidade $i")
}
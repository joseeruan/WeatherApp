package com.example.weatherap.model

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherap.api.WeatherService
import com.example.weatherap.api.toForecast
import com.example.weatherap.api.toWeather
import com.example.weatherap.monitor.ForecastMonitor
import com.example.weatherap.repo.Repository
import com.example.weatherap.ui.nav.Route
import com.google.android.gms.maps.model.LatLng

class MainViewModel (private val repo: Repository, private val service : WeatherService, private val monitor : ForecastMonitor): ViewModel(),
    Repository.Listener {
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
        repo.setListener(this)
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

    fun update(city: City) {
        repo.update(city)
    }
    fun remove(city: City) {
        repo.remove(city)
    }
    fun addCity(name: String) {
        service.getLocation(name) { lat, lng ->
            if (lat != null && lng != null) {
                repo.add(City(name=name, location=LatLng(lat, lng)))
            }
        }
    }
    fun addCity(location: LatLng) {
        service.getName(location.latitude, location.longitude) { name ->
            if (name != null) {
                repo.add(City(name = name, location = location))
            }
        }
    }

    override fun onUserLoaded(user: User) {
        _user.value = user
    }
    override fun onUserSignOut() {
        monitor.cancelAll();
    }
    override fun onCityAdded(city: City) {
        _cities[city.name] = city
        monitor.updateCity(city);
    }
    override fun onCityUpdated(city: City) {
        _cities.remove(city.name)
        _cities[city.name] = city
        monitor.updateCity(city)
    }
    override fun onCityRemoved(city: City) {
        _cities.remove(city.name)
        monitor.cancelCity(city)
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
class MainViewModelFactory(private val repo : Repository, private val service : WeatherService, private val monitor: ForecastMonitor) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repo, service, monitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

private fun getCities() = List(20) { i ->
    City(name = "Cidade $i")
}

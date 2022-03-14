package com.example.foody

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.foody.data.Repository
import com.example.foody.models.FoodRecipe
import com.example.foody.util.NetworkResult
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception

class MainViewModel @ViewModelInject constructor( private val repository: Repository, application: Application):AndroidViewModel(application) {

    val recipeResponse: MutableLiveData<NetworkResult<FoodRecipe>> = MutableLiveData()

    fun getRecipes(queries: Map<String,String>) =viewModelScope.launch {
        getSafeRecipeCall(queries)
    }

    private suspend fun getSafeRecipeCall(queries: Map<String, String>) {
        recipeResponse.value=NetworkResult.Loading()
        if(hasInternetConnection()){
            try {
                val response=repository.remote.getRecipes(queries)
                recipeResponse.value=handleRecipeResponse(response)
            }
            catch (e: Exception){
                recipeResponse.value=NetworkResult.Error("No Recipe found")
            }

        }
        else{
            recipeResponse.value=NetworkResult.Error("No Internet Connection.")
        }
    }

    private fun handleRecipeResponse(response: Response<FoodRecipe>): NetworkResult<FoodRecipe>? {
        when{
            response.message().toString().contains("timeout") -> return NetworkResult.Error("TimeOut")

            response.code()==402 -> return NetworkResult.Error("API Key Limit")

            response.body()!!.results.isNullOrEmpty() -> return NetworkResult.Error("Data is Null")

            response.isSuccessful -> {
                val foodRecipe=response.body()
                NetworkResult.Success(foodRecipe!!)
            }

            else -> {
                return NetworkResult.Error(response.message())
            }

        }
    }

    private fun hasInternetConnection():Boolean{
        val connectivityManager=getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork=connectivityManager.activeNetwork ?: return false
        val capabilities=connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)-> true
            else -> false
        }
    }

}
package org.musicpimp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.musicpimp.*
import org.musicpimp.ui.Outcome
import timber.log.Timber

class PrivacyPolicyViewModel(app: Application)  : AndroidViewModel(app)  {
    private val policyUrl = FullUrl("https", "www.musicpimp.org", "/legal/privacy.json")

    private val comps = (app as PimpApp).components
    private val policyData = MutableLiveData<Outcome<PrivacyPolicy>>()
    val policy: LiveData<Outcome<PrivacyPolicy>> = policyData

    fun load() {
        val http = comps.http
        viewModelScope.launch {
            policyData.value = Outcome.loading()
            try {
                Timber.i("Loading privacy policy from '$policyUrl'...")
                val response = http.getJson(policyUrl, PrivacyPolicy.json)
                policyData.value = Outcome.success(response)
                Timber.i("Loaded privacy policy from '$policyUrl'.")
            } catch (e: Exception) {
                val msg ="Failed to load privacy policy from '$policyUrl'."
                Timber.e(e, msg)
                policyData.value = Outcome.error(SingleError.backend(msg))
            }
        }
    }
}

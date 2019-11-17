package org.musicpimp.ui.settings

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.edit_endpoint_fragment.*
import kotlinx.android.synthetic.main.edit_endpoint_fragment.view.*
import org.musicpimp.*
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.CloudEndpointInput
import org.musicpimp.endpoints.Endpoint
import timber.log.Timber

class EditEndpointFragment : Fragment() {
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.edit_endpoint_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel =
            activity?.run { ViewModelProviders.of(this).get(SettingsViewModel::class.java) }!!
        Timber.i("Editing ${viewModel.editedEndpoint?.id ?: "new endpoint"}")
        view.save_button.setOnClickListener {
            val name = cloud_id_edit_text.text.string()
            val user = username_edit_text.text.string()
            val pass = password_edit_text.text.string()
            val creds = CloudCredential(CloudId(name), Username(user), Password(pass))
            val id = viewModel.editedEndpoint?.id ?: EndpointId.random()
            val input = CloudEndpointInput(NonEmptyString(name), creds).withId(id)
            viewModel.save(input)
            activity?.onBackPressed()
        }
        view.delete_button.visibility =
            if (viewModel.editedEndpoint == null) View.GONE else View.VISIBLE
        view.delete_button.setOnClickListener {
            viewModel.removeIfExists()
            activity?.onBackPressed()
        }
        viewModel.editedEndpoint?.let { fill(it, view) }
    }

    private fun fill(e: Endpoint, on: View) {
        if (e is CloudEndpoint) {
            val creds = e.creds
            on.findViewById<TextInputEditText>(R.id.cloud_id_edit_text).setText(creds.server.value)
            on.findViewById<TextInputEditText>(R.id.username_edit_text)
                .setText(creds.username.value)
            on.findViewById<TextInputEditText>(R.id.password_edit_text)
                .setText(creds.password.value)
        }
    }
}

fun Editable?.string(): String {
    return this?.toString() ?: ""
}

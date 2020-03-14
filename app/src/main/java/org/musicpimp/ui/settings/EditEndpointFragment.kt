package org.musicpimp.ui.settings

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.edit_endpoint_fragment.*
import kotlinx.android.synthetic.main.edit_endpoint_fragment.view.*
import org.musicpimp.*
import org.musicpimp.endpoints.CloudEndpoint
import org.musicpimp.endpoints.CloudEndpointInput
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.ui.ResourceFragment
import timber.log.Timber

class EditEndpointFragment : ResourceFragment(R.layout.edit_endpoint_fragment) {
    private val viewModel: SettingsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            on.cloud_id_edit_text.setText(creds.server.value)
            on.username_edit_text.setText(creds.username.value)
            on.password_edit_text.setText(creds.password.value)
        }
    }
}

fun Editable?.string(): String {
    return this?.toString() ?: ""
}

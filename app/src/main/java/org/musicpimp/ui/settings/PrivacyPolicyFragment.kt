package org.musicpimp.ui.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.observe
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_privacy.view.*
import org.musicpimp.R
import org.musicpimp.ui.ResourceFragment
import org.musicpimp.ui.Status

class PrivacyPolicyFragment : ResourceFragment(R.layout.fragment_privacy) {
    protected val viewModel: PrivacyPolicyViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.policy.observe(viewLifecycleOwner) { outcome ->
            when (outcome.status) {
                Status.Success ->
                    outcome.data?.let { policy ->
                        view.privacy_text.text = policy.paragraphs.joinToString("\n\n")
                    }
                Status.Error -> {
                }
                Status.Loading -> {
                }
            }
        }
        viewModel.load()
    }
}

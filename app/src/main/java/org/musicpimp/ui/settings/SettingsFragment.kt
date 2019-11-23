package org.musicpimp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.settings_fragment.view.*
import org.musicpimp.MainActivityViewModel
import org.musicpimp.R
import org.musicpimp.endpoints.Endpoint
import timber.log.Timber

class SettingsFragment : Fragment() {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewModel =
            activity?.run { ViewModelProviders.of(this).get(SettingsViewModel::class.java) }!!
        val playbackAdapter = DropdownAdapter(requireContext(), mutableListOf())
        val sourceAdapter = DropdownAdapter(requireContext(), mutableListOf())
        view.playback_device_dropdown.setAdapter(playbackAdapter)
        view.music_source_dropdown.setAdapter(sourceAdapter)
        viewModel.endpoints.observe(viewLifecycleOwner) { es ->
            playbackAdapter.update(es)
            sourceAdapter.update(es)
        }
        viewModel.playbackDevice.observe(viewLifecycleOwner) {
            Timber.i("Playback device changed to ${it.name}")
            mainViewModel.activatePlayer(it)
            // Bug if two endpoints have the same name. Perhaps forbid that.
            view.playback_device_dropdown.setText(it.name.value, false)
        }
        viewModel.musicSource.observe(viewLifecycleOwner) {
            mainViewModel.activateSource(it)
            Timber.i("Music source changed to ${it.name}.")
            view.music_source_dropdown.setText(it.name.value, false)
        }
        view.playback_device_dropdown.setOnItemClickListener { parent, v, position, id ->
            playbackAdapter.getItem(position)?.let {
                viewModel.onPlayback(it)
            }
        }
        view.music_source_dropdown.setOnItemClickListener { parent, v, position, id ->
            sourceAdapter.getItem(position)?.let {
                viewModel.onSource(it)
            }
        }
        view.manage_endpoints_button.setOnClickListener {
            val action = SettingsFragmentDirections.settingsToEndpoints()
            findNavController().navigate(action)
        }
    }
}

class DropdownAdapter(context: Context, val endpoints: MutableList<Endpoint>) :
    ArrayAdapter<Endpoint>(context, 0, endpoints) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val target = (convertView ?: LayoutInflater.from(context).inflate(
            R.layout.endpoint_dropdown_item,
            parent,
            false
        )) as TextView
        getItem(position)?.let { endpoint ->
            target.text = endpoint.name.value
            // Apparently interferes with framework functionality if set
//            target.setOnClickListener { ... }
        }
        return target
    }

    fun update(es: List<Endpoint>) {
        clear()
        addAll(es)
        notifyDataSetChanged()
    }
}

interface EndpointsDelegate {
    fun onEndpoint(e: Endpoint)
}

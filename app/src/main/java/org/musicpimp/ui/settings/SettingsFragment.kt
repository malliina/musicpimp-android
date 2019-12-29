package org.musicpimp.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_settings.view.*
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
        return inflater.inflate(R.layout.fragment_settings, container, false)
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
            view.playback_device_dropdown.clearFocus()
            playbackAdapter.getItem(position)?.let {
                viewModel.onPlayback(it)
            }
        }
        view.music_source_dropdown.setOnItemClickListener { parent, v, position, id ->
            view.music_source_dropdown.clearFocus()
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

class NonFilter : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        return FilterResults()
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
    }
}

class DropdownAdapter(val context: Context, val endpoints: MutableList<Endpoint>) :
    BaseAdapter(), Filterable {

    private val nonFilter = NonFilter()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val target = (convertView ?: LayoutInflater.from(context).inflate(
            R.layout.endpoint_dropdown_item,
            parent,
            false
        )) as TextView
        val endpoint = endpoints[position]
        target.text = endpoint.name.value
        // Apparently interferes with framework functionality if set
//            target.setOnClickListener { ... }
        return target
    }

    fun update(es: List<Endpoint>) {
        endpoints.clear()
        endpoints.addAll(es)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = nonFilter
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getCount(): Int = endpoints.size
    override fun getItem(position: Int): Endpoint? =
        if (endpoints.size > position) endpoints[position] else null
}

interface EndpointsDelegate {
    fun onEndpoint(e: Endpoint)
}

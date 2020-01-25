package org.musicpimp.ui.settings

import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.endpoint_item.view.*
import kotlinx.android.synthetic.main.endpoints_fragment.view.*
import org.musicpimp.MainActivityViewModel
import org.musicpimp.R
import org.musicpimp.endpoints.Endpoint
import org.musicpimp.endpoints.LocalEndpoint
import org.musicpimp.ui.init

class EndpointsFragment : Fragment(), EndpointsDelegate {
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var viewModel: SettingsViewModel
    private lateinit var viewAdapter: EndpointsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.endpoints_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel =
            activity?.run { ViewModelProviders.of(this).get(MainActivityViewModel::class.java) }!!
        viewModel =
            activity?.run { ViewModelProviders.of(this).get(SettingsViewModel::class.java) }!!
        viewManager = LinearLayoutManager(context)
        viewAdapter = EndpointsAdapter(emptyList(), this)
        view.endpoints_list.init(viewManager, viewAdapter)
        viewModel.endpoints.observe(viewLifecycleOwner) { es ->
            viewAdapter.endpoints = es
            viewAdapter.notifyDataSetChanged()
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_top_nav_menu, menu)
    }

    override fun onEndpoint(e: Endpoint) {
        if (e.id != LocalEndpoint.local.id) {
            viewModel.onEndpoint(e)
            val action = EndpointsFragmentDirections.endpointsToEndpoint(e.id, getString(R.string.title_edit_endpoint))
            findNavController().navigate(action)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.endpoint_edit -> {
                viewModel.editedEndpoint = null
                val action = EndpointsFragmentDirections.endpointsToEndpoint(
                    null,
                    getString(R.string.title_add_endpoint)
                )
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class EndpointsAdapter(var endpoints: List<Endpoint>, private val delegate: EndpointsDelegate) :
    RecyclerView.Adapter<EndpointsAdapter.EndpointHolder>() {
    class EndpointHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EndpointHolder {
        val layout = LayoutInflater.from(parent.context).inflate(
            R.layout.endpoint_item,
            parent,
            false
        ) as ConstraintLayout
        return EndpointHolder(layout)
    }

    override fun onBindViewHolder(th: EndpointHolder, position: Int) {
        val layout = th.layout
        val endpoint = endpoints[position]
        layout.endpoint_name.text = endpoint.name.value
        layout.setOnClickListener {
            delegate.onEndpoint(endpoint)
        }
    }

    override fun getItemCount(): Int = endpoints.size
}

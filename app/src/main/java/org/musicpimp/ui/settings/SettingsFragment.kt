package org.musicpimp.ui.settings

import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.endpoint_item.view.*
import org.musicpimp.R
import org.musicpimp.endpoints.Endpoint
import timber.log.Timber

class SettingsFragment : Fragment(), EndpointsDelegate {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var viewAdapter: EndpointsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.settings_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel =
            activity?.run { ViewModelProviders.of(this).get(SettingsViewModel::class.java) }!!
        viewManager = LinearLayoutManager(context)
        viewAdapter = EndpointsAdapter(emptyList(), this)
        view.findViewById<RecyclerView>(R.id.endpoints_list).apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        viewModel.endpoints.observe(viewLifecycleOwner) {
            viewAdapter.endpoints = it
            viewAdapter.notifyDataSetChanged()
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_top_nav_menu, menu)
    }

    override fun onEndpoint(e: Endpoint) {
        viewModel.onEndpoint(e)
        Timber.i("Selected ${e.id}")
        findNavController().navigate(R.id.endpoint_edit)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.endpoint_edit -> {
                viewModel.editedEndpoint = null
                findNavController().navigate(R.id.endpoint_edit)
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

interface EndpointsDelegate {
    fun onEndpoint(e: Endpoint)
}

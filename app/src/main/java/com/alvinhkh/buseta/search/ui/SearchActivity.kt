package com.alvinhkh.buseta.search.ui


import android.app.SearchManager
import androidx.lifecycle.ViewModelProvider
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.model.KmbAppIntentData
import com.alvinhkh.buseta.mtr.ui.MtrBusActivity
import com.alvinhkh.buseta.kmb.ui.KmbActivity
import com.alvinhkh.buseta.lwb.ui.LwbActivity
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.mtr.ui.MtrStationActivity
import com.alvinhkh.buseta.nlb.ui.NlbActivity
import com.alvinhkh.buseta.nwst.ui.NwstActivity
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.ui.RouteListViewModel
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.ui.PinnedHeaderItemDecoration
import com.alvinhkh.buseta.utils.ColorUtil
import com.alvinhkh.buseta.utils.PreferenceUtil
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*

import java.util.regex.Pattern


class SearchActivity : AppCompatActivity() {

    private lateinit var suggestionDatabase: SuggestionDatabase

    private lateinit var viewModel: RouteListViewModel
    private lateinit var viewAdapter: SearchViewAdapter

    private var isOpened = false
    private var searchQueryText = ""
    private var checkedProviders = arrayListOf<String>()
    private var queryLiveData: LiveData<MutableList<Route>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val color = ContextCompat.getColor(this, R.color.background)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            window?.statusBarColor = color
            window?.navigationBarColor = ContextCompat.getColor(this, R.color.transparent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.statusBarColor = ColorUtil.darkenColor(color)
            window?.navigationBarColor = ColorUtil.darkenColor(color)
        }

        suggestionDatabase = SuggestionDatabase.getInstance(this)!!
        setContentView(R.layout.activity_search)

        val listener = object: SearchViewAdapter.OnItemClickListener {
            override fun onClick(data: SearchViewAdapter.Data) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClass(applicationContext, SearchActivity::class.java)
                if (data.type == SearchViewAdapter.Data.TYPE_SUGGESTION) {
                    val suggestion = data.obj as Suggestion
                    intent.putExtra(C.EXTRA.ROUTE_NO, suggestion.route)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, suggestion.companyCode)
                }
                if (data.type == SearchViewAdapter.Data.TYPE_ROUTE) {
                    val route = data.obj as Route
                    intent.putExtra(C.EXTRA.ROUTE_NO, route.name)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, route.companyCode)
                }
                startActivity(intent)
                finish()
            }

            override fun onLongClick(data: SearchViewAdapter.Data) {
            }
        }

        with(provider_group) {
            val companyCodes = arrayListOf(C.PROVIDER.KMB, C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NLB, C.PROVIDER.LRTFEEDER)
            val chipListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                val companyCode = buttonView.tag as String
                if (companyCode.isEmpty()) return@OnCheckedChangeListener
                if (isChecked && !checkedProviders.contains(companyCode)) {
                    checkedProviders.add(companyCode)
                } else if (!isChecked && checkedProviders.contains(companyCode)) {
                    checkedProviders.remove(companyCode)
                }
                loadSearchResult(searchQueryText, checkedProviders, false)
            }
            for(companyCode in companyCodes) {
                val companyName = Route.companyName(context, companyCode, "")
                val chip = Chip(context, null, R.style.Widget_MaterialComponents_Chip_Filter)
                chip.tag = companyCode
                chip.text = companyName
                chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_outline_directions_bus_24dp)
                chip.setChipIconTintResource(R.color.icons)
                chip.isCloseIconVisible = false
                chip.isClickable = true
                chip.isCheckable = true
                chip.isChecked = false
                chip.setOnCheckedChangeListener(chipListener)
                this.addView(chip)
            }
        }

        viewModel = ViewModelProvider(this).get(RouteListViewModel::class.java)
        with(recycler_view) {
            addItemDecoration(PinnedHeaderItemDecoration())
            layoutManager = LinearLayoutManager(context)
            viewAdapter = SearchViewAdapter(listener)
            adapter = viewAdapter
        }

        val newIntent = handleIntent(intent)
        if (!newIntent.hasExtra(SearchManager.QUERY)) {
            try {
                startActivity(newIntent)
                finish()
                return
            } catch (ignored: ActivityNotFoundException) { }
        } else {
            intent.putExtra(SearchManager.QUERY, newIntent.getStringExtra(SearchManager.QUERY))
        }

        val query = intent.getStringExtra(SearchManager.QUERY)?.toUpperCase(Locale.ENGLISH)
        if (query == C.PREF.AD_KEY || query == C.PREF.AD_SHOW) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val hidden = preferences.getBoolean(C.PREF.AD_HIDE, false)
            val editor = preferences.edit()
            editor.putBoolean(C.PREF.AD_HIDE, query == C.PREF.AD_KEY)
            editor.apply()
            var stringId = R.string.message_request_hide_ad
            if (query == C.PREF.AD_SHOW)
                stringId = R.string.message_request_show_ad
            if (hidden && query == C.PREF.AD_KEY)
                stringId = R.string.message_request_hide_ad_again
            Toast.makeText(applicationContext, stringId, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadSearchResult(query?:"", checkedProviders, true)
        with(search_et) {
            val action = intent.action
            if (Intent.ACTION_SEARCH == action && !query.isNullOrEmpty()) {
                text.clear()
                text.insert(0, query)
            }
            setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val t = text?.replace("[^a-zA-Z0-9]*".toRegex(), "")?.toUpperCase(Locale.ENGLISH)
                    loadSearchResult(t?:"", checkedProviders, true)
                    return@OnEditorActionListener true
                }
                false
            })
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    val t = p0?.replace("[^a-zA-Z0-9]*".toRegex(), "")?.toUpperCase(Locale.ENGLISH)
                    loadSearchResult("$t%", checkedProviders, false)
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            startActivity(handleIntent(intent))
            finish()
            return
        } catch (ignored: ActivityNotFoundException) {}
        if (intent.action == Intent.ACTION_SEARCH) {
            loadSearchResult(intent.getStringExtra(SearchManager.QUERY)?:"", emptyList(), true)
        }
    }

    private fun loadSearchResult(route: String, companyCodes: List<String>, singleWillOpen: Boolean) {
        queryLiveData?.removeObservers(this@SearchActivity)
        searchQueryText = route
        queryLiveData = viewModel.liveData(route, companyCodes)
        queryLiveData?.observe(this@SearchActivity, { list ->
            viewAdapter.clear()
            val providerSearchList = arrayListOf<String>()
            listOf(C.PROVIDER.CTB, C.PROVIDER.KMB, C.PROVIDER.LWB, C.PROVIDER.NLB, C.PROVIDER.NWFB).forEach {
                if (checkedProviders.size == 0 || checkedProviders.contains(it)) {
                    providerSearchList.add(it)
                }
            }
            var lastCompanyCode = ""
            var lastRouteNo = ""
            val routeNo = route.replace(Regex("[^a-zA-Z0-9 ]"), "")
            val shownCompanyCode = arrayListOf<String>()
            if (list?.size?:0 > 0) {
                if (list?.size?:0 == 1 && !list?.get(0)?.companyCode.isNullOrEmpty() && singleWillOpen && !isOpened) {
                    isOpened = true
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.ROUTE_NO, routeNo)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, list?.get(0)?.companyCode?:"")
                    startActivity(intent)
                    finish()
                } else {
                    list?.forEach { route ->
                        val companyCode = route.companyCode?:""
                        if (lastCompanyCode != companyCode) {
                            if (lastCompanyCode.isNotBlank() && lastRouteNo.isNotBlank()
                                    && routeNo.isNotBlank() && providerSearchList.contains(lastCompanyCode)) {
                                viewAdapter.addButton(Suggestion(0, lastCompanyCode, lastRouteNo, 0, Suggestion.TYPE_DEFAULT))
                            }
                            val companyName = Route.companyName(applicationContext, companyCode, route.name)
                            viewAdapter.addSection(companyName)
                            if (!shownCompanyCode.contains(companyCode)) {
                                shownCompanyCode.add(companyCode)
                            }
                        }
                        if (lastRouteNo != route.name?:"" || lastCompanyCode != companyCode) {
                            viewAdapter.add(route)
                        }
                        lastRouteNo = route.name?:""
                        lastCompanyCode = companyCode
                    }
                    if (lastCompanyCode.isNotBlank() && lastRouteNo.isNotBlank()
                            && routeNo.isNotBlank() && providerSearchList.contains(lastCompanyCode)) {
                        viewAdapter.addButton(Suggestion(0, lastCompanyCode, lastRouteNo, 0, Suggestion.TYPE_DEFAULT))
                    }
                }
            }
            if (routeNo.isNotBlank()) {
                providerSearchList.forEach {
                    if (!shownCompanyCode.contains(it)) {
                        val companyName = Route.companyName(applicationContext, it, route.replace(Regex("[^a-zA-Z0-9]"), ""))
                        viewAdapter.addSection(companyName)
                        viewAdapter.addSection(getString(R.string.no_search_result))
                        viewAdapter.addButton(Suggestion(0, it, route.replace(Regex("[^a-zA-Z0-9]"), ""), 0, Suggestion.TYPE_DEFAULT))
                    }
                }
            }
        })
    }

    private fun providerIntent(companyCode: String): Intent {
        var intent: Intent
        when (companyCode) {
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> intent = Intent(applicationContext, NwstActivity::class.java)
            C.PROVIDER.LRTFEEDER -> intent = Intent(applicationContext, MtrBusActivity::class.java)
            C.PROVIDER.MTR -> intent = Intent(applicationContext, MtrStationActivity::class.java)
            C.PROVIDER.NLB, C.PROVIDER.GMB901 -> intent = Intent(applicationContext, NlbActivity::class.java)
            C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                intent = Intent(applicationContext, LwbActivity::class.java)
                if (PreferenceUtil.isUsingKmbWebApi(applicationContext)) {
                    intent = Intent(applicationContext, KmbActivity::class.java)
                }
            }
            else -> {
                intent = Intent(applicationContext, LwbActivity::class.java)
                if (PreferenceUtil.isUsingKmbWebApi(applicationContext)) {
                    intent = Intent(applicationContext, KmbActivity::class.java)
                }
            }
        }
        intent.putExtra(C.EXTRA.COMPANY_CODE, companyCode)
        return intent
    }

    private fun handleIntent(intent: Intent): Intent {
        val action = intent.action
        val data = intent.dataString

        if (Intent.ACTION_SEARCH == action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!query.isNullOrEmpty()) {
                val company = intent.getStringExtra(C.EXTRA.COMPANY_CODE)
                if (!company.isNullOrEmpty()) {
                    val i = providerIntent(company)
                    i.putExtra(C.EXTRA.ROUTE_NO, query)
                    return i
                }
            }
        }

        if (Intent.ACTION_VIEW == action) {
            var routeStop: RouteStop? = intent.getParcelableExtra(C.EXTRA.STOP_OBJECT)
            val stopText = intent.getStringExtra(C.EXTRA.STOP_OBJECT_STRING)
            val company = intent.getStringExtra(C.EXTRA.COMPANY_CODE)
            val routeNo = intent.getStringExtra(C.EXTRA.ROUTE_NO)
            if (routeStop == null && !stopText.isNullOrEmpty()) {
                routeStop = Gson().fromJson(stopText, RouteStop::class.java)
            }
            if (routeStop != null) {
                val i = providerIntent(routeStop.companyCode!!)
                i.putExtra(C.EXTRA.ROUTE_NO, routeStop.routeNo)
                i.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                return i
            } else if (!routeNo.isNullOrEmpty() && !company.isNullOrEmpty()) {
                val i = providerIntent(company)
                i.putExtra(C.EXTRA.ROUTE_NO, routeNo)
                return i
            } else if (intent.data?.scheme == "app1933") {
                if (!data.isNullOrEmpty()) {
                    val decodedStr = String(Base64.decode(intent.data?.pathSegments?.get(0), Base64.DEFAULT))
                    val gson = Gson()
                    val intentData = gson.fromJson(decodedStr, KmbAppIntentData::class.java)
                    if (intentData != null) {
                        val i = providerIntent(C.PROVIDER.KMB)
                        i.putExtra(C.EXTRA.ROUTE_NO, intentData.route)
                        i.putExtra(C.EXTRA.ROUTE_SEQUENCE, intentData.bound)
                        i.putExtra(C.EXTRA.ROUTE_SERVICE_TYPE, intentData.serviceType)
                        i.putExtra(C.EXTRA.STOP_ID, intentData.stopCode)
                        val stopObject = RouteStop()
                        stopObject.companyCode = C.PROVIDER.KMB
                        stopObject.routeNo = intentData.route
                        stopObject.routeSequence = intentData.bound
                        stopObject.routeServiceType = intentData.serviceType
                        stopObject.stopId = intentData.stopCode
                        i.putExtra(C.EXTRA.STOP_OBJECT, stopObject)
                        return i
                    }
                }
                if (intent.resolveActivity(packageManager) != null) {
                    return intent
                }
            } else if (intent.data?.host == "search.kmb.hk") {
                if (intent.data?.getQueryParameter("action") == "routesearch") {
                    val i = providerIntent(C.PROVIDER.KMB)
                    i.putExtra(C.EXTRA.ROUTE_NO, intent.data?.getQueryParameter("route"))
                    return i
                }
                // let browser handle unknown urls
                val uri = Uri.parse("googlechrome://navigate?url=${intent.dataString}")
                val i = Intent(Intent.ACTION_VIEW, uri)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return i
            } else if (intent.data?.host == "mobile.nwstbus.com.hk") {
                if (intent.data?.getQueryParameter("action") == "ETA") {
                    val companyCode = intent.data?.getQueryParameter("compcode")?:C.PROVIDER.NWST
                    val serviceNo = intent.data?.getQueryParameter("serviceno")?:""
                    val stopId = intent.data?.getQueryParameter("stopid")?:""
                    if (serviceNo.isNotEmpty()) {
                        val i = providerIntent(companyCode)
                        i.putExtra(C.EXTRA.ROUTE_NO, serviceNo)
                        i.putExtra(C.EXTRA.STOP_ID, stopId)
                        return i
                    }
                }
                if (!intent.data?.getQueryParameter("ds").isNullOrEmpty()) {
                    val i = Intent(Intent.ACTION_SEARCH)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.setClass(this, SearchActivity::class.java)
                    i.putExtra(SearchManager.QUERY, intent.data?.getQueryParameter("ds")?:"")
                    return i
                }
                if (intent.data?.host?.startsWith("http") == true) {
                    // let browser handle unknown urls
                    val uri = Uri.parse("googlechrome://navigate?url=${intent.dataString}")
                    val i = Intent(Intent.ACTION_VIEW, uri)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return i
                }
            } else if (!data.isNullOrEmpty()) {
                val lastQuery: String?
                val regex = "/route/(.*)/?"
                val regexPattern = Pattern.compile(regex)
                val match = regexPattern.matcher(data)
                lastQuery = if (match.find()) {
                    match.group(1)
                } else {
                    data.substring(data.lastIndexOf("/") + 1)
                }
                val i = providerIntent("")
                i.putExtra(C.EXTRA.ROUTE_NO, lastQuery)
                return i
            }
        }
        return Intent()
    }
}

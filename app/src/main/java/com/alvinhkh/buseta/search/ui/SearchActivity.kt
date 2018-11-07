package com.alvinhkh.buseta.search.ui


import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.ui.MtrBusActivity
import com.alvinhkh.buseta.kmb.ui.KmbActivity
import com.alvinhkh.buseta.lwb.ui.LwbActivity
import com.alvinhkh.buseta.model.RouteStop
import com.alvinhkh.buseta.mtr.ui.AESBusActivity
import com.alvinhkh.buseta.mtr.ui.MtrActivity
import com.alvinhkh.buseta.nlb.ui.NlbActivity
import com.alvinhkh.buseta.nwst.ui.NwstActivity
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.ui.PinnedHeaderItemDecoration
import com.alvinhkh.buseta.utils.PreferenceUtil
import com.alvinhkh.buseta.utils.RouteUtil
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_suggestion.*

import java.util.regex.Pattern


class SearchActivity : AppCompatActivity() {

    private lateinit var suggestionDatabase: SuggestionDatabase

    private lateinit var viewModel: SuggestionViewModel
    private lateinit var viewAdapter: SuggestionViewAdapter

    private var isOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            startActivity(handleIntent(intent))
            finish()
            return
        } catch (ignored: ActivityNotFoundException) { }

        suggestionDatabase = SuggestionDatabase.getInstance(this)!!
        setContentView(R.layout.activity_suggestion)

        val listener = object: SuggestionViewAdapter.OnItemClickListener {
            override fun onClick(suggestion: Suggestion?) {
                if (suggestion == null) return
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClass(applicationContext, SearchActivity::class.java)
                intent.putExtra(C.EXTRA.ROUTE_NO, suggestion.route)
                intent.putExtra(C.EXTRA.COMPANY_CODE, suggestion.companyCode)
                startActivity(intent)
                finish()
            }

            override fun onLongClick(suggestion: Suggestion?) {
            }
        }
        val query = intent.getStringExtra(SearchManager.QUERY)
        with(recycler_view) {
            addItemDecoration(PinnedHeaderItemDecoration())
            layoutManager = LinearLayoutManager(context)
            viewAdapter = SuggestionViewAdapter(context, null, listener)
            adapter = viewAdapter
            viewModel = ViewModelProviders.of(this@SearchActivity).get(SuggestionViewModel::class.java)
            loadSearchResult(query?:"", true)
        }
        with(search_et) {
            val action = intent.action
            if (Intent.ACTION_SEARCH == action) {
                text.clear()
                text.insert(0, query)
            }
            setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val t = text?.replace("[^a-zA-Z0-9]*".toRegex(), "")?.toUpperCase()
                    loadSearchResult(t?:"", true)
                    return@OnEditorActionListener true
                }
                false
            })
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    val t = p0?.replace("[^a-zA-Z0-9]*".toRegex(), "")?.toUpperCase()
                    loadSearchResult("$t%", false)
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
        startActivity(handleIntent(intent))
        finish()
    }

    private fun loadSearchResult(route: String, singleWillOpen: Boolean) {
        var lastCompanyCode = ""
        viewModel.getAsLiveData(route).observe(this@SearchActivity, Observer {
            viewAdapter.clear()
            val routeNo = route.replace(Regex("[^a-zA-Z0-9 ]"), "")
            val shownCompanyCode = arrayListOf<String>()
            if (it?.size?:0 > 0) {
                if (it?.size?:0 == 1 && !it?.get(0)?.companyCode.isNullOrEmpty() && singleWillOpen && !isOpened) {
                    isOpened = true
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.ROUTE_NO, routeNo)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, it?.get(0)?.companyCode?:"")
                    startActivity(intent)
                    finish()
                } else {
                    it?.forEach {
                        if (lastCompanyCode != it.companyCode) {
                            if (lastCompanyCode.isNotBlank() && routeNo.isNotBlank()) {
                                viewAdapter.addButton(Suggestion(0, lastCompanyCode, routeNo, 0, Suggestion.TYPE_DEFAULT))
                            }
                            val companyName = RouteUtil.getCompanyName(applicationContext, it.companyCode, it.route)
                            viewAdapter.addSection(companyName)
                            shownCompanyCode.add(it.companyCode)
                        }
                        viewAdapter.addItem(it)
                        lastCompanyCode = it.companyCode
                    }
                }
            }
            if (routeNo.isNotBlank()) {
                listOf(C.PROVIDER.CTB, C.PROVIDER.KMB, C.PROVIDER.NLB, C.PROVIDER.NWFB).forEach {
                    if (!shownCompanyCode.contains(it)) {
                        val companyName = RouteUtil.getCompanyName(applicationContext, it, route.replace(Regex("[^a-zA-Z0-9]"), ""))
                        viewAdapter.addSection(companyName)
                        viewAdapter.addSection(getString(R.string.no_search_result))
                        viewAdapter.addButton(Suggestion(0, it, route.replace(Regex("[^a-zA-Z0-9]"), ""), 0, Suggestion.TYPE_DEFAULT))
                    }
                }
            }
        })
    }

    private fun getBusIntent(companyCode: String): Intent {
        var code = companyCode
        var intent: Intent
        if (TextUtils.isEmpty(code)) {
            code = C.PROVIDER.KMB
        }
        when (code) {
            C.PROVIDER.AESBUS -> intent = Intent(applicationContext, AESBusActivity::class.java)
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> intent = Intent(applicationContext, NwstActivity::class.java)
            C.PROVIDER.LRTFEEDER -> intent = Intent(applicationContext, MtrBusActivity::class.java)
            C.PROVIDER.NLB -> intent = Intent(applicationContext, NlbActivity::class.java)
            C.PROVIDER.KMB -> {
                intent = Intent(applicationContext, LwbActivity::class.java)
                if (PreferenceUtil.isUsingNewKmbApi(applicationContext)) {
                    intent = Intent(applicationContext, KmbActivity::class.java)
                }
            }
            else -> {
                intent = Intent(applicationContext, LwbActivity::class.java)
                if (PreferenceUtil.isUsingNewKmbApi(applicationContext)) {
                    intent = Intent(applicationContext, KmbActivity::class.java)
                }
            }
        }
        return intent
    }

    private fun handleIntent(intent: Intent): Intent {
        val action = intent.action
        val data = intent.dataString
        val type = intent.getStringExtra(C.EXTRA.TYPE)

        if (Intent.ACTION_SEARCH == action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!TextUtils.isEmpty(type) && type == C.TYPE.RAILWAY) {
                val lineCode = intent.getStringExtra(C.EXTRA.LINE_CODE)
                val i = Intent(applicationContext, MtrActivity::class.java)
                i.putExtra(C.EXTRA.LINE_CODE, if (TextUtils.isEmpty(lineCode)) query else lineCode)
                return i
            } else if (!query.isNullOrEmpty()) {
                val company = intent.getStringExtra(C.EXTRA.COMPANY_CODE)
                if (!company.isNullOrEmpty()) {
                    val i = getBusIntent(company)
                    i.putExtra(C.EXTRA.ROUTE_NO, query)
                    return i
                }
            }
        }

        if (Intent.ACTION_VIEW == action) {
            if (!TextUtils.isEmpty(type) && type == C.TYPE.RAILWAY) {
                val lineCode = intent.getStringExtra(C.EXTRA.LINE_CODE)
                val lineColour = intent.getStringExtra(C.EXTRA.LINE_COLOUR)
                val lineName = intent.getStringExtra(C.EXTRA.LINE_NAME)
                val i = Intent(applicationContext, MtrActivity::class.java)
                i.putExtra(C.EXTRA.LINE_CODE, lineCode)
                i.putExtra(C.EXTRA.LINE_COLOUR, lineColour)
                i.putExtra(C.EXTRA.LINE_NAME, lineName)
                return i
            } else {
                var routeStop: RouteStop? = intent.getParcelableExtra(C.EXTRA.STOP_OBJECT)
                val stopText = intent.getStringExtra(C.EXTRA.STOP_OBJECT_STRING)
                val company = intent.getStringExtra(C.EXTRA.COMPANY_CODE)
                val routeNo = intent.getStringExtra(C.EXTRA.ROUTE_NO)
                if (routeStop == null && !TextUtils.isEmpty(stopText)) {
                    routeStop = Gson().fromJson(stopText, RouteStop::class.java)
                }
                if (routeStop != null) {
                    if (routeStop.companyCode!! == C.PROVIDER.MTR) {
                        val i = Intent(applicationContext, MtrActivity::class.java)
                        i.putExtra(C.EXTRA.LINE_CODE, routeStop.routeId)
                        i.putExtra(C.EXTRA.LINE_NAME, routeStop.routeNo)
                        return i
                    }
                    val i = getBusIntent(routeStop.companyCode!!)
                    i.putExtra(C.EXTRA.ROUTE_NO, routeStop.routeNo)
                    i.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                    return i
                } else if (!TextUtils.isEmpty(routeNo) && !TextUtils.isEmpty(company)) {
                    val i = getBusIntent(company)
                    i.putExtra(C.EXTRA.ROUTE_NO, routeNo)
                    return i
                } else if (!TextUtils.isEmpty(data)) {
                    val lastQuery: String?
                    val regex = "/route/(.*)/?"
                    val regexPattern = Pattern.compile(regex)
                    val match = regexPattern.matcher(data!!)
                    if (match.find()) {
                        lastQuery = match.group(1)
                    } else {
                        lastQuery = data.substring(data.lastIndexOf("/") + 1)
                    }
                    val i = getBusIntent("")
                    i.putExtra(C.EXTRA.ROUTE_NO, lastQuery)
                    return i
                }
            }
        }
        return Intent()
    }
}

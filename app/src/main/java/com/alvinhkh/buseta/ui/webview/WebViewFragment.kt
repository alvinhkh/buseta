package com.alvinhkh.buseta.ui.webview

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

import com.alvinhkh.buseta.R


class WebViewFragment : Fragment() {

    private lateinit var actionBar: ActionBar

    private lateinit var webView: WebView

    private var contentTitle: String? = null

    private var contentHtml: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_webview, container, false)
        contentTitle = arguments?.getString(ARG_TITLE)
        contentHtml = arguments?.getString(ARG_HTML)
        if (activity == null) return view
        actionBar = (activity as AppCompatActivity).supportActionBar!!
        actionBar.title = contentTitle
        actionBar.subtitle = null
        actionBar.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        webView = view.findViewById(R.id.web_view)
        webView.loadData(contentHtml, "text/html; charset=UTF-8", null)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_TITLE, contentTitle)
        outState.putString(ARG_HTML, contentHtml)
    }

    override fun onResume() {
        super.onResume()
        actionBar.title = contentTitle
        actionBar.subtitle = null
    }

    override fun onDestroyView() {
        view?.visibility = View.GONE
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if (id == R.id.action_refresh) {
            webView.loadData(contentHtml, "text/html; charset=UTF-8", null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        const val ARG_TITLE = "content_title"

        const val ARG_HTML = "content_html"

        fun newInstance(title: String, html: String): WebViewFragment {
            val fragment = WebViewFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_HTML, html)
            fragment.arguments = args
            return fragment
        }
    }

}
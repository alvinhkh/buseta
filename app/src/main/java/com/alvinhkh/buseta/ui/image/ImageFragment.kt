package com.alvinhkh.buseta.ui.image

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.alvinhkh.buseta.Api
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.github.chrisbanes.photoview.PhotoView

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import timber.log.Timber


class ImageFragment : Fragment() {

    private val disposables = CompositeDisposable()

    private var actionBar: ActionBar? = null

    private var progressBar: ProgressBar? = null

    private var photoView: PhotoView? = null

    private var bitmap: Bitmap? = null

    private var imageTitle: String? = null

    private var imageUrl: String? = null

    internal val image: DisposableObserver<ResponseBody>
        get() = object : DisposableObserver<ResponseBody>() {
            override fun onNext(body: ResponseBody) {
                if (body.contentType() == null) return
                val contentType = body.contentType().toString()
                if (contentType.contains("image")) {
                    bitmap = BitmapFactory.decodeStream(body.byteStream())
                } else {
                    Timber.d(contentType)
                }
            }

            override fun onError(e: Throwable) {
                bitmap = null
                Timber.d(e)
            }

            override fun onComplete() {
                if (photoView != null && bitmap != null) {
                    photoView?.setImageBitmap(bitmap)
                }
                progressBar?.visibility = View.GONE
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)
        imageTitle = arguments?.getString(ARG_TITLE)
        imageUrl = arguments?.getString(ARG_URL)
        val taskDescription: String? = arguments?.getString(ARG_TASK_DESC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!taskDescription.isNullOrEmpty()) {
                setTaskDescription(taskDescription)
            } else {
                setTaskDescription(getString(R.string.notice) + getString(R.string.interpunct) + getString(R.string.app_name))
            }
        }
        if (activity == null) return view
        actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setTitle(R.string.app_name)
        actionBar?.subtitle = null
        actionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        progressBar = view.findViewById(R.id.progressBar)
        progressBar?.visibility = View.GONE
        photoView = view.findViewById(R.id.image_view)
        photoView?.maximumScale = 4f
        val mTextView = view.findViewById<TextView>(android.R.id.text1)
        mTextView.setOnClickListener { _ -> photoView?.scale = 1f }
        if (!imageTitle.isNullOrEmpty()) {
            mTextView.text = imageTitle
            mTextView.visibility = View.VISIBLE
        } else {
            mTextView.visibility = View.GONE
        }

        showNoticeImage(imageUrl)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_TITLE, imageTitle)
        outState.putString(ARG_URL, imageUrl)
    }

    override fun onResume() {
        super.onResume()
        actionBar?.setTitle(R.string.notice)
        actionBar?.subtitle = null
    }

    override fun onDestroyView() {
        photoView?.setImageBitmap(null)
        view?.visibility = View.GONE
        disposables.clear()
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if (id == R.id.action_refresh) {
            showNoticeImage(imageUrl)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setTaskDescription(title: String?) {
        // overview task
        if (Build.VERSION.SDK_INT >= 28) {
            val taskDesc = ActivityManager.TaskDescription(title,
                    R.mipmap.ic_launcher, R.color.colorPrimary600)
            activity?.setTaskDescription(taskDesc)
        } else {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val taskDesc = ActivityManager.TaskDescription(title, bm,
                    ContextCompat.getColor(context!!, R.color.colorPrimary600))
            activity?.setTaskDescription(taskDesc)
        }
    }

    private fun showNoticeImage(url: String?) {
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(context, R.string.missing_input, Toast.LENGTH_SHORT).show()
            progressBar?.visibility = View.GONE
            return
        }
        // Check internet connection
        if (!ConnectivityUtil.isConnected(context)) {
            if (activity != null) {
                Snackbar.make(activity!!.findViewById(android.R.id.content),
                        R.string.message_no_internet_connection, Snackbar.LENGTH_LONG).show()
            }
            progressBar?.visibility = View.GONE
            return
        }
        progressBar?.visibility = View.VISIBLE
        disposables.add(Api.raw.create(Api::class.java).get(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(image))
    }

    companion object {

        const val ARG_TITLE = "image_title"

        const val ARG_URL = "image_url"

        const val ARG_BITMAP = "image_bitmap"

        const val ARG_TASK_DESC = "task_description"

        fun newInstance(title: String, url: String): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_URL, url)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(title: String, url: String, taskDescription: String): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_URL, url)
            args.putString(ARG_TASK_DESC, taskDescription)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(title: String, bitmap: Bitmap): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putParcelable(ARG_BITMAP, bitmap)
            fragment.arguments = args
            return fragment
        }
    }

}
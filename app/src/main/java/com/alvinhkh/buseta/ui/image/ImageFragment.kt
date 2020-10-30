package com.alvinhkh.buseta.ui.image

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.webkit.URLUtil
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.service.ImageDownloadWorker
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.github.chrisbanes.photoview.PhotoView

import timber.log.Timber


class ImageFragment : Fragment() {

    private val TAG = "Rawimage"

    private lateinit var actionBar: ActionBar

    private lateinit var progressBar: ProgressBar

    private lateinit var photoView: PhotoView

    private var imageTitle: String? = null

    private var imageUrl: String? = null

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
        actionBar = (activity as AppCompatActivity).supportActionBar!!
        actionBar.setTitle(R.string.app_name)
        actionBar.subtitle = null
        actionBar.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE
        photoView = view.findViewById(R.id.image_view)
        photoView.maximumScale = 4f
        val mTextView = view.findViewById<TextView>(android.R.id.text1)
        mTextView.setOnClickListener { photoView.scale = 1f }
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
        actionBar.title = ""
        actionBar.subtitle = null
    }

    override fun onDestroyView() {
        photoView.setImageBitmap(null)
        view?.visibility = View.GONE
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_search)?.isVisible = false
        menu.findItem(R.id.action_search_open)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
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
            activity?.setTaskDescription(ActivityManager.TaskDescription(title, R.mipmap.ic_launcher,
                    ContextCompat.getColor(requireContext(), R.color.black)))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            @Suppress("DEPRECATION")
            activity?.setTaskDescription(ActivityManager.TaskDescription(title, bm,
                    ContextCompat.getColor(requireContext(), R.color.black)))
        }
    }

    private fun showNoticeImage(url: String?) {
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(context, R.string.missing_input, Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }
        // Check internet connection
        if (!ConnectivityUtil.isConnected(context)) {
            if (activity != null) {
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                        R.string.message_no_internet_connection, Snackbar.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            return
        }
        progressBar.visibility = View.VISIBLE
        photoView.setImageBitmap(null)

        WorkManager.getInstance().cancelAllWorkByTag(TAG)
        val request = OneTimeWorkRequest.Builder(ImageDownloadWorker::class.java)
                .setInputData(Data.Builder().putString("url", url).build())
                .addTag(TAG)
                .build()
        WorkManager.getInstance().enqueue(request)
        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id).observe(viewLifecycleOwner,
                { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        progressBar.visibility = View.GONE
                        if (activity != null) {
                            Snackbar.make(requireActivity().findViewById(android.R.id.content),
                                    R.string.message_fail_to_request, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        progressBar.visibility = View.GONE
                        try {
                            photoView.setImageBitmap(BitmapFactory.decodeFile(workInfo.outputData.getString("filepath")))
                        } catch (e: Throwable) {
                            Timber.e(e)
                        }
                    }
                })
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
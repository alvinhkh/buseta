package com.alvinhkh.buseta.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import uk.co.senab.photoview.PhotoView;


public class NoticeImageFragment extends Fragment {

    private static final String TAG = "NoticeImage";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private ProgressBar mProgressBar;
    private PhotoView mPhotoView;
    private TextView mTextView;
    private Bitmap mBitmap = null;

    private String _notice_title = null;
    private String _notice_image = null;

    public NoticeImageFragment() {
    }

    public static NoticeImageFragment newInstance(String notice_title, String notice_image) {
        NoticeImageFragment f = new NoticeImageFragment();
        Bundle args = new Bundle();
        args.putString("notice_title", notice_title);
        args.putString("notice_image", notice_image);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imageview, container, false);
        mContext = super.getActivity();
        // Get arguments
        _notice_title = getArguments().getString("notice_title");
        _notice_image = getArguments().getString("notice_image");
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setSubtitle(null);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        //
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        // Set PhotoView
        mPhotoView = (PhotoView) view.findViewById(R.id.photoView);
        mPhotoView.setMaximumScale(8);
        // Text View
        mTextView = (TextView) view.findViewById(android.R.id.text1);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mPhotoView)
                    mPhotoView.setScale(1);
            }
        });
        if (null != _notice_title && !_notice_title.equals("")) {
            mTextView.setText(_notice_title);
            mTextView.setVisibility(View.VISIBLE);
        } else {
            mTextView.setVisibility(View.GONE);
        }
        if (null != savedInstanceState) {
            mBitmap = savedInstanceState.getParcelable("notice_image_bitmap");
            mPhotoView.setImageBitmap(mBitmap);
        } else {
            showNoticeImage(_notice_image);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("notice_title", _notice_title);
        outState.putString("notice_image", _notice_image);
        mBitmap = Ion.with(mPhotoView).getBitmap();
        outState.putParcelable("notice_image_bitmap", mBitmap);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.passenger_notice);
            mActionBar.setSubtitle(null);
        }
    }

    @Override
    public void onDestroyView() {
        if (null != mPhotoView) {
            mPhotoView.setImageBitmap(null);
            mPhotoView.destroyDrawingCache();
        }
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    private void showNoticeImage(final String notice_image) {

        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);

        Ion.with(mContext)
                .load(Constants.URL.ROUTE_NOTICES_IMAGE + notice_image)
                .progressBar(mProgressBar)
                .setHeader("Referer", Constants.URL.ROUTE_NOTICES + notice_image)
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .withBitmap()
                .error(R.drawable.ic_error_outline_black_48dp)
                .fitCenter()
                .deepZoom()
                .intoImageView(mPhotoView)
                .setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        mBitmap = Ion.with(mPhotoView).getBitmap();
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

}